package com.example.personal_loan.mq;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import com.example.personal_loan.config.RabbitMQConfig;
import com.example.personal_loan.entity.Notification;
import com.example.personal_loan.mapper.NotificationMapper;
import com.example.personal_loan.mapper.ProcessMessageMapper;
import com.example.personal_loan.service.NotificationSseService;
import com.example.personal_loan.utils.RabbitUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final ProcessMessageMapper processedMessageMapper;
    private final NotificationMapper notificationMapper;
    private final ObjectMapper objectMapper;
    private final RabbitUtil rabbitUtil;
    private final NotificationSseService notificationSseService;
    private static final int MAX_RETRIES = 3;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void consume(Message message, Channel channel) throws IOException {
        String payload = new String(message.getBody());
        String messageId = message.getMessageProperties().getHeader("messageId");
        Long tag = rabbitUtil.getTag(message);

        if (messageId == null) {
            channel.basicNack(tag, false, false);
            return;
        }

        if (processedMessageMapper.isProcessMessage(messageId)) {
            channel.basicAck(tag, false);
            return;
        }

        try {
            Notification notification = objectMapper.readValue(payload, Notification.class);
            // 处理管理员通知，使用虚拟管理员ID 999999L
            if (notification.getBusinessType().equals("LOAN_APPLICATION_APPROVE")) {
                notificationMapper.insert(notification);
                notificationSseService.publish(999999L, notification);
                markAsProcessed(messageId, "NOTIFICATION", notification.getBusinessId());
                channel.basicAck(tag, false);
                
            }else{
                // 处理普通通知
                notificationMapper.insert(notification);
                notificationSseService.publish(notification.getUserId(), notification);
                markAsProcessed(messageId, "NOTIFICATION", notification.getBusinessId());
                channel.basicAck(tag, false);
            }

        } catch (Exception e) {
            int deathCount = getDeathCount(message, RabbitMQConfig.NOTIFICATION_QUEUE);
            if (deathCount >= MAX_RETRIES) {
                Message dlqMsg = copyWithHeaders(message, messageId);
                rabbitUtil.sendToDLX(RabbitMQConfig.DLQ, dlqMsg);
                channel.basicAck(tag, false);
                log.error("notification moved to dlq: {}", messageId, e);
                return;
            }
            channel.basicNack(tag, false, false);
            log.error("notification consume failed: {}", messageId, e);
        }
    }

    private void markAsProcessed(String messageId, String businessType, Long businessId) {
        try {
            processedMessageMapper.insertMessage(messageId, businessType, businessId);
        } catch (DuplicateKeyException e) {
            log.warn("processed message exists: {}", messageId);
        }
    }

    private int getDeathCount(Message message, String queue) {
        Map<String, Object> headers = message.getMessageProperties().getHeaders();
        Object xDeath = headers.get("x-death");
        if (!(xDeath instanceof List)) {
            return 0;
        }
        int count = 0;
        for (Object entry : (List<?>) xDeath) {
            if (entry instanceof Map) {
                Object q = ((Map<?, ?>) entry).get("queue");
                Object c = ((Map<?, ?>) entry).get("count");
                if (q != null && q.toString().equals(queue) && c instanceof Long) {
                    count = Math.max(count, ((Long) c).intValue());
                }
            }
        }
        return count;
    }

    private Message copyWithHeaders(Message origin, String messageId) {
        MessageProperties props = new MessageProperties();
        props.getHeaders().putAll(origin.getMessageProperties().getHeaders());
        props.setHeader("messageId", messageId);
        props.setContentType(origin.getMessageProperties().getContentType());
        return new Message(origin.getBody(), props);
    }
}
