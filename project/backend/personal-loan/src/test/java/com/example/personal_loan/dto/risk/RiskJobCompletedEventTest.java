package com.example.personal_loan.dto.risk;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * RiskJobCompletedEvent 单元测试（L7.2）
 * <p>
 * 覆盖 Python → Java 的 snake_case 反序列化，以及 llm_feature_available 的取值优先级。
 */
class RiskJobCompletedEventTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("snake_case 字段可正确反序列化，未知字段被忽略")
    void deserializeSnakeCase() throws Exception {
        String json = """
                {
                  "event": "RISK_JOB_COMPLETED",
                  "job_id": 12,
                  "application_id": 1001,
                  "user_id": 7,
                  "job_version": 1,
                  "status": "ok",
                  "score": 0.18,
                  "features": {"llm_verdict": 1.0, "llm_feature_available": 1.0},
                  "deterministic_features": {"doc_count": 5, "doc_chars_total": 2400},
                  "error_code": null,
                  "model_version": "full_v1",
                  "prompt_version": "prompt_v1",
                  "converter_version": "converter_v1",
                  "model": "deepseek-v4-flash",
                  "cached": true,
                  "reasoning": "ok",
                  "llm_feature_available": 1,
                  "unknown_field": "ignored"
                }
                """;

        RiskJobCompletedEvent event = objectMapper.readValue(json, RiskJobCompletedEvent.class);

        assertThat(event.getEvent()).isEqualTo("RISK_JOB_COMPLETED");
        assertThat(event.getJobId()).isEqualTo(12L);
        assertThat(event.getApplicationId()).isEqualTo(1001L);
        assertThat(event.getUserId()).isEqualTo(7L);
        assertThat(event.getJobVersion()).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo("ok");
        assertThat(event.getScore()).isEqualTo(0.18);
        assertThat(event.getFeatures()).containsKey("llm_verdict");
        assertThat(event.getDeterministicFeatures()).containsEntry("doc_count", 5);
        assertThat(event.getModelVersion()).isEqualTo("full_v1");
        assertThat(event.getPromptVersion()).isEqualTo("prompt_v1");
        assertThat(event.getConverterVersion()).isEqualTo("converter_v1");
        assertThat(event.getModel()).isEqualTo("deepseek-v4-flash");
        assertThat(event.getCached()).isTrue();
        assertThat(event.getReasoning()).isEqualTo("ok");
        assertThat(event.getLlmFeatureAvailable()).isEqualTo(1);
    }

    @Test
    @DisplayName("显式 llm_feature_available 优先于 features 内的值")
    void explicitFlagWins() {
        RiskJobCompletedEvent event = new RiskJobCompletedEvent();
        Map<String, Object> features = new HashMap<>();
        features.put("llm_feature_available", 1.0);
        event.setFeatures(features);
        event.setLlmFeatureAvailable(0);

        assertThat(event.isLlmFeatureAvailable()).isFalse();
    }

    @Test
    @DisplayName("显式字段为空时回退到 features 内的 llm_feature_available")
    void fallbackToFeatures() {
        RiskJobCompletedEvent event = new RiskJobCompletedEvent();
        Map<String, Object> features = new HashMap<>();
        features.put("llm_feature_available", 1.0);
        event.setFeatures(features);

        assertThat(event.isLlmFeatureAvailable()).isTrue();
    }

    @Test
    @DisplayName("features 中该值为 0 或非数字 → 视为不可用")
    void featuresFlagNotAvailable() {
        RiskJobCompletedEvent zero = new RiskJobCompletedEvent();
        Map<String, Object> features = new HashMap<>();
        features.put("llm_feature_available", 0.0);
        zero.setFeatures(features);
        assertThat(zero.isLlmFeatureAvailable()).isFalse();

        RiskJobCompletedEvent text = new RiskJobCompletedEvent();
        Map<String, Object> textFeatures = new HashMap<>();
        textFeatures.put("llm_feature_available", "1");
        text.setFeatures(textFeatures);
        assertThat(text.isLlmFeatureAvailable()).isFalse();
    }

    @Test
    @DisplayName("没有 features 也没有显式字段 → 不可用")
    void noFlags() {
        RiskJobCompletedEvent event = new RiskJobCompletedEvent();
        assertThat(event.isLlmFeatureAvailable()).isFalse();
    }
}
