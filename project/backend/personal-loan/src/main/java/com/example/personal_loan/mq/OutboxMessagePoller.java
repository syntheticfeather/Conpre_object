package com.example.personal_loan.mq;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.personal_loan.entity.OutboxMessage;
import com.example.personal_loan.mapper.OutboxMapper;
import com.example.personal_loan.utils.RabbitUtil;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableScheduling
public class OutboxMessagePoller {

    private static final int BATCH_SIZE = 8; // 每次最多处理 8 条

    @Autowired
    private OutboxMapper outboxMapper;

    @Autowired
    private RabbitUtil rabbitUtil;

    /**
     * 每 5 秒轮询一次待发送消息
     */
    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void pollAndSendOutboxMessages() {
        // log.info("轮询器开始工作...");
        try {
            List<OutboxMessage> pendingMessages = outboxMapper.selectPendingMessages(BATCH_SIZE);
            if (pendingMessages.isEmpty()) {
                return;
            }
            log.info("找到 {} 条待发送消息，开始处理...", pendingMessages.size());
            for (OutboxMessage message : pendingMessages) {
                // 发送前先更新状态为 SENDING
                int rows = outboxMapper.updateStatusToSending(message.getMessageId());
                if (rows == 0) {
                    continue; // 如果更新失败，跳过
                }
                CorrelationData correlationData = new CorrelationData(message.getMessageId());
                sendAndMarkMessage(message, correlationData);
            }
        } catch (Exception e) {
            log.error("轮询器异常", e);
        }
    }

    private Message createMessageWithHeaders(OutboxMessage outbox) {
        MessageProperties props = new MessageProperties();
        props.setHeader("messageId", outbox.getMessageId()); // ← 放入 header
        props.setContentType("application/json");
        return new Message(outbox.getPayload().getBytes(), props);
    }

    public void sendAndMarkMessage (OutboxMessage message, CorrelationData correlationData){
        try {
            rabbitUtil.sendToApp(
                    message.getTopic(),
                    createMessageWithHeaders(message),
                    correlationData
            );
            log.info("消息已投递到Broker: messageId={}, topic={}",
                    message.getMessageId(), message.getTopic());
            outboxMapper.markAsSent(message.getMessageId(), LocalDateTime.now());
        } catch (Exception e) {
            log.info("发送消息失败，标记为 FAILED: messageId={}",
                    message.getMessageId(), e);
            outboxMapper.markAsFailed(message.getMessageId());
        }
    }

    @Scheduled(fixedDelay = 60000)
    public void rollbackExpiredSending() {
        try {
            outboxMapper.resetExpiredSendingToPending(120);
        } catch (Exception e) {
            log.error("回滚SENDING超时消息失败", e);
        }
    }

    public void sendAndMarkMessage(OutboxMessage message) {
        CorrelationData correlationData = new CorrelationData(message.getMessageId());
        sendAndMarkMessage(message, correlationData);
    }
}

