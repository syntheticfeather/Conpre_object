package com.example.personal_loan.mq;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import com.example.personal_loan.config.RabbitMQConfig;
import com.example.personal_loan.entity.LoanApplication;
import com.example.personal_loan.enums.ApplicationStatus;
import com.example.personal_loan.enums.RepaidType;
import com.example.personal_loan.mapper.ProcessMessageMapper;
import com.example.personal_loan.service.AIApproveService;
import com.example.personal_loan.utils.RabbitUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;

@ExtendWith(MockitoExtension.class)
class LoanApplicationConsumerTest {

    @Mock
    private ProcessMessageMapper processedMessageMapper;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    private AIApproveService aiApproveService;

    @Mock
    private RabbitUtil rabbitUtil;

    @Mock
    private Channel channel;

    // 收到一条新消息。
    // 业务处理成功。
    // 数据库记录成功。
    // 最终向 MQ 发送了正确的 ACK 信号。
    @Test
    void consume_success_shouldCallBizAndAck() throws Exception {
        when(rabbitUtil.getTag(any())).thenReturn(1L);
        when(processedMessageMapper.isProcessMessage("mid")).thenReturn(false);

        LoanApplication app = new LoanApplication(
                11L,
                2L,
                3L,
                ApplicationStatus.审核中,
                new BigDecimal("1000.00"),
                new BigDecimal("0.10"),
                12,
                12,
                RepaidType.等额本息,
                null,
                LocalDateTime.now(),
                null
        );
        Message message = buildMessage(objectMapper.writeValueAsBytes(app), "mid", 1L);

        LoanApplicationConsumer consumer = new LoanApplicationConsumer(
                processedMessageMapper,
                objectMapper,
                aiApproveService,
                rabbitUtil
        );
        consumer.consume(message, channel);

        verify(aiApproveService).AICheck(any(LoanApplication.class));
        verify(processedMessageMapper).insertMessage("mid", "LOAN_APPLICATION", 11L);
        verify(channel).basicAck(1L, false);
    }

    // 验证当消息中缺少“消息ID”时，消费者拒绝处理该消息（发送 NACK），并且确保不会触发后续的业务逻辑
    @Test
    void consume_missingMessageId_shouldNack() throws Exception {
        when(rabbitUtil.getTag(any())).thenReturn(2L);

        LoanApplication app = new LoanApplication();
        app.setId(1L);
        app.setStatus(ApplicationStatus.审核中);

        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(2L);
        Message message = new Message(objectMapper.writeValueAsBytes(app), props);

        LoanApplicationConsumer consumer = new LoanApplicationConsumer(
                processedMessageMapper,
                objectMapper,
                aiApproveService,
                rabbitUtil
        );
        consumer.consume(message, channel);

        verify(channel).basicNack(2L, false, false);
        verify(aiApproveService, never()).AICheck(any());
    }

    // 验证消息重试，达到最大重试次数，但业务逻辑依然报错时，系统将其移入死信队列（DLQ），并发送 ACK 确认以将其从原队列移除。
    @Test
    void consume_errorWithMaxRetries_shouldMoveToDlqAndAck() throws Exception {
        when(rabbitUtil.getTag(any())).thenReturn(3L);
        when(processedMessageMapper.isProcessMessage("mid")).thenReturn(false);

        LoanApplication app = new LoanApplication();
        app.setId(11L);
        app.setStatus(ApplicationStatus.审核中);
        app.setStatus(ApplicationStatus.审核中);
        app.setStatus(ApplicationStatus.审核中);
        app.setStatus(ApplicationStatus.审核中);
        Message message = buildMessage(objectMapper.writeValueAsBytes(app), "mid", 3L);

        List<Map<String, Object>> xDeath = List.of(buildXDeathEntry(RabbitMQConfig.LOAN_APPLICATION_QUEUE, 3L));
        message.getMessageProperties().setHeader("x-death", xDeath);

        when(aiApproveService.AICheck(any())).thenThrow(new RuntimeException("boom"));

        LoanApplicationConsumer consumer = new LoanApplicationConsumer(
                processedMessageMapper,
                objectMapper,
                aiApproveService,
                rabbitUtil
        );
        consumer.consume(message, channel);

        verify(rabbitUtil).sendToDLX(eq(RabbitMQConfig.DLQ), any(Message.class));
        verify(channel).basicAck(3L, false);
    }

    private static Map<String, Object> buildXDeathEntry(String queue, long count) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("queue", queue);
        entry.put("count", count);
        return entry;
    }

    private static Message buildMessage(byte[] body, String messageId, long tag) {
        MessageProperties props = new MessageProperties();
        props.setHeader("messageId", messageId);
        props.setContentType("application/json");
        props.setDeliveryTag(tag);
        return new Message(body, props);
    }
}
