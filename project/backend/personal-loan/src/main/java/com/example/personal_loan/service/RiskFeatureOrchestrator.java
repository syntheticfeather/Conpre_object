package com.example.personal_loan.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.personal_loan.client.RiskFeatureClient;
import com.example.personal_loan.client.RiskFeatureClient.RiskFeatureClientException;
import com.example.personal_loan.dto.risk.RiskFeatureRequest;
import com.example.personal_loan.dto.risk.RiskFeatureResponse;
import com.example.personal_loan.entity.LoanApplication;
import com.example.personal_loan.entity.RiskJob;
import com.example.personal_loan.entity.UserCert;
import com.example.personal_loan.mapper.ApplicationMapper;
import com.example.personal_loan.mapper.RiskJobMapper;
import com.example.personal_loan.mapper.UserCertMapper;
import com.example.personal_loan.service.assembler.RiskFeatureAssembler;
import com.example.personal_loan.service.impl.FullRiskScoringService;
import com.example.personal_loan.utils.RiskFeatureConverter;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * L7.1 风控特征同步闭环 —— 编排器
 * <p>
 * 把 Java 主业务的申请 + 材料 → 调 Python FastAPI → 合并特征 → full 模型打分 → 写 risk_job。
 * <p>
 * 使用方式：{@code ApplicationServiceImpl.addApplication()} 里在现有 9 维 baseline 打分之后调用。
 * 如果 feature flag {@code risk.llm.enabled=false}，本服务直接跳过，完全不发起 Python 调用。
 * <p>
 * 降级策略：
 * <ul>
 *   <li>Python 超时 / 401 / 5xx / INVALID_PATH → G 组全 0 + llm_feature_available=0</li>
 *   <li>仍然进入同一个 full XGBoost → 得到降级分</li>
 *   <li>risk_job.status = "DEGRADED"</li>
 *   <li>不会回滚申请；不会自动通过（由 AIApproveService 按分数阈值决策）</li>
 * </ul>
 */
@Slf4j
@Service
public class RiskFeatureOrchestrator {

    private final RiskFeatureClient client;
    private final RiskFeatureAssembler assembler;
    private final FullRiskScoringService fullScoringService;
    private final MlTrainingLogService mlTrainingLogService;
    private final RiskJobMapper riskJobMapper;
    private final ApplicationMapper applicationMapper;
    private final UserCertMapper userCertMapper;
    private final ObjectMapper objectMapper;

    /** feature flag：开启/关闭 L7.1 新链路 */
    @Value("${risk.llm.enabled:false}")
    private boolean llmEnabled;

    /** L7.1 的 job_version 固定为 1；L7.2 异步可用 version=2 来区分 */
    private static final int L71_JOB_VERSION = 1;

