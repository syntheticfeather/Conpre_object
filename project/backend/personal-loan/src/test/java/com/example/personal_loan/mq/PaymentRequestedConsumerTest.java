package com.example.personal_loan.mq;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

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

@ExtendWith(MockitoExtension.class)
class PaymentRequestedConsumerTest {

    @Mock
    private ProcessMessageMapper processedMessageMapper;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private RabbitUtil rabbitUtil;

    @Mock
    private PayService payService;

    @Mock
    private OutboxMapper outboxMapper;

    @Mock
    private Channel channel;

    // 验证当收到一条“新的”支付请求时，系统是否正确调用支付服务、保存 Outbox 消息（以便后续发送支付成功事件），并正确 ACK。
    @Test
    void consume_success_shouldCreateOutboxAndAck() throws Exception {
        when(rabbitUtil.getTag(any())).thenReturn(7L);
        when(processedMessageMapper.isProcessMessage("mid")).thenReturn(false);

        PaymentRequestedEvent req = new PaymentRequestedEvent(11L, new BigDecimal("12.34"), LocalDateTime.now());
        PaymentSuccessEvent success = new PaymentSuccessEvent(11L, req.getAmount(), "tx", LocalDateTime.now());
        when(payService.pay(any())).thenReturn(success);

        Message message = buildMessage(objectMapper.writeValueAsBytes(req), "mid", 7L);

        PaymentRequestedConsumer consumer = new PaymentRequestedConsumer(
                processedMessageMapper,
                objectMapper,
                rabbitUtil,
                payService,
                outboxMapper
        );
        consumer.consume(message, channel);

        ArgumentCaptor<OutboxMessage> outboxCaptor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxMapper).insert(outboxCaptor.capture());
        assertEquals(RabbitMQConfig.PAYMENT_SUCCESS_ROUTING_KEY, outboxCaptor.getValue().getTopic());
        assertEquals("PENDING", outboxCaptor.getValue().getStatus());
        assertEquals("PAYMENT_SUCCESS", outboxCaptor.getValue().getBusinessType());
        assertEquals(11L, outboxCaptor.getValue().getBusinessId());

        verify(processedMessageMapper).insertMessage("mid", "PAYMENT_REQUESTED", 11L);
        verify(channel).basicAck(7L, false);
    }

    // 验证当收到的消息格式错误（缺少关键的 Message ID）时，系统将其移入死信队列（DLQ）
    @Test
    void consume_missingMessageId_shouldMoveToDlqAndAck() throws Exception {
        when(rabbitUtil.getTag(any())).thenReturn(3L);

        PaymentRequestedEvent req = new PaymentRequestedEvent(11L, new BigDecimal("12.34"), LocalDateTime.now());
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(3L);
        Message message = new Message(objectMapper.writeValueAsBytes(req), props);

        PaymentRequestedConsumer consumer = new PaymentRequestedConsumer(
                processedMessageMapper,
                objectMapper,
                rabbitUtil,
                payService,
                outboxMapper
        );
        consumer.consume(message, channel);

        verify(rabbitUtil).sendToDLX(eq(RabbitMQConfig.DLQ), any(Message.class));
        verify(channel).basicAck(3L, false);
        verify(outboxMapper, never()).insert(any());
    }

    // 验证当收到一条“已经处理过”的消息时，系统是否直接忽略业务逻辑，仅发送 ACK。防止重复扣款
    @Test
    void consume_idempotent_shouldAck() throws Exception {
        when(rabbitUtil.getTag(any())).thenReturn(9L);
        when(processedMessageMapper.isProcessMessage("mid")).thenReturn(true);

        PaymentRequestedEvent req = new PaymentRequestedEvent(11L, new BigDecimal("12.34"), LocalDateTime.now());
        Message message = buildMessage(objectMapper.writeValueAsBytes(req), "mid", 9L);

        PaymentRequestedConsumer consumer = new PaymentRequestedConsumer(
                processedMessageMapper,
                objectMapper,
                rabbitUtil,
                payService,
                outboxMapper
        );
        consumer.consume(message, channel);

        verify(channel).basicAck(9L, false);
        verify(outboxMapper, never()).insert(any());
        verify(payService, never()).pay(any());
    }

    private static Message buildMessage(byte[] body, String messageId, long tag) {
        MessageProperties props = new MessageProperties();
        props.setHeader("messageId", messageId);
        props.setContentType("application/json");
        props.setDeliveryTag(tag);
        return new Message(body, props);
    }
}
