package com.example.personal_loan.mq;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.example.personal_loan.config.RabbitMQConfig;
import com.example.personal_loan.entity.Notification;
import com.example.personal_loan.entity.Order;
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
public class OrderRepaidConsumer {
    private final ProcessMessageMapper processedMessageMapper;
    private final ObjectMapper objectMapper;
    private final RabbitUtil rabbitUtil;
    private final NotificationMapper notificationMapper;
    private final NotificationSseService notificationSseService;

    @RabbitListener(queues = RabbitMQConfig.ORDER_REPAID_QUEUE)
    public void consume(Message message, Channel channel) throws IOException {
        String payload = new String(message.getBody());
        String messageId = message.getMessageProperties().getHeader("messageId");
        Long tag = rabbitUtil.getTag(message);
        if (messageId == null) {
            MessageProperties props = new MessageProperties();
            props.getHeaders().putAll(message.getMessageProperties().getHeaders());
            props.setHeader("messageId", "missing_" + System.currentTimeMillis());
            props.setContentType(message.getMessageProperties().getContentType());
            rabbitUtil.sendToDLX(RabbitMQConfig.DLQ, new Message(message.getBody(), props));
            channel.basicAck(tag, false);
            return;
        }
        if (processedMessageMapper.isProcessMessage(messageId)) {
            channel.basicAck(tag, false);
            return;
        }
        try {
            Order order = objectMapper.readValue(payload, Order.class);
            processedMessageMapper.insertMessage(messageId, "ORDER_REPAID", order.getId());

            Notification notification = new Notification();
            notification.setUserId(order.getUserId());
            notification.setBusinessId(order.getId());
            notification.setBusinessType("REPAYMENT");
            notification.setTitle("还款成功");
            notification.setContent("订单(" + order.getId() + ")已完成第" + order.getCurrentTerm() + "期还款");
            notification.setReadFlag(false);
            notification.setCreatedAt(LocalDateTime.now());
            notification.setReadAt(null);
            notificationMapper.insert(notification);
            notificationSseService.publish(notification.getUserId(), notification);

            channel.basicAck(tag, false);
        } catch (Exception e) {
            Message dlqMsg = copyWithHeaders(message, messageId);
            rabbitUtil.sendToDLX(RabbitMQConfig.DLQ, dlqMsg);
            channel.basicAck(tag, false);
            log.error("order repaid consume failed, moved to dlq: {}", messageId, e);
        }
    }

    private Message copyWithHeaders(Message origin, String messageId) {
        MessageProperties props = new MessageProperties();
        props.getHeaders().putAll(origin.getMessageProperties().getHeaders());
        props.setHeader("messageId", messageId);
        props.setContentType(origin.getMessageProperties().getContentType());
        return new Message(origin.getBody(), props);
    }
}
