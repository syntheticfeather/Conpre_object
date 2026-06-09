package com.example.personal_loan.mq;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.personal_loan.config.RabbitMQConfig;
import com.example.personal_loan.entity.LoanApplication;
import com.example.personal_loan.entity.OutboxMessage;
import com.example.personal_loan.entity.User;
import com.example.personal_loan.enums.ApplicationStatus;
import com.example.personal_loan.factory.OutboxMessageFactory;
import com.example.personal_loan.mapper.ApplicationMapper;
import com.example.personal_loan.mapper.OutboxMapper;
import com.example.personal_loan.mapper.UserMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class NotificationOutboxPublisherTest {

    @Mock
    private OutboxMapper outboxMapper;

    @Mock
    private ApplicationMapper applicationMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private OutboxMessageFactory outboxMessageFactory;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void enqueueNotification_shouldInsertOutbox() {
        NotificationOutboxPublisher publisher = new NotificationOutboxPublisher();
        ReflectionTestUtils.setField(publisher, "outboxMapper", outboxMapper);
        ReflectionTestUtils.setField(publisher, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(publisher, "applicationMapper", applicationMapper);
        ReflectionTestUtils.setField(publisher, "outboxMessageFactory", outboxMessageFactory);

        LoanApplication app = new LoanApplication();
        app.setId(11L);
        app.setStatus(ApplicationStatus.审核中);
        when(applicationMapper.selectById(11L)).thenReturn(app);
        
        OutboxMessage expectedOutbox = new OutboxMessage();
        expectedOutbox.setBusinessType("NOTIFICATION");
        expectedOutbox.setBusinessId(11L);
        expectedOutbox.setStatus("PENDING");
        expectedOutbox.setTopic(RabbitMQConfig.NOTIFICATION_ROUTING_KEY);
        expectedOutbox.setPayload("{\"businessType\":\"LOAN_APPLICATION_STATUS\"}");
        when(outboxMessageFactory.create(any(), any(), any())).thenReturn(expectedOutbox);

        publisher.enqueueNotification(2L, 11L, "LOAN_APPLICATION_STATUS");

        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxMapper).insert(captor.capture());
        OutboxMessage outbox = captor.getValue();
        assertEquals("NOTIFICATION", outbox.getBusinessType());
        assertEquals(11L, outbox.getBusinessId());
        assertEquals("PENDING", outbox.getStatus());
        assertEquals(RabbitMQConfig.NOTIFICATION_ROUTING_KEY, outbox.getTopic());
    }

    @Test
    void enqueueAdminNotification_shouldInsertOutbox() {
        NotificationOutboxPublisher publisher = new NotificationOutboxPublisher();
        ReflectionTestUtils.setField(publisher, "outboxMapper", outboxMapper);
        ReflectionTestUtils.setField(publisher, "objectMapper", objectMapper);
        ReflectionTestUtils.setField(publisher, "userMapper", userMapper);
        ReflectionTestUtils.setField(publisher, "outboxMessageFactory", outboxMessageFactory);

        User admin = new User();
        admin.setId(999999L);
        when(userMapper.findByRole(1)).thenReturn(Arrays.asList(admin));
        
        OutboxMessage expectedOutbox = new OutboxMessage();
        expectedOutbox.setBusinessType("NOTIFICATION");
        expectedOutbox.setBusinessId(11L);
        expectedOutbox.setStatus("PENDING");
        expectedOutbox.setTopic(RabbitMQConfig.NOTIFICATION_ROUTING_KEY);
        expectedOutbox.setPayload("{\"businessType\":\"LOAN_APPLICATION_APPROVE\"}");
        when(outboxMessageFactory.create(any(), any(), any())).thenReturn(expectedOutbox);

        publisher.enqueueAdminNotification(11L, "LOAN_APPLICATION_APPROVE");

        ArgumentCaptor<OutboxMessage> captor = ArgumentCaptor.forClass(OutboxMessage.class);
        verify(outboxMapper).insert(captor.capture());
        OutboxMessage outbox = captor.getValue();
        assertEquals("NOTIFICATION", outbox.getBusinessType());
        assertEquals(11L, outbox.getBusinessId());
        assertEquals("PENDING", outbox.getStatus());
        assertEquals(RabbitMQConfig.NOTIFICATION_ROUTING_KEY, outbox.getTopic());
    }

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }
}
