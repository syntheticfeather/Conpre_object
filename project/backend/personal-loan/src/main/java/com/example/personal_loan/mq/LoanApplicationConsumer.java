package com.example.personal_loan.mq;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import com.example.personal_loan.config.RabbitMQConfig;
import com.example.personal_loan.entity.LoanApplication;
import com.example.personal_loan.mapper.ProcessMessageMapper;
import com.example.personal_loan.service.AIApproveService;
import com.example.personal_loan.utils.RabbitUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
@RequiredArgsConstructor
public class LoanApplicationConsumer {

    private final ProcessMessageMapper processedMessageMapper;
    private final ObjectMapper objectMapper;
    private final AIApproveService aiApproveService; // 你的业务服务
    private final RabbitUtil rabbitUtil;
    private static final int MAX_RETRIES = 3;

    @RabbitListener(queues = RabbitMQConfig.LOAN_APPLICATION_QUEUE)
    public void consume(Message message, Channel channel) throws IOException {
        String payload = new String(message.getBody());
        String messageId = message.getMessageProperties().getHeader("messageId");
        log.info("🔔 收到消息: {}", payload);
        Long tag = rabbitUtil.getTag(message);

        if (messageId == null) {
            log.error("❌ 消息缺少 messageId 头");
            channel.basicNack(tag, false, false);
            return;
        }

        // 幂等检查
        if (processedMessageMapper.isProcessMessage(messageId)) {
            log.info("🔄 消息已处理: {}", messageId);
            channel.basicAck(tag, false); //  Ack
            return;
        }
        try {
            // 进入业务逻辑
            LoanApplication app = objectMapper.readValue(payload, LoanApplication.class);
            aiApproveService.AICheck(app);
            markAsProcessed(messageId, "LOAN_APPLICATION", app.getId());
            channel.basicAck(tag, false);
            log.info("处理成功: {}", messageId);
        } catch (Exception e) {
            // 读取主队列的 x-death 次数
            int deathCount = getDeathCount(message, RabbitMQConfig.LOAN_APPLICATION_QUEUE);
            // 超过最大重试次数(3次)，转入 DLQ并 ack 原消息，以避免再次重试
            if (deathCount >= MAX_RETRIES) {   // 或者直接直接 Ack 掉原消息，手动记录死信？
                Message dlqMsg = copyWithHeaders(message, messageId);
                rabbitUtil.sendToDLX(RabbitMQConfig.DLQ, dlqMsg);
                channel.basicAck(tag, false);
                log.error("超过最大重试次数,转入DLQ: {}", messageId, e);
                return;
            }
            //  小于3次，Nack 并重新入队
            channel.basicNack(tag, false, false);
            log.error("❌ 处理失败: {}", messageId, e);
        }
    }

    private void markAsProcessed(String messageId, String businessType, Long loanApplicationId) {
        try {
            processedMessageMapper.insertMessage(messageId, businessType, loanApplicationId);
        } catch (DuplicateKeyException e) {
            // 并发场景下可能重复插入，忽略
            log.warn("幂等记录已存在: {}", messageId);
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