    public RiskFeatureOrchestrator(
            RiskFeatureClient client,
            RiskFeatureAssembler assembler,
            FullRiskScoringService fullScoringService,
            MlTrainingLogService mlTrainingLogService,
            RiskJobMapper riskJobMapper,
            ApplicationMapper applicationMapper,
            UserCertMapper userCertMapper,
            ObjectMapper objectMapper) {
        this.client = client;
        this.assembler = assembler;
        this.fullScoringService = fullScoringService;
        this.mlTrainingLogService = mlTrainingLogService;
        this.riskJobMapper = riskJobMapper;
        this.applicationMapper = applicationMapper;
        this.userCertMapper = userCertMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * L7.1 同步闭环主入口
     *
     * @param userId        用户 ID
     * @param applicationId 申请 ID
     * @return 最终的违约概率（0~1）；如果完整链路被跳过或失败，返回 -1 表示未产生有效 full 打分
     */
    @Transactional
    public double execute(Long userId, Long applicationId) {
        // feature flag 关闭则直接跳过
        if (!llmEnabled) {
            log.info("[L7.1] risk.llm.enabled=false，跳过 LLM 特征链路 userId={} appId={}",
                    userId, applicationId);
            return -1;
        }

        // 幂等：同一 application_id + version=1 已存在就不重复处理
        RiskJob existing = riskJobMapper.selectByAppVersion(applicationId, L71_JOB_VERSION);
        if (existing != null) {
            log.info("[L7.1] risk_job 已存在，跳过 userId={} appId={} jobId={}",
                    userId, applicationId, existing.getJobId());
            return existing.getScore() != null ? existing.getScore().doubleValue() : -1;
        }

        LocalDateTime startedAt = LocalDateTime.now();
        RiskJob job = new RiskJob();
        job.setApplicationId(applicationId);
        job.setUserId(userId);
        job.setJobVersion(L71_JOB_VERSION);
        job.setAttempt(0);
        job.setStatus("FAILED");  // 先标记失败，后面成功时更新
        job.setFeatureSet("full");
        job.setModelVersion(fullScoringService.getModelVersion());
        job.setStartedAt(startedAt);

        try {
            // Step 1: 组装请求
            RiskFeatureRequest req = assembler.buildRequest(userId, applicationId);
            int docCount = req.getDocuments() == null ? 0 : req.getDocuments().size();
            job.setDocumentCount(docCount);
            job.setConvertedCount(docCount);  // 假设全成功，后面若有错误 Python 会在响应里反馈

            // Step 2: 调用 Python FastAPI
            RiskFeatureResponse resp = client.extractFeatures(req);
            job.setModel(resp.getModel());
            job.setCached(resp.isCached() ? 1 : 0);
            job.setReasoning(resp.getReasoning());

            // Step 3: 结构化特征（Java 侧 9 维）
            Map<String, Object> structured = mlTrainingLogService.extractModelFeatures(
                    userId, applicationId);

            // Step 4: 合并三路特征 → full 模型打分
            Map<String, Double> llmFeatures = RiskFeatureConverter.toDoubleMap(resp.getFeatures());
            Map<String, Double> deterministicFeatures = RiskFeatureConverter.toDoubleMap(resp.getDeterministicFeatures());

            double prob = fullScoringService.predict(structured, llmFeatures, deterministicFeatures);

            // Step 5: 组装完整特征 JSON 写入 risk_job
            Map<String, Object> allFeatures = new HashMap<>();
            allFeatures.putAll(structured);
            allFeatures.putAll(resp.getFeatures() != null ? resp.getFeatures() : new HashMap<>());
            allFeatures.putAll(resp.getDeterministicFeatures() != null ? resp.getDeterministicFeatures() : new HashMap<>());
            job.setFeaturesJson(objectMapper.writeValueAsString(allFeatures));

            job.setScore(BigDecimal.valueOf(prob));
            job.setLlmFeatureAvailable(resp.isLlmFeatureAvailable() ? 1 : 0);

            // 决定状态：LLM 特征可用 → COMPLETED，否则 → DEGRADED
            if (resp.isLlmFeatureAvailable() && "ok".equals(resp.getStatus())) {
                job.setStatus("COMPLETED");
            } else {
                job.setStatus("DEGRADED");
                job.setErrorCode(resp.getErrorCode() != null ? resp.getErrorCode() : "LLM_DEGRADED");
            }

            log.info("[L7.1] 成功 userId={} appId={} status={} score={} llmAvailable={} model={}",
                    userId, applicationId, job.getStatus(), prob, job.getLlmFeatureAvailable(),
                    job.getModel());

        } catch (RiskFeatureClientException e) {
            // Python 服务层失败（超时 / 401 / 5xx / INVALID_PATH）——降级继续
            handleDegraded(userId, applicationId, job, "CLIENT_" + e.getClientErrorCode(), e);
        } catch (Exception e) {
            log.error("[L7.1] 未预期异常 userId={} appId={}: {}",
                    userId, applicationId, e.getMessage(), e);
            job.setStatus("FAILED");
            job.setErrorCode("INTERNAL_ERROR");
            job.setReasoning(e.getMessage());
        } finally {
            job.setFinishedAt(LocalDateTime.now());
            // 只在第一次不存在时 insert；后面 update 会走 updateResult（L7.1 同步版不需要 update）
            riskJobMapper.insert(job);
        }

        // 同步把信用分写到 user_certification（供后续流程使用）
        updateUserCertCreditScore(userId, job);

        return job.getScore() != null ? job.getScore().doubleValue() : -1;
    }

    // ──────────────────────────────────────────────────────
    //  内部：降级处理
    // ──────────────────────────────────────────────────────
    private void handleDegraded(Long userId, Long applicationId, RiskJob job,
                                String errorCode, Exception e) {
        log.warn("[L7.1] 降级 userId={} appId={} code={}: {}",
                userId, applicationId, errorCode, e.getMessage());

        // 降级时仍然跑 baseline 模型得分，给下游 AIApproveService 一个可以用的值
        try {
            Map<String, Object> structured = mlTrainingLogService.extractModelFeatures(
                    userId, applicationId);
            // 用全 0 的 LLM 特征跑 full 模型（FullRiskScoringService 内部已做了缺失值处理）
            Map<String, Double> emptyLlm = new HashMap<>();
            emptyLlm.put("llm_feature_available", 0.0);
            Map<String, Double> emptyDet = new HashMap<>();
            double prob = fullScoringService.predict(structured, emptyLlm, emptyDet);
            job.setScore(BigDecimal.valueOf(prob));
            job.setLlmFeatureAvailable(0);
            job.setErrorCode(errorCode);
            job.setStatus("DEGRADED");
            job.setReasoning(e.getMessage());

            Map<String, Object> allFeatures = new HashMap<>(structured);
            allFeatures.putAll(emptyLlm);
            allFeatures.putAll(emptyDet);
            job.setFeaturesJson(objectMapper.writeValueAsString(allFeatures));
        } catch (Exception fallbackEx) {
            log.error("[L7.1] 降级 baseline 打分也失败 userId={}: {}",
                    userId, fallbackEx.getMessage());
            job.setStatus("FAILED");
            job.setErrorCode(errorCode);
            job.setReasoning(e.getMessage());
        }
    }

    // ──────────────────────────────────────────────────────
    //  内部：更新 user_certification.credit_score
    // ──────────────────────────────────────────────────────
    private void updateUserCertCreditScore(Long userId, RiskJob job) {
        if (job.getScore() == null) return;
        try {
            // full 模型给出违约概率 prob，转成信用分
            int score = RiskFeatureConverter.toCreditScore(job.getScore().doubleValue());
            int rows = userCertMapper.updateCreditScore(userId, score);
            if (rows > 0) {
                log.info("[L7.1] 更新 credit_score userId={} score={} model={}",
                        userId, score, job.getFeatureSet());
            }
        } catch (Exception e) {
            log.error("[L7.1] 更新 credit_score 失败 userId={}: {}", userId, e.getMessage());
        }
    }

    /** 暴露 feature flag 状态（供 ApplicationServiceImpl 或监控使用） */
    public boolean isEnabled() {
        return llmEnabled;
    }

}
