package com.example.personal_loan.mq;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import com.example.personal_loan.config.RabbitMQConfig;
import com.example.personal_loan.entity.Notification;
import com.example.personal_loan.entity.Order;
import com.example.personal_loan.mapper.NotificationMapper;
import com.example.personal_loan.mapper.ProcessMessageMapper;
import com.example.personal_loan.service.NotificationSseService;
import com.example.personal_loan.utils.RabbitUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;

@ExtendWith(MockitoExtension.class)
class OrderRepaidConsumerTest {

    @Mock
    private ProcessMessageMapper processedMessageMapper;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private RabbitUtil rabbitUtil;

    @Mock
    private NotificationMapper notificationMapper;

    @Mock
    private NotificationSseService notificationSseService;

    @Mock
    private Channel channel;

    @Test
    void consume_success_shouldInsertNotificationAndAck() throws Exception {
        when(rabbitUtil.getTag(any())).thenReturn(10L);
        when(processedMessageMapper.isProcessMessage("mid")).thenReturn(false);

        Order order = new Order();
        order.setId(11L);
        order.setUserId(2L);
        order.setCurrentTerm(1);
        Message message = buildMessage(objectMapper.writeValueAsBytes(order), "mid", 10L);

        OrderRepaidConsumer consumer = new OrderRepaidConsumer(
                processedMessageMapper,
                objectMapper,
                rabbitUtil,
                notificationMapper,
                notificationSseService
        );
        consumer.consume(message, channel);

        verify(processedMessageMapper).insertMessage("mid", "ORDER_REPAID", 11L);
        verify(notificationMapper).insert(any(Notification.class));
        verify(notificationSseService).publish(eq(2L), any(Notification.class));
        verify(channel).basicAck(10L, false);
    }

    @Test
    void consume_idempotent_shouldAck() throws Exception {
        when(rabbitUtil.getTag(any())).thenReturn(11L);
        when(processedMessageMapper.isProcessMessage("mid")).thenReturn(true);

        Order order = new Order();
        order.setId(11L);
        order.setUserId(2L);
        order.setCurrentTerm(1);
        Message message = buildMessage(objectMapper.writeValueAsBytes(order), "mid", 11L);

        OrderRepaidConsumer consumer = new OrderRepaidConsumer(
                processedMessageMapper,
                objectMapper,
                rabbitUtil,
                notificationMapper,
                notificationSseService
        );
        consumer.consume(message, channel);

        verify(channel).basicAck(11L, false);
        verify(notificationMapper, never()).insert(any());
        verify(notificationSseService, never()).publish(any(), any());
    }

    @Test
    void consume_missingMessageId_shouldMoveToDlqAndAck() throws Exception {
        when(rabbitUtil.getTag(any())).thenReturn(12L);

        Order order = new Order();
        order.setId(11L);
        order.setUserId(2L);
        order.setCurrentTerm(1);

        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(12L);
        Message message = new Message(objectMapper.writeValueAsBytes(order), props);

        OrderRepaidConsumer consumer = new OrderRepaidConsumer(
                processedMessageMapper,
                objectMapper,
                rabbitUtil,
                notificationMapper,
                notificationSseService
        );
        consumer.consume(message, channel);

        verify(rabbitUtil).sendToDLX(eq(RabbitMQConfig.DLQ), any(Message.class));
        verify(channel).basicAck(12L, false);
    }

    private static Message buildMessage(byte[] body, String messageId, long tag) {
        MessageProperties props = new MessageProperties();
        props.setHeader("messageId", messageId);
        props.setContentType("application/json");
        props.setDeliveryTag(tag);
        return new Message(body, props);
    }
}
