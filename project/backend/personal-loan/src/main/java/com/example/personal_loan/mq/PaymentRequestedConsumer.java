package com.example.personal_loan.mq;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.example.personal_loan.config.RabbitMQConfig;
import com.example.personal_loan.dto.PaymentRequestedEvent;
import com.example.personal_loan.dto.PaymentSuccessEvent;
import com.example.personal_loan.entity.OutboxMessage;
import com.example.personal_loan.mapper.OutboxMapper;
import com.example.personal_loan.mapper.ProcessMessageMapper;
import com.example.personal_loan.service.PayService;
import com.example.personal_loan.utils.RabbitUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRequestedConsumer {
    private final ProcessMessageMapper processedMessageMapper;
    private final ObjectMapper objectMapper;
    private final RabbitUtil rabbitUtil;
    private final PayService payService;
    private final OutboxMapper outboxMapper;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_REQUESTED_QUEUE)
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
            PaymentRequestedEvent event = objectMapper.readValue(payload, PaymentRequestedEvent.class);
            PaymentSuccessEvent successEvent = payService.pay(event);

            OutboxMessage outbox = new OutboxMessage();
            outbox.setMessageId("payment_success_" + event.getOrderId() + "_" + System.currentTimeMillis());
            outbox.setBusinessType("PAYMENT_SUCCESS");
            outbox.setBusinessId(event.getOrderId());
            outbox.setTopic(RabbitMQConfig.PAYMENT_SUCCESS_ROUTING_KEY);
            outbox.setPayload(objectMapper.writeValueAsString(successEvent));
            outbox.setStatus("PENDING");
            outbox.setCreatedAt(LocalDateTime.now());
            outboxMapper.insert(outbox);

            processedMessageMapper.insertMessage(messageId, "PAYMENT_REQUESTED", event.getOrderId());
            channel.basicAck(tag, false);
        } catch (Exception e) {
            Message dlqMsg = copyWithHeaders(message, messageId);
            rabbitUtil.sendToDLX(RabbitMQConfig.DLQ, dlqMsg);
            channel.basicAck(tag, false);
            log.error("payment requested consume failed, moved to dlq: {}", messageId, e);
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
