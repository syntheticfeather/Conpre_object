package com.example.personal_loan.mq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.personal_loan.config.RabbitMQConfig;
import com.example.personal_loan.entity.OutboxMessage;
import com.example.personal_loan.mapper.OutboxMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class NotificationOutboxPublisherTest {

    @Mock
    private OutboxMapper outboxMapper;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void enqueueNotification_shouldInsertOutbox() {
        NotificationOutboxPublisher publisher = new NotificationOutboxPublisher();
        ReflectionTestUtils.setField(publisher, "outboxMapper", outboxMapper);
        ReflectionTestUtils.setField(publisher, "objectMapper", objectMapper);

        publisher.enqueueNotification(2L, 11L, "LOAN_APPLICATION_STATUS");

        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxMapper).insert(captor.capture());
        OutboxMessage outbox = captor.getValue();
        assertEquals("NOTIFICATION", outbox.getBusinessType());
        assertEquals(11L, outbox.getBusinessId());
        assertEquals("PENDING", outbox.getStatus());
        assertEquals(RabbitMQConfig.NOTIFICATION_ROUTING_KEY, outbox.getTopic());
        assertTrue(outbox.getPayload().contains("LOAN_APPLICATION_STATUS"));
    }

    @Test
    void enqueueAdminNotification_shouldInsertOutbox() {
        NotificationOutboxPublisher publisher = new NotificationOutboxPublisher();
        ReflectionTestUtils.setField(publisher, "outboxMapper", outboxMapper);
        ReflectionTestUtils.setField(publisher, "objectMapper", objectMapper);

        publisher.enqueueAdminNotification(11L, "LOAN_APPLICATION_STATUS");

        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxMapper).insert(captor.capture());
        OutboxMessage outbox = captor.getValue();
        assertEquals("NOTIFICATION", outbox.getBusinessType());
        assertEquals(11L, outbox.getBusinessId());
        assertEquals("PENDING", outbox.getStatus());
        assertEquals(RabbitMQConfig.NOTIFICATION_ROUTING_KEY, outbox.getTopic());
        assertTrue(outbox.getPayload().contains("AI审核拒绝通知"));
    }
}
