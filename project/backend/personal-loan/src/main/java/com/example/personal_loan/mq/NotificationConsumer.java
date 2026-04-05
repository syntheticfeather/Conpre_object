package com.example.personal_loan.mq;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import com.example.personal_loan.config.RabbitMQConfig;
import com.example.personal_loan.dto.NotificationEvent;
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
            NotificationEvent event = objectMapper.readValue(payload, NotificationEvent.class);
            Notification notification = new Notification();
            notification.setUserId(event.getUserId());
            notification.setBusinessId(event.getBusinessId());
            notification.setBusinessType(event.getBusinessType());
            notification.setTitle(buildTitle(event));
            notification.setContent(buildContent(event));
            notification.setReadFlag(false);
            notification.setCreatedAt(LocalDateTime.now());
            notification.setReadAt(null);
            notificationMapper.insert(notification);

            notificationSseService.publish(notification.getUserId(), notification);

            markAsProcessed(messageId, "NOTIFICATION", event.getBusinessId());
            channel.basicAck(tag, false);
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

    private String buildTitle(NotificationEvent event) {
        if (event == null) {
            return "通知";
        }
        String status = event.getVisibleStatus();
        if ("审核中".equals(status)) {
            return "贷款申请已提交";
        }
        if ("已通过".equals(status)) {
            return "贷款申请已通过";
        }
        if ("申请失败".equals(status)) {
            return "贷款申请未通过";
        }
        if ("已取消".equals(status)) {
            return "贷款申请已取消";
        }
        return "贷款申请状态更新";
    }

    private String buildContent(NotificationEvent event) {
        if (event == null) {
            return "您有一条新通知";
        }
        String status = event.getVisibleStatus();
        String businessType = event.getBusinessType();
        Long businessId = event.getBusinessId();
        if (businessId == null) {
            if ("LOAN_APPLICATION".equals(businessType)) {
                if ("审核中".equals(status)) {
                    return "您的贷款申请已提交，正在审核中";
                }
                if ("已通过".equals(status)) {
                    return "您的贷款申请已通过";
                }
                if ("申请失败".equals(status)) {
                    return "您的贷款申请未通过审核";
                }
                if ("已取消".equals(status)) {
                    return "您的贷款申请已取消";
                }
                return "您的贷款申请状态已更新";
            } else if ("REPAYMENT".equals(businessType)) {
                return "您的还款已处理";
            }
            return "您有一条新通知";
        }

        if ("LOAN_APPLICATION".equals(businessType)) {
            if ("审核中".equals(status)) {
                return "您的贷款申请(" + businessId + ")已提交，正在审核中";
            }
            if ("已通过".equals(status)) {
                return "您的贷款申请(" + businessId + ")已通过";
            }
            if ("申请失败".equals(status)) {
                return "您的贷款申请(" + businessId + ")未通过审核";
            }
            if ("已取消".equals(status)) {
                return "您的贷款申请(" + businessId + ")已取消";
            }
            return "您的贷款申请(" + businessId + ")状态已更新";
        } else if ("REPAYMENT".equals(businessType)) {
            return "您的还款(" + businessId + ")已处理";
        }
        return "您有一条新通知";
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
