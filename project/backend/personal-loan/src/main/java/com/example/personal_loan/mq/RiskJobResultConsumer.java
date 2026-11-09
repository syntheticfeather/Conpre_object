package com.example.personal_loan.mq;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import com.example.personal_loan.config.RabbitMQConfig;
import com.example.personal_loan.dto.risk.RiskJobCompletedEvent;
import com.example.personal_loan.mapper.ProcessMessageMapper;
import com.example.personal_loan.service.RiskJobResultHandler;
import com.example.personal_loan.utils.RabbitUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * L7.2 风控结果消费者。
 * <p>
 * 消费 {@code risk.job.completed.queue}：
 * <ul>
 *   <li>processed_message 幂等表去重（message_id）</li>
 *   <li>{@link RiskJobResultHandler} 在事务内落库 + 写通知 Outbox</li>
 *   <li>失败 NACK 走 DLX → 重试队列（TTL 10s）→ 原队列；超过 3 次进 DLQ</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RiskJobResultConsumer {

    private static final int MAX_RETRIES = 3;
    private static final String BUSINESS_TYPE = "RISK_JOB";

    private final RiskJobResultHandler resultHandler;
    private final ProcessMessageMapper processedMessageMapper;
    private final ObjectMapper objectMapper;
    private final RabbitUtil rabbitUtil;

    @RabbitListener(queues = RabbitMQConfig.RISK_JOB_COMPLETED_QUEUE)
    public void consume(Message message, Channel channel) throws IOException {
        String payload = new String(message.getBody(), StandardCharsets.UTF_8);
        String messageId = message.getMessageProperties().getHeader("messageId");
        Long tag = rabbitUtil.getTag(message);

        if (messageId == null) {
            log.error("[L7.2] 结果消息缺少 messageId，直接丢弃");
            channel.basicNack(tag, false, false);
            return;
        }

        if (processedMessageMapper.isProcessMessage(messageId)) {
            log.info("[L7.2] 结果消息已处理，ACK 跳过 messageId={}", messageId);
            channel.basicAck(tag, false);
            return;
        }

        try {
            RiskJobCompletedEvent event = objectMapper.readValue(payload, RiskJobCompletedEvent.class);
            resultHandler.apply(event);
            markAsProcessed(messageId, event.getApplicationId());
            channel.basicAck(tag, false);
        } catch (Exception e) {
            int deathCount = getDeathCount(message, RabbitMQConfig.RISK_JOB_COMPLETED_QUEUE);
            if (deathCount >= MAX_RETRIES) {
                // 注意：dlq 队列绑定的 routing key 是 DLQ_ROUTING_KEY，不是 DLQ 常量
                rabbitUtil.sendToDLX(RabbitMQConfig.DLQ_ROUTING_KEY, copyWithHeaders(message, messageId));
                channel.basicAck(tag, false);
                log.error("[L7.2] 结果消息进入 DLQ messageId={}", messageId, e);
                return;
            }
            channel.basicNack(tag, false, false);
            log.error("[L7.2] 结果消息消费失败，等待重试 messageId={} deathCount={}",
                    messageId, deathCount, e);
        }
    }

    private void markAsProcessed(String messageId, Long applicationId) {
        try {
            processedMessageMapper.insertMessage(messageId, BUSINESS_TYPE, applicationId);
        } catch (DuplicateKeyException e) {
            log.warn("[L7.2] processed_message 已存在 messageId={}", messageId);
        } catch (Exception e) {
            // 幂等表写入失败不应阻塞 ACK：DB 终态已经是幂等兜底
            log.error("[L7.2] processed_message 写入失败 messageId={}", messageId, e);
        }
    }

    private int getDeathCount(Message message, String queue) {
        Map<String, Object> headers = message.getMessageProperties().getHeaders();
        Object xDeath = headers.get("x-death");
        if (!(xDeath instanceof List)) {
            return 0;
        }
        int count = 0;
        for (Object entry : (List<?>) xDeath) {
            if (entry instanceof Map) {
                Object q = ((Map<?, ?>) entry).get("queue");
                Object c = ((Map<?, ?>) entry).get("count");
                if (q != null && q.toString().equals(queue) && c instanceof Long) {
                    count = Math.max(count, ((Long) c).intValue());
                }
            }
        }
        return count;
    }

    private Message copyWithHeaders(Message origin, String messageId) {
        MessageProperties props = new MessageProperties();
        props.getHeaders().putAll(origin.getMessageProperties().getHeaders());
        props.setHeader("messageId", messageId);
        props.setContentType(origin.getMessageProperties().getContentType());
        return new Message(origin.getBody(), props);
    }
}
