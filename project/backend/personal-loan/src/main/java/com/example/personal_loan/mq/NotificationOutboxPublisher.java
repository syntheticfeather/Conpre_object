package com.example.personal_loan.mq;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.personal_loan.config.RabbitMQConfig;
import com.example.personal_loan.dto.NotificationEvent;
import com.example.personal_loan.entity.OutboxMessage;
import com.example.personal_loan.mapper.OutboxMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class NotificationOutboxPublisher {

    @Autowired
    private OutboxMapper outboxMapper;

    @Autowired
    private ObjectMapper objectMapper;

    public void enqueueLoanApplicationStatus(Long userId, Long applicationId, String visibleStatus) {
        NotificationEvent event = new NotificationEvent(
                "LOAN_APPLICATION_STATUS",
                userId,
                applicationId,
                "LOAN_APPLICATION",
                visibleStatus,
                LocalDateTime.now()
        );

        OutboxMessage outbox = new OutboxMessage();
        outbox.setMessageId("notif_app_" + applicationId + "_" + System.currentTimeMillis());
        outbox.setBusinessType("NOTIFICATION");
        outbox.setBusinessId(applicationId);
        outbox.setTopic(RabbitMQConfig.NOTIFICATION_ROUTING_KEY);
        try {
            outbox.setPayload(objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        outbox.setStatus("PENDING");
        outbox.setCreatedAt(LocalDateTime.now());
        outboxMapper.insert(outbox);
    }

    public void enqueueRepaymentStatus(Long userId, Long orderId, String visibleStatus) {
        NotificationEvent event = new NotificationEvent(
                "REPAYMENT_STATUS",
                userId,
                orderId,
                "REPAYMENT",
                visibleStatus,
                LocalDateTime.now()
        );

        OutboxMessage outbox = new OutboxMessage();
        outbox.setMessageId("notif_repay_" + orderId + "_" + System.currentTimeMillis());
        outbox.setBusinessType("NOTIFICATION");
        outbox.setBusinessId(orderId);
        outbox.setTopic(RabbitMQConfig.NOTIFICATION_ROUTING_KEY);
        try {
            outbox.setPayload(objectMapper.writeValueAsString(event));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        outbox.setStatus("PENDING");
        outbox.setCreatedAt(LocalDateTime.now());
        outboxMapper.insert(outbox);
    }


}

