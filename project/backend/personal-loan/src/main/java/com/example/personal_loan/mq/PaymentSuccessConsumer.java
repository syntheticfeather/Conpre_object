package com.example.personal_loan.mq;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import com.example.personal_loan.config.RabbitMQConfig;
import com.example.personal_loan.dto.PaymentSuccessEvent;
import com.example.personal_loan.entity.Order;
import com.example.personal_loan.entity.OutboxMessage;
import com.example.personal_loan.enums.OrderStatus;
import com.example.personal_loan.mapper.OrderMapper;
import com.example.personal_loan.mapper.OutboxMapper;
import com.example.personal_loan.mapper.PaymentRecordMapper;
import com.example.personal_loan.mapper.ProcessMessageMapper;
import com.example.personal_loan.utils.RabbitUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentSuccessConsumer {
    private final ProcessMessageMapper processedMessageMapper;
    private final ObjectMapper objectMapper;
    private final RabbitUtil rabbitUtil;
    private final OrderMapper orderMapper;
    private final OutboxMapper outboxMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private static final int MAX_RETRIES = 3;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_SUCCESS_QUEUE)
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
            PaymentSuccessEvent event = objectMapper.readValue(payload, PaymentSuccessEvent.class);
            try {
                // 插入支付记录
                paymentRecordMapper.insert(event.getTxId(), event.getOrderId(), event.getAmount(), "SUCCESS", event.getPaidAt());
            } catch (DuplicateKeyException ex) {
                channel.basicAck(tag, false);
                return;
            }
            // 更新订单状态, 增加已还金额, 更新当前期数
            // 如果当前期数等于总期数, 则订单状态为已完成
            Order order = orderMapper.selectById(event.getOrderId());
            if (order == null) {
                channel.basicAck(tag, false);
                return;
            }
            BigDecimal newRepaidAmount = order.getRepaidAmount().add(event.getAmount());
            Integer newCurrentTerm = order.getCurrentTerm() + 1;
            OrderStatus newStatus = (newCurrentTerm.equals(order.getTerm())) ? OrderStatus.已完成 : OrderStatus.正常;
            order.setRepaidAmount(newRepaidAmount);
            order.setCurrentTerm(newCurrentTerm);
            order.setStatus(newStatus);
            orderMapper.update(order);

            // 记录已处理信息，插入幂等表
            processedMessageMapper.insertMessage(messageId, "PAYMENT_SUCCESS", order.getId());

            // 写order_repaid消息到outbox,用于后续发通知
            OutboxMessage outbox = new OutboxMessage();
            outbox.setMessageId("order_repaid_" + order.getId() + "_" + System.currentTimeMillis());
            outbox.setBusinessType("ORDER_REPAID");
            outbox.setBusinessId(order.getId());
            outbox.setTopic(RabbitMQConfig.ORDER_REPAID_ROUTING_KEY);
            try {
                outbox.setPayload(objectMapper.writeValueAsString(order));
            } catch (Exception ex) {}
            outbox.setStatus("PENDING");
            outbox.setCreatedAt(LocalDateTime.now());
            outboxMapper.insert(outbox);

            // 确认消费成功
            channel.basicAck(tag, false);
        } catch (Exception e) {
            int deathCount = getDeathCount(message, RabbitMQConfig.PAYMENT_SUCCESS_QUEUE);
            if (deathCount >= MAX_RETRIES) {
                Message dlqMsg = copyWithHeaders(message, messageId);
                rabbitUtil.sendToDLX(RabbitMQConfig.DLQ, dlqMsg);
                channel.basicAck(tag, false);
                log.error("payment success consume failed, moved to dlq: {}", messageId, e);
                return;
            }
            channel.basicNack(tag, false, false);
            log.error("payment success consume failed, retrying: {}", messageId, e);
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
