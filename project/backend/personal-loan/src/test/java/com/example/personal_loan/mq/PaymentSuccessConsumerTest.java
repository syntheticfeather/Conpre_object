package com.example.personal_loan.mq;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.dao.DuplicateKeyException;

import com.example.personal_loan.config.RabbitMQConfig;
import com.example.personal_loan.dto.PaymentSuccessEvent;
import com.example.personal_loan.entity.Order;
import com.example.personal_loan.enums.OrderStatus;
import com.example.personal_loan.mapper.OrderMapper;
import com.example.personal_loan.mapper.OutboxMapper;
import com.example.personal_loan.mapper.PaymentRecordMapper;
import com.example.personal_loan.mapper.ProcessMessageMapper;
import com.example.personal_loan.utils.RabbitUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;

@ExtendWith(MockitoExtension.class)
class PaymentSuccessConsumerTest {

    @Mock
    private ProcessMessageMapper processedMessageMapper;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private RabbitUtil rabbitUtil;

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private OutboxMapper outboxMapper;

    @Mock
    private PaymentRecordMapper paymentRecordMapper;

    @Mock
    private Channel channel;

    //验证当收到一条“新的”支付成功消息时，系统是否按顺序执行了所有必要的业务操作（记录流水、更新订单、发送下游消息）。
    @Test
    void consume_success_shouldUpdateOrderAndAck() throws Exception {
        when(rabbitUtil.getTag(any())).thenReturn(1L);
        when(processedMessageMapper.isProcessMessage("mid")).thenReturn(false);

        Order order = new Order();
        order.setId(11L);
        order.setUserId(2L);
        order.setStatus(OrderStatus.正常);
        order.setRepaidAmount(new BigDecimal("0.00"));
        order.setCurrentTerm(0);
        order.setTerm(3);
        when(orderMapper.selectById(11L)).thenReturn(order);

        PaymentSuccessEvent event = new PaymentSuccessEvent(11L, new BigDecimal("12.34"), "tx", LocalDateTime.now());
        Message message = buildMessage(objectMapper.writeValueAsBytes(event), "mid", 1L);

        PaymentSuccessConsumer consumer = new PaymentSuccessConsumer(
                processedMessageMapper,
                objectMapper,
                rabbitUtil,
                orderMapper,
                outboxMapper,
                paymentRecordMapper
        );
        consumer.consume(message, channel);

        //验证是否插入了支付流水记录。
        verify(paymentRecordMapper).insert(eq("tx"), eq(11L), eq(new BigDecimal("12.34")), eq("SUCCESS"), any(LocalDateTime.class));
        //验证是否更新了订单状态为“已还款”。
        verify(orderMapper).update(any(Order.class));
        //验证是否生成了 Outbox 消息
        verify(outboxMapper).insert(any());

        verify(processedMessageMapper).insertMessage("mid", "PAYMENT_SUCCESS", 11L);
        verify(channel).basicAck(1L, false);
    }

    // 验证了当发生数据库唯一键冲突（例如重复的交易号）时，不报错，也不重复更新数据。
    @Test
    void consume_duplicatePaymentRecord_shouldAckAndStop() throws Exception {
        when(rabbitUtil.getTag(any())).thenReturn(2L);
        when(processedMessageMapper.isProcessMessage("mid")).thenReturn(false);
        //模拟了数据库层面的唯一索引约束,模拟插入支付流水时抛出 DuplicateKeyException 异常
        when(paymentRecordMapper.insert(any(), any(), any(), any(), any())).thenThrow(new DuplicateKeyException("dup"));

        PaymentSuccessEvent event = new PaymentSuccessEvent(11L, new BigDecimal("12.34"), "tx", LocalDateTime.now());
        Message message = buildMessage(objectMapper.writeValueAsBytes(event), "mid", 2L);

        PaymentSuccessConsumer consumer = new PaymentSuccessConsumer(
                processedMessageMapper,
                objectMapper,
                rabbitUtil,
                orderMapper,
                outboxMapper,
                paymentRecordMapper
        );
        consumer.consume(message, channel);

        verify(channel).basicAck(2L, false);
        verify(orderMapper, never()).update(any());
        verify(outboxMapper, never()).insert(any());
        verify(processedMessageMapper, never()).insertMessage(any(), any(), any());
    }

    @Test
    void consume_missingMessageId_shouldMoveToDlqAndAck() throws Exception {
        when(rabbitUtil.getTag(any())).thenReturn(3L);
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(3L);
        Message message = new Message(objectMapper.writeValueAsBytes(new PaymentSuccessEvent()), props);

        PaymentSuccessConsumer consumer = new PaymentSuccessConsumer(
                processedMessageMapper,
                objectMapper,
                rabbitUtil,
                orderMapper,
                outboxMapper,
                paymentRecordMapper
        );
        consumer.consume(message, channel);

        verify(rabbitUtil).sendToDLX(eq(RabbitMQConfig.DLQ), any(Message.class));
        verify(channel).basicAck(3L, false);
    }

    private static Message buildMessage(byte[] body, String messageId, long tag) {
        MessageProperties props = new MessageProperties();
        props.setHeader("messageId", messageId);
        props.setContentType("application/json");
        props.setDeliveryTag(tag);
        return new Message(body, props);
    }
}
