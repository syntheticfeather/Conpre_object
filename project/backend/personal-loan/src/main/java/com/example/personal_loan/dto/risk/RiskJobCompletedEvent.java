package com.example.personal_loan.dto.risk;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * 风控任务完成事件（L7.2）。
 * <p>
 * 由 Python Worker 发布到 {@code risk.exchange} / {@code risk.job.completed}，
 * 由 Java {@code RiskJobResultConsumer} 消费。
 * <p>
 * status 取值约定：
 * <ul>
 *   <li>{@code ok}：LLM 特征正常</li>
 *   <li>{@code error}：Python 侧降级</li>
 * </ul>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RiskJobCompletedEvent {

    @JsonProperty("event")
    private String event;

    @JsonProperty("job_id")
    private Long jobId;

    @JsonProperty("application_id")
    private Long applicationId;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("job_version")
    private Integer jobVersion;

    /** ok / error */
    @JsonProperty("status")
    private String status;

    /** 违约概率 0~1（已废弃，最终分数由 Java 端 {@code FullRiskScoringService} 计算） */
    @Deprecated
    @JsonProperty("score")
    private Double score;

    /** LLM 特征（G 组），含 llm_feature_available */
    @JsonProperty("features")
    private Map<String, Object> features;

    /** 确定性特征（H 组） */
    @JsonProperty("deterministic_features")
    private Map<String, Object> deterministicFeatures;

    @JsonProperty("error_code")
    private String errorCode;

    @JsonProperty("model_version")
    private String modelVersion;

    @JsonProperty("prompt_version")
    private String promptVersion;

    @JsonProperty("converter_version")
    private String converterVersion;

    @JsonProperty("model")
    private String model;

    @JsonProperty("cached")
    private Boolean cached;

    @JsonProperty("reasoning")
    private String reasoning;

    /** LLM 特征是否可用（兼容 Python 直接给出该字段的情况） */
    @JsonProperty("llm_feature_available")
    private Integer llmFeatureAvailable;

    /**
     * 判断 LLM 特征是否可用：
     * 优先看显式字段，其次看 features 里的 llm_feature_available。
     */
    public boolean isLlmFeatureAvailable() {
        if (llmFeatureAvailable != null) {
            return llmFeatureAvailable == 1;
        }
        if (features == null) {
            return false;
        }
        Object available = features.get("llm_feature_available");
        if (available instanceof Number) {
            return ((Number) available).doubleValue() == 1.0;
        }
        return false;
    }
}
