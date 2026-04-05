package com.example.personal_loan.mq;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import com.example.personal_loan.dto.NotificationEvent;
import com.example.personal_loan.entity.Notification;
import com.example.personal_loan.mapper.NotificationMapper;
import com.example.personal_loan.mapper.ProcessMessageMapper;
import com.example.personal_loan.service.NotificationSseService;
import com.example.personal_loan.utils.RabbitUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private ProcessMessageMapper processedMessageMapper;

    @Mock
    private NotificationMapper notificationMapper;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private RabbitUtil rabbitUtil;

    @Mock
    private NotificationSseService notificationSseService;

    @Mock
    private Channel channel;

    @Test
    void consume_success_shouldInsertAndPublishAndAck() throws Exception {
        when(rabbitUtil.getTag(any())).thenReturn(1L);
        when(processedMessageMapper.isProcessMessage("mid")).thenReturn(false);

        NotificationEvent event = new NotificationEvent("LOAN_APPLICATION_STATUS", 2L, 11L, null, "已通过", LocalDateTime.now());
        Message message = buildMessage(objectMapper.writeValueAsBytes(event), "mid", 1L);

        NotificationConsumer consumer = new NotificationConsumer(
                processedMessageMapper,
                notificationMapper,
                objectMapper,
                rabbitUtil,
                notificationSseService
        );
        consumer.consume(message, channel);

        verify(notificationMapper).insert(any(Notification.class));
        verify(notificationSseService).publish(any(), any(Notification.class));
        verify(processedMessageMapper).insertMessage("mid", "NOTIFICATION", 11L);
        verify(channel).basicAck(1L, false);
    }

    @Test
    void consume_missingMessageId_shouldNack() throws Exception {
        when(rabbitUtil.getTag(any())).thenReturn(2L);

        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(2L);
        Message message = new Message(objectMapper.writeValueAsBytes(new NotificationEvent()), props);

        NotificationConsumer consumer = new NotificationConsumer(
                processedMessageMapper,
                notificationMapper,
                objectMapper,
                rabbitUtil,
                notificationSseService
        );
        consumer.consume(message, channel);

        verify(channel).basicNack(2L, false, false);
        verify(notificationMapper, never()).insert(any());
    }

    private static Message buildMessage(byte[] body, String messageId, long tag) {
        MessageProperties props = new MessageProperties();
        props.setHeader("messageId", messageId);
        props.setContentType("application/json");
        props.setDeliveryTag(tag);
        return new Message(body, props);
    }
}
