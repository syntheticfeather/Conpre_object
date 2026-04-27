package com.example.personal_loan.factory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.personal_loan.entity.OutboxMessage;
import com.example.personal_loan.enums.BusinessType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OutboxMessageFactory {

    @Autowired
    private ObjectMapper objectMapper;

    public OutboxMessage create(BusinessType type, Object businessObject, Long businessId) {
        OutboxMessage outbox = new OutboxMessage();

        // 生成 messageId
        String messageId = type.getMessageIdPrefix() + businessId + "_" + System.currentTimeMillis();
        outbox.setMessageId(messageId);

        // 设置业务类型和ID
        outbox.setBusinessType(type.getBusinessType());
        outbox.setBusinessId(businessId);

        // 设置 topic
        outbox.setTopic(type.getTopic());

        // 序列化 payload
        try {
            outbox.setPayload(objectMapper.writeValueAsString(businessObject));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("序列化失败", e);
        }

        // 固定字段
        outbox.setStatus("PENDING");
        outbox.setCreatedAt(java.time.LocalDateTime.now());

        return outbox;
    }
}