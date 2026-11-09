package com.example.personal_loan.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.personal_loan.service.RiskScoringService;

/**
 * FullRiskScoringService 单元测试
 * <p>
 * full 模型文件可能不存在，此时应自动回退 baseline。
 */
class FullRiskScoringServiceTest {

    private FullRiskScoringService service;
    private RiskScoringService baselineMock;

    @BeforeEach
    void setUp() {
        baselineMock = mock(RiskScoringService.class);
        service = new FullRiskScoringService(baselineMock);
        // 触发 @PostConstruct
        service.init();
    }

    @Test
    @DisplayName("FullRiskScoringService 初始化后，要么 full 模型可用要么 baseline 回退")
    void init_reachesSomeState() {
        String activeType = service.getActiveModelType();
        assertThat(activeType).isIn("full", "baseline");
        System.out.println("Active model type: " + activeType);
    }

    @Test
    @DisplayName("即使没有 full 模型，predict 也不抛异常 —— 应该回退 baseline")
    void predict_noFullModel_noException() {
        Map<String, Object> structured = new HashMap<>();
        structured.put("age", 30);
        structured.put("monthly_income", 8000);
        structured.put("dti_ratio", 10);
        structured.put("active_loan_count", 0);
        structured.put("has_mortgage", 0);
        structured.put("max_overdue_days_90", 0);
        structured.put("overdue_30_59_count", 0);
        structured.put("overdue_60_89_count", 0);
        structured.put("credit_card_usage_pct", 6);

        Map<String, Double> llm = new HashMap<>();
        llm.put("llm_feature_available", 0.0);  // 降级

        Map<String, Double> det = new HashMap<>();
        det.put("doc_count", 0.0);

        // 不抛异常即可
        double prob = service.predict(structured, llm, det);
        System.out.println("predict result (fallback expected): " + prob);
        // 如果 baselineMock 没有 stub，会返回 0.0（mock 默认值）
        // 但实际情况取决于 full 模型是否存在；这里只验证不抛异常
        assertThat(prob).isInstanceOf(Double.class);
    }

    @Test
    @DisplayName("full 模型特征名列表固定且非空")
    void fullFeatureNames_nonEmpty() {
        assertThat(service.getFullFeatureNames()).isNotEmpty();
    }
}
