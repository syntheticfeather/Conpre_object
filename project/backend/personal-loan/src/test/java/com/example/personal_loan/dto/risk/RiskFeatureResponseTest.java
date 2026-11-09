package com.example.personal_loan.dto.risk;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * RiskFeatureResponse 便捷方法测试
 */
class RiskFeatureResponseTest {

    @Test
    @DisplayName("LLM 特征可用 → isLlmFeatureAvailable=true")
    void llmAvailable_true() {
        RiskFeatureResponse resp = new RiskFeatureResponse();
        Map<String, Object> features = new HashMap<>();
        features.put("llm_feature_available", 1.0);
        features.put("llm_verdict", 1.0);
        features.put("llm_confidence", 0.86);
        resp.setFeatures(features);
        resp.setStatus("ok");

        assertThat(resp.isLlmFeatureAvailable()).isTrue();
        assertThat(resp.isDegraded()).isFalse();
    }

    @Test
    @DisplayName("Python 降级 → llm_feature_available=0 → 判定不可用")
    void llmAvailable_degradedFeatures() {
        RiskFeatureResponse resp = new RiskFeatureResponse();
        Map<String, Object> features = new HashMap<>();
        features.put("llm_feature_available", 0.0);
        features.put("llm_verdict", 0.0);
        features.put("llm_confidence", 0.0);
        resp.setFeatures(features);
        resp.setStatus("error");
        resp.setErrorCode("TIMEOUT");

        assertThat(resp.isLlmFeatureAvailable()).isFalse();
        assertThat(resp.isDegraded()).isTrue();
    }

    @Test
    @DisplayName("features 为空 → 判定不可用")
    void llmAvailable_emptyFeatures() {
        RiskFeatureResponse resp = new RiskFeatureResponse();
        resp.setFeatures(null);
        assertThat(resp.isLlmFeatureAvailable()).isFalse();
        assertThat(resp.isDegraded()).isTrue();

        resp.setFeatures(new HashMap<>());
        assertThat(resp.isLlmFeatureAvailable()).isFalse();
    }

    @Test
    @DisplayName("status=error 也算 degraded")
    void degraded_errorStatus() {
        RiskFeatureResponse resp = new RiskFeatureResponse();
        Map<String, Object> features = new HashMap<>();
        features.put("llm_feature_available", 1.0);
        resp.setFeatures(features);
        resp.setStatus("error");
        assertThat(resp.isDegraded()).isTrue();
    }

    @Test
    @DisplayName("JSON 解析时 Integer 也能正确转 Double")
    void llmAvailable_integerValue() {
        RiskFeatureResponse resp = new RiskFeatureResponse();
        Map<String, Object> features = new HashMap<>();
        // Jackson 解析 JSON {"llm_feature_available": 1} 会得到 Integer 1
        features.put("llm_feature_available", 1);
        resp.setFeatures(features);
        assertThat(resp.isLlmFeatureAvailable()).isTrue();

        features.put("llm_feature_available", 0);
        assertThat(resp.isLlmFeatureAvailable()).isFalse();
    }
}
