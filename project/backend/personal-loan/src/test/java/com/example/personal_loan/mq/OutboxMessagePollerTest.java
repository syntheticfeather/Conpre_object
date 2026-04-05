package com.example.personal_loan.mq;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.personal_loan.entity.OutboxMessage;
import com.example.personal_loan.mapper.OutboxMapper;
import com.example.personal_loan.utils.RabbitUtil;

@ExtendWith(MockitoExtension.class)
class OutboxMessagePollerTest {

    @Mock
    private OutboxMapper outboxMapper;

    @Mock
    private RabbitUtil rabbitUtil;

    //测试正常发送流程
    @Test
    void pollAndSendOutboxMessages_shouldSendMessage() {
        OutboxMessage outbox = new OutboxMessage();
        outbox.setMessageId("mid");
        outbox.setTopic("rk");
        outbox.setPayload("{\"k\":\"v\"}");
        outbox.setCreatedAt(LocalDateTime.now());

        when(outboxMapper.selectPendingMessages(8)).thenReturn(List.of(outbox));
        when(outboxMapper.updateStatusToSending("mid")).thenReturn(1);

        OutboxMessagePoller poller = new OutboxMessagePoller();
        ReflectionTestUtils.setField(poller, "outboxMapper", outboxMapper);
        ReflectionTestUtils.setField(poller, "rabbitUtil", rabbitUtil);

        poller.pollAndSendOutboxMessages();

        ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
        ArgumentCaptor<CorrelationData> cdCaptor = ArgumentCaptor.forClass(CorrelationData.class);
        verify(rabbitUtil).sendToApp(eq("rk"), msgCaptor.capture(), cdCaptor.capture());
        assertEquals("mid", cdCaptor.getValue().getId());
        assertEquals("mid", msgCaptor.getValue().getMessageProperties().getHeaders().get("messageId"));
    }

    // 测试获取锁失败流程
    @Test
    void pollAndSendOutboxMessages_whenLockFailed_shouldSkipSend() {
        OutboxMessage outbox = new OutboxMessage();
        outbox.setMessageId("mid");
        outbox.setTopic("rk");
        outbox.setPayload("{\"k\":\"v\"}");
        outbox.setCreatedAt(LocalDateTime.now());

        when(outboxMapper.selectPendingMessages(8)).thenReturn(List.of(outbox));
        when(outboxMapper.updateStatusToSending("mid")).thenReturn(0);

        OutboxMessagePoller poller = new OutboxMessagePoller();
        ReflectionTestUtils.setField(poller, "outboxMapper", outboxMapper);
        ReflectionTestUtils.setField(poller, "rabbitUtil", rabbitUtil);

        poller.pollAndSendOutboxMessages();

        verify(rabbitUtil, never()).sendToApp(any(), any(), any());
    }

    // 验证当发送消息到 MQ 时出错时，系统将其标记为“失败”，并重试发送。
    @Test
    void sendAndMarkMessage_whenSendThrows_shouldMarkFailed() {
        OutboxMessage outbox = new OutboxMessage();
        outbox.setMessageId("mid");
        outbox.setTopic("rk");
        outbox.setPayload("{\"k\":\"v\"}");
        outbox.setCreatedAt(LocalDateTime.now());

        doThrow(new RuntimeException("boom")).when(rabbitUtil).sendToApp(eq("rk"), any(), any());
        OutboxMessagePoller poller = new OutboxMessagePoller();
        ReflectionTestUtils.setField(poller, "outboxMapper", outboxMapper);
        ReflectionTestUtils.setField(poller, "rabbitUtil", rabbitUtil);

        poller.sendAndMarkMessage(outbox);

        verify(outboxMapper).markAsFailed("mid");
    }
}
