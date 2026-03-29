package com.example.personal_loan.mq;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.example.personal_loan.config.RabbitMQConfig;
import com.example.personal_loan.entity.Order;
import com.example.personal_loan.mapper.ProcessMessageMapper;
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
    private static final int MAX_RETRIES = 3;

    @RabbitListener(queues = RabbitMQConfig.ORDER_REPAID_QUEUE)
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
            Order order = objectMapper.readValue(payload, Order.class);
            processedMessageMapper.insertMessage(messageId, "ORDER_REPAID", order.getId());
            // 发送短信通知等业务
            channel.basicAck(tag, false);
        } catch (Exception e) {
            int deathCount = getDeathCount(message, RabbitMQConfig.ORDER_REPAID_QUEUE);
            if (deathCount >= MAX_RETRIES) {
                Message dlqMsg = copyWithHeaders(message, messageId);
                rabbitUtil.sendToDLX(RabbitMQConfig.DLQ, dlqMsg);
                channel.basicAck(tag, false);
                return;
            }
            channel.basicNack(tag, false, false);
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
