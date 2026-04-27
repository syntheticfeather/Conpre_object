package com.example.personal_loan.enums;

import com.example.personal_loan.config.RabbitMQConfig;

public enum BusinessType {
    LOAN_APPLICATION("loan_app_", "LOAN_APPLICATION", RabbitMQConfig.LOAN_APPLICATION_ROUTING_KEY),
    PAYMENT_REQUESTED("payment_requested_", "PAYMENT_REQUESTED", RabbitMQConfig.PAYMENT_REQUESTED_ROUTING_KEY),
    NOTIFICATION("notif_", "NOTIFICATION", RabbitMQConfig.NOTIFICATION_ROUTING_KEY),
    PAYMENT_SUCCESS("payment_success_", "PAYMENT_SUCCESS", RabbitMQConfig.PAYMENT_SUCCESS_ROUTING_KEY),
    ;

    private final String messageIdPrefix;
    private final String businessType;
    private final String topic;

    BusinessType(String messageIdPrefix, String businessType, String topic) {
        this.messageIdPrefix = messageIdPrefix;
        this.businessType = businessType;
        this.topic = topic;
    }

    public String getMessageIdPrefix() {
        return messageIdPrefix;
    }

    public String getBusinessType() {
        return businessType;
    }

    public String getTopic() {
        return topic;
    }
}