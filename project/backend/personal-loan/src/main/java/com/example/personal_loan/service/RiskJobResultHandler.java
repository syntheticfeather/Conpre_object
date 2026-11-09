package com.example.personal_loan.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.personal_loan.dto.risk.RiskJobCompletedEvent;
import com.example.personal_loan.entity.RiskJob;
import com.example.personal_loan.mapper.RiskJobMapper;
import com.example.personal_loan.mapper.UserCertMapper;
import com.example.personal_loan.mq.NotificationOutboxPublisher;
import com.example.personal_loan.service.impl.FullRiskScoringService;
import com.example.personal_loan.utils.RiskFeatureConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * L7.2 风控结果落库与通知（事务边界）。
 * <p>
 * 由 {@code RiskJobResultConsumer} 调用；本类负责：
 * <ol>
 *   <li>校验 risk_job 存在且未处于终态（幂等）</li>
 *   <li>从 Java 侧提取结构化特征，并与 Python 回传的 G/H 组特征合并</li>
 *   <li>调用 {@link FullRiskScoringService} 计算 full 模型分数</li>
 *   <li>把分数 / 特征 / 版本写回 risk_job</li>
 *   <li>按 full 模型分数回写 user_certification.credit_score</li>
 *   <li>写一条 {@code LOAN_APPLICATION_STATUS} 通知（Outbox）</li>
 * </ol>
 * 任何一步异常都会抛出，由消费者 NACK 触发重试 / DLQ。
 * <p>
 * 设计约束：Python 侧 LLM 只输出 G/H 特征，<b>不打分</b>；最终违约概率由 Java 端 full XGBoost 模型给出。
 */
@Slf4j
@Service
public class RiskJobResultHandler {

    /** 已处理完成的终态，重复消息不再重复写 */
    private static final Set<String> FINAL_STATUS = Set.of("COMPLETED", "DEGRADED", "FAILED");

    /** 复用现有通知类型，前端零改动 */
    private static final String NOTIFICATION_TYPE = "LOAN_APPLICATION_STATUS";

    private final RiskJobMapper riskJobMapper;
    private final UserCertMapper userCertMapper;
    private final NotificationOutboxPublisher notificationOutboxPublisher;
    private final ObjectMapper objectMapper;
    private final MlTrainingLogService mlTrainingLogService;
    private final FullRiskScoringService fullRiskScoringService;

    public RiskJobResultHandler(RiskJobMapper riskJobMapper,
                                UserCertMapper userCertMapper,
                                NotificationOutboxPublisher notificationOutboxPublisher,
                                ObjectMapper objectMapper,
                                MlTrainingLogService mlTrainingLogService,
                                FullRiskScoringService fullRiskScoringService) {
        this.riskJobMapper = riskJobMapper;
        this.userCertMapper = userCertMapper;
        this.notificationOutboxPublisher = notificationOutboxPublisher;
        this.objectMapper = objectMapper;
        this.mlTrainingLogService = mlTrainingLogService;
        this.fullRiskScoringService = fullRiskScoringService;
    }

