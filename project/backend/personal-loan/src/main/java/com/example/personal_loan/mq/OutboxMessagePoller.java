package com.example.personal_loan.mq;

import com.example.personal_loan.entity.OutboxMessage;
import com.example.personal_loan.mapper.OutboxMapper;
import com.example.personal_loan.utils.RabbitUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
    public void pollAndSendOutboxMessages() {
//        log.info("🔎 轮询器开始工作...");
        try {
            List<OutboxMessage> pendingMessages = outboxMapper.selectPendingMessages(BATCH_SIZE);
            if (pendingMessages.isEmpty()) {
                return;
            }
            log.info("🔍 找到 {} 条待发送消息，开始处理...", pendingMessages.size());
            for (OutboxMessage message : pendingMessages) {
                sendAndMarkMessage(message);
            }
        } catch (Exception e) {
            log.error("🚨 轮询器异常", e);
        }
    }

    private Message createMessageWithHeaders(OutboxMessage outbox) {
        MessageProperties props = new MessageProperties();
        props.setHeader("messageId", outbox.getMessageId()); // ← 放入 header
        props.setContentType("application/json");
        return new Message(outbox.getPayload().getBytes(), props);
    }

    @Transactional
    public void sendAndMarkMessage (OutboxMessage message){
        try {
            // 发送消息到 RabbitMQ
            rabbitUtil.sendToApp(
                    message.getTopic(),
                    createMessageWithHeaders(message)
            );
            // 标记为已发送（在独立事务中）
            outboxMapper.markAsSent(message.getMessageId());
            log.info("✅ 消息发送成功: messageId={}, topic={}",
                    message.getMessageId(), message.getTopic());
        } catch (Exception e) {
            log.info("❌ 发送消息失败，标记为 FAILED: messageId={}",
                    message.getMessageId(), e);
            outboxMapper.markAsFailed(message.getMessageId());
        }
    }
}
