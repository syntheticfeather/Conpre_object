package com.example.personal_loan.config;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.personal_loan.mapper.OutboxMapper;

class RabbitTemplateConfigTest {

    @Test
    void confirmCallback_ack_shouldMarkSent() {
        ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);
        MessageConverter messageConverter = Mockito.mock(MessageConverter.class);
        OutboxMapper outboxMapper = Mockito.mock(OutboxMapper.class);

        RabbitTemplate template = new RabbitTemplateConfig().rabbitTemplate(connectionFactory, messageConverter, outboxMapper);

        Object cb = ReflectionTestUtils.getField(template, "confirmCallback");
        RabbitTemplate.ConfirmCallback confirmCallback = (RabbitTemplate.ConfirmCallback) cb;

        confirmCallback.confirm(new CorrelationData("mid"), true, null);

        verify(outboxMapper).markAsSent("mid", Mockito.any(LocalDateTime.class));
        verify(outboxMapper, never()).markAsFailed("mid");
    }

    @Test
    void confirmCallback_nack_shouldMarkFailed() {
        ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);
        MessageConverter messageConverter = Mockito.mock(MessageConverter.class);
        OutboxMapper outboxMapper = Mockito.mock(OutboxMapper.class);

        RabbitTemplate template = new RabbitTemplateConfig().rabbitTemplate(connectionFactory, messageConverter, outboxMapper);

        Object cb = ReflectionTestUtils.getField(template, "confirmCallback");
        RabbitTemplate.ConfirmCallback confirmCallback = (RabbitTemplate.ConfirmCallback) cb;

        confirmCallback.confirm(new CorrelationData("mid"), false, "nack");

        verify(outboxMapper).markAsFailed("mid");
        verify(outboxMapper, never()).markAsSent("mid", Mockito.any(LocalDateTime.class));
    }

    @Test
    void confirmCallback_nullCorrelationOrId_shouldIgnore() {
        ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);
        MessageConverter messageConverter = Mockito.mock(MessageConverter.class);
        OutboxMapper outboxMapper = Mockito.mock(OutboxMapper.class);

        RabbitTemplate template = new RabbitTemplateConfig().rabbitTemplate(connectionFactory, messageConverter, outboxMapper);

        Object cb = ReflectionTestUtils.getField(template, "confirmCallback");
        RabbitTemplate.ConfirmCallback confirmCallback = (RabbitTemplate.ConfirmCallback) cb;

        confirmCallback.confirm(null, true, null);
        confirmCallback.confirm(new CorrelationData(null), true, null);

        verify(outboxMapper, never()).markAsSent(Mockito.any(), Mockito.any(LocalDateTime.class));
        verify(outboxMapper, never()).markAsFailed(Mockito.any());
    }

    @Test
    void returnsCallback_shouldMarkFailedWhenHeaderMessageIdPresent() {
        ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);
        MessageConverter messageConverter = Mockito.mock(MessageConverter.class);
        OutboxMapper outboxMapper = Mockito.mock(OutboxMapper.class);

        RabbitTemplate template = new RabbitTemplateConfig().rabbitTemplate(connectionFactory, messageConverter, outboxMapper);

        Object cb = ReflectionTestUtils.getField(template, "returnsCallback");
        RabbitTemplate.ReturnsCallback returnsCallback = (RabbitTemplate.ReturnsCallback) cb;

        MessageProperties props = new MessageProperties();
        props.setHeader("messageId", "mid");
        Message msg = new Message("{}".getBytes(), props);

        ReturnedMessage returned = new ReturnedMessage(msg, 312, "NO_ROUTE", "ex", "rk");
        returnsCallback.returnedMessage(returned);

        verify(outboxMapper).markAsFailed("mid");
    }

    @Test
    void returnsCallback_whenHeaderMissing_shouldIgnore() {
        ConnectionFactory connectionFactory = Mockito.mock(ConnectionFactory.class);
        MessageConverter messageConverter = Mockito.mock(MessageConverter.class);
        OutboxMapper outboxMapper = Mockito.mock(OutboxMapper.class);

        RabbitTemplate template = new RabbitTemplateConfig().rabbitTemplate(connectionFactory, messageConverter, outboxMapper);

        Object cb = ReflectionTestUtils.getField(template, "returnsCallback");
        RabbitTemplate.ReturnsCallback returnsCallback = (RabbitTemplate.ReturnsCallback) cb;

        Message msg = new Message("{}".getBytes(), new MessageProperties());
        ReturnedMessage returned = new ReturnedMessage(msg, 312, "NO_ROUTE", "ex", "rk");
        returnsCallback.returnedMessage(returned);

        verify(outboxMapper, never()).markAsFailed(Mockito.any());
    }
}