    @Transactional
    public void apply(RiskJobCompletedEvent event) {
        if (event.getJobId() == null) {
            throw new IllegalArgumentException("缺少 job_id");
        }

        RiskJob job = riskJobMapper.selectById(event.getJobId());
        if (job == null) {
            throw new IllegalStateException("risk_job 不存在: " + event.getJobId());
        }
        if (FINAL_STATUS.contains(job.getStatus())) {
            log.info("[L7.2] 重复结果消息，忽略 jobId={} status={}",
                    job.getJobId(), job.getStatus());
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        // 1. 准备特征：结构化 + Python 回传的 G/H 组
        Map<String, Object> structured = loadStructuredFeatures(job.getUserId(), job.getApplicationId());
        Map<String, Double> llmFeatures = RiskFeatureConverter.toDoubleMap(event.getFeatures());
        Map<String, Double> deterministicFeatures = RiskFeatureConverter.toDoubleMap(event.getDeterministicFeatures());

        // 2. 状态判定
        String status = resolveStatus(event);
        job.setStatus(status);
        job.setStage(status);
        job.setErrorCode(event.getErrorCode());

        // 3. 分数计算：FAILED 不回写分数；其余情况用 full 模型计算
        if ("FAILED".equals(status)) {
            job.setScore(null);
            job.setLlmFeatureAvailable(0);
        } else {
            double prob = fullRiskScoringService.predict(structured, llmFeatures, deterministicFeatures);
            job.setScore(BigDecimal.valueOf(prob));
            job.setLlmFeatureAvailable(event.isLlmFeatureAvailable() ? 1 : 0);
        }

        // 4. 写入完整特征 JSON（结构化 + G + H）
        job.setFeaturesJson(mergeFeatures(structured, event));

        // 5. 元信息：模型版本以 Java 端 full 模型为准；prompt/converter 来自 Python 侧
        job.setModelVersion(fullRiskScoringService.getModelVersion());
        job.setPromptVersion(event.getPromptVersion());
        job.setConverterVersion(event.getConverterVersion());
        job.setModel(event.getModel());
        job.setCached(event.getCached() != null && event.getCached() ? 1 : 0);
        job.setReasoning(event.getReasoning());
        job.setStartedAt(job.getStartedAt() != null ? job.getStartedAt() : now);
        job.setFinishedAt(now);

        riskJobMapper.updateCompleted(job);
        updateCreditScore(job);
        notificationOutboxPublisher.enqueueNotification(
                job.getUserId(), job.getApplicationId(), NOTIFICATION_TYPE);

        log.info("[L7.2] 结果已落库 jobId={} appId={} status={} score={} llmAvailable={} modelType={}",
                job.getJobId(), job.getApplicationId(), job.getStatus(),
                job.getScore(), job.getLlmFeatureAvailable(),
                fullRiskScoringService.getActiveModelType());
    }

    private String resolveStatus(RiskJobCompletedEvent event) {
        if ("failed".equalsIgnoreCase(event.getStatus())) {
            return "FAILED";
        }
        boolean ok = "ok".equalsIgnoreCase(event.getStatus()) && event.isLlmFeatureAvailable();
        return ok ? "COMPLETED" : "DEGRADED";
    }

    private Map<String, Object> loadStructuredFeatures(Long userId, Long applicationId) {
        try {
            Map<String, Object> structured = mlTrainingLogService.extractModelFeatures(userId, applicationId);
            if (structured != null) {
                return structured;
            }
        } catch (Exception e) {
            log.warn("[L7.2] 结构化特征加载失败 userId={} appId={}: {}",
                    userId, applicationId, e.getMessage());
        }
        return new HashMap<>();
    }

    private String mergeFeatures(Map<String, Object> structured, RiskJobCompletedEvent event) {
        Map<String, Object> merged = new HashMap<>();
        if (structured != null) {
            merged.putAll(structured);
        }
        if (event.getFeatures() != null) {
            merged.putAll(event.getFeatures());
        }
        if (event.getDeterministicFeatures() != null) {
            merged.putAll(event.getDeterministicFeatures());
        }
        try {
            return objectMapper.writeValueAsString(merged);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("特征 JSON 序列化失败", e);
        }
    }

    /** 与 L7.1 保持一致：信用分 = (1 - 违约概率) * 750 */
    private void updateCreditScore(RiskJob job) {
        if (job.getScore() == null || "FAILED".equals(job.getStatus())) {
            return;
        }
        int creditScore = RiskFeatureConverter.toCreditScore(job.getScore().doubleValue());
        int rows = userCertMapper.updateCreditScore(job.getUserId(), creditScore);
        if (rows > 0) {
            log.info("[L7.2] 更新 credit_score userId={} score={}", job.getUserId(), creditScore);
        }
    }

}
