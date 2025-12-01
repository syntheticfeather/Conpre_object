package com.example.personal_loan.mq;

import com.example.personal_loan.config.RabbitMQConfig;
import com.example.personal_loan.entity.LoanApplication;
import com.example.personal_loan.mapper.ProcessMessageMapper;
import com.example.personal_loan.service.AIApproveService;
import com.example.personal_loan.utils.RabbitUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Slf4j
@Component
@RequiredArgsConstructor
public class LoanApplicationConsumer {

    private final ProcessMessageMapper processedMessageMapper;
    private final ObjectMapper objectMapper;
    private final AIApproveService aiApproveService; // 你的业务服务
    private final RabbitUtil rabbitUtil;

    @RabbitListener(queues = RabbitMQConfig.LOAN_APPLICATION_QUEUE)
    public void consume(Message message, Channel channel) throws IOException {
        String payload = new String(message.getBody());
        String messageId = message.getMessageProperties().getHeader("messageId");
        Long tag = rabbitUtil.getTag(message);

        if (messageId == null) {
            log.error("❌ 消息缺少 messageId 头");
            channel.basicNack(tag, false, false);
            return;
        }

        // 幂等检查
        if (processedMessageMapper.isProcessMessage(messageId)) {
            log.info("🔄 消息已处理: {}", messageId);
            return;
        }

        try {
            // 进入业务逻辑
            LoanApplication app = objectMapper.readValue(payload, LoanApplication.class);
            aiApproveService.AICheck(app);
            markAsProcessed(messageId, "LOAN_APPLICATION", app.getId());
            channel.basicAck(tag, false);
            log.info("✅ 处理成功: {}", messageId);
        } catch (Exception e) {
            // 处理失败，将消息重新放入队列
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
}