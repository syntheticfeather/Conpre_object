package com.example.personal_loan.config;

import com.example.personal_loan.mapper.OutboxMapper;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitTemplateConfig {
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter, OutboxMapper outboxMapper) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setMandatory(true);
        template.setConfirmCallback((CorrelationData correlationData, boolean ack, String cause) -> {
            if (correlationData == null) {
                return;
            }
            String messageId = correlationData.getId();
            if (messageId == null) {
                return;
            }
            if (ack) {
                outboxMapper.markAsSent(messageId);
            } else {
                outboxMapper.markAsFailed(messageId);
            }
        });
        template.setReturnsCallback(returned -> {
            Object id = returned.getMessage().getMessageProperties().getHeaders().get("messageId");
            if (id instanceof String) {
                outboxMapper.markAsFailed((String) id);
            }
        });
        return template;
    }
}
