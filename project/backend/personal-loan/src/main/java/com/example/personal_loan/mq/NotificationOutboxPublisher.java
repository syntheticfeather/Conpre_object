package com.example.personal_loan.mq;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.personal_loan.config.RabbitMQConfig;
import com.example.personal_loan.entity.Notification;
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

    public void enqueueNotification(Long userId, Long businessId, String businessType) {
        // 构建通知实体 Notification
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setBusinessId(businessId);
        notification.setBusinessType(businessType);
        notification.setReadFlag(false);
        notification.setCreatedAt(LocalDateTime.now());

        // 写 outbox 消息
        String messageId = "notif_" + businessType.toLowerCase() + "_" + businessId + "_" + System.currentTimeMillis();
        OutboxMessage outbox = new OutboxMessage();
        outbox.setMessageId(messageId);
        outbox.setBusinessType("NOTIFICATION");
        outbox.setBusinessId(businessId);
        outbox.setTopic(RabbitMQConfig.NOTIFICATION_ROUTING_KEY);
        
        try {
            outbox.setPayload(objectMapper.writeValueAsString(notification));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        
        outbox.setStatus("PENDING");
        outbox.setCreatedAt(LocalDateTime.now());
        outboxMapper.insert(outbox);
    }

    public void enqueueAdminNotification(Long businessId, String businessType) {
        
        Notification notification = new Notification();
        notification.setUserId(null);
        notification.setBusinessId(businessId);
        notification.setBusinessType(businessType);
        notification.setTitle("AI审核拒绝通知");
        notification.setContent("贷款申请 " + businessId + " 被AI拒绝，需要人工审核");
        notification.setReadFlag(false);
        notification.setCreatedAt(LocalDateTime.now());
        
        String messageId = "notif_admin_" + businessType.toLowerCase() + "_" + businessId + "_" + System.currentTimeMillis();
        
        OutboxMessage outbox = new OutboxMessage();
        outbox.setMessageId(messageId);
        outbox.setBusinessType("NOTIFICATION");
        outbox.setBusinessId(businessId);
        outbox.setTopic(RabbitMQConfig.NOTIFICATION_ROUTING_KEY);
        
        try {
            outbox.setPayload(objectMapper.writeValueAsString(notification));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        
        outbox.setStatus("PENDING");
        outbox.setCreatedAt(LocalDateTime.now());
        outboxMapper.insert(outbox);
    }

}

