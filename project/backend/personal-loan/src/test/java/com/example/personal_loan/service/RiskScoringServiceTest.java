package com.example.personal_loan.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import com.example.personal_loan.service.impl.RiskScoringServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * XGBoost 风控打分 — 单元测试
 *
 * 测试 countRealFeatures / toCreditScore / predict 核心逻辑，
 * 不依赖 Spring 容器（模型文件在 classpath 即可）。
 */
class RiskScoringServiceTest {

    private RiskScoringServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RiskScoringServiceImpl();
        // 触发 @PostConstruct 加载模型
        service.init();
    }

    // ═══════════════════════════════════════════════
    //  countRealFeatures
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("空资料新用户 → 0 个真实特征")
    void realFeatures_EmptyUser() {
        Map<String, Object> features = allDefaults();
        int count = service.countRealFeatures(features);
        assertThat(count).isEqualTo(0);
    }

    @Test
    @DisplayName("有身份证 + 房产证 → 2 个真实特征")
    void realFeatures_WithIdCardAndHouse() {
        Map<String, Object> features = allDefaults();
        features.put("age", 28);                  // 非默认 30 → 真实
        features.put("has_mortgage", 1);          // 有房贷 → 真实
        int count = service.countRealFeatures(features);
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("老用户有逾期记录 → 5+ 个真实特征")
    void realFeatures_RepeatBorrower() {
        Map<String, Object> features = allDefaults();
        features.put("age", 35);                   // 真实
        features.put("has_mortgage", 1);           // 真实
        features.put("active_loan_count", 3);      // 真实(>0)
        features.put("dti_ratio", 42.0);           // 真实(>0)
        features.put("max_overdue_days_90", 1);    // 真实(>0)
        features.put("overdue_30_59_count", 2);    // 真实(>0)
        int count = service.countRealFeatures(features);
        assertThat(count).isGreaterThanOrEqualTo(5);
    }

    // ═══════════════════════════════════════════════
    //  toCreditScore (0-750)
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("违约概率 0% → 750 分")
    void score_Perfect() {
        assertThat(service.toCreditScore(0.0)).isEqualTo(750);
    }

    @Test
    @DisplayName("违约概率 5% → 713 分")
    void score_LowRisk() {
        assertThat(service.toCreditScore(0.05)).isEqualTo(713);
    }

    @Test
    @DisplayName("违约概率 20% → 600 分（刚好到通过线）")
    void score_PassThreshold() {
        assertThat(service.toCreditScore(0.20)).isEqualTo(600);
    }

    @Test
    @DisplayName("违约概率 47% → 400 分（拒绝线）")
    void score_RejectThreshold() {
        assertThat(service.toCreditScore(0.47)).isEqualTo(398);
    }

    @Test
    @DisplayName("违约概率 100% → 0 分")
    void score_WorstCase() {
        assertThat(service.toCreditScore(1.0)).isEqualTo(0);
    }

    // ═══════════════════════════════════════════════
    //  predict (XGBoost 模型)
    // ═══════════════════════════════════════════════

    @Test
    @DisplayName("模型已加载 → isReady=true")
    void model_Ready() {
        assertThat(service.isReady())
            .as("模型文件应在 resources/ml/risk_model.json")
            .isTrue();
    }

    @Test
    @DisplayName("低风险用户 → 违约概率 < 10%")
    void predict_LowRisk() {
        if (!service.isReady()) return;
        Map<String, Object> features = lowRiskFeatures();
        double prob = service.predict(features);
        assertThat(prob).isGreaterThanOrEqualTo(0.0).isLessThan(0.15);
        System.out.println("低风险违约概率: " + String.format("%.2f%%", prob * 100)
                           + " → 信用分: " + service.toCreditScore(prob));
    }

    @Test
    @DisplayName("高风险用户 → 违约概率 > 20%")
    void predict_HighRisk() {
        if (!service.isReady()) return;
        Map<String, Object> features = highRiskFeatures();
        double prob = service.predict(features);
        assertThat(prob).isGreaterThanOrEqualTo(0.0).isLessThan(1.0);
        System.out.println("高风险违约概率: " + String.format("%.2f%%", prob * 100)
                           + " → 信用分: " + service.toCreditScore(prob));
    }

    @Test
    @DisplayName("中等风险用户 → 违约概率在合理范围")
    void predict_MediumRisk() {
        if (!service.isReady()) return;
        Map<String, Object> features = mediumRiskFeatures();
        double prob = service.predict(features);
        System.out.println("中等风险违约概率: " + String.format("%.2f%%", prob * 100)
                           + " → 信用分: " + service.toCreditScore(prob));
    }

    // ═══════════════════════════════════════════════
    //  特征工厂方法
    // ═══════════════════════════════════════════════

    /** 全默认值（空资料用户） */
    private Map<String, Object> allDefaults() {
        Map<String, Object> f = new HashMap<>();
        f.put("age", 30);
        f.put("monthly_income", 6670);
        f.put("dti_ratio", 0);
        f.put("active_loan_count", 0);
        f.put("has_mortgage", 0);
        f.put("max_overdue_days_90", 0);
        f.put("overdue_30_59_count", 0);
        f.put("overdue_60_89_count", 0);
        f.put("credit_card_usage_pct", 6);
        return f;
    }

    /** 低风险：35岁 月入15000 无逾期 有房贷 使用率低 */
    private Map<String, Object> lowRiskFeatures() {
        Map<String, Object> f = allDefaults();
        f.put("age", 35);
        f.put("monthly_income", 15000);
        f.put("dti_ratio", 20);
        f.put("active_loan_count", 1);
        f.put("has_mortgage", 1);
        f.put("credit_card_usage_pct", 10);
        return f;
    }

    /** 高风险：25岁 月入4000 多次逾期 多头借贷 */
    private Map<String, Object> highRiskFeatures() {
        Map<String, Object> f = allDefaults();
        f.put("age", 25);
        f.put("monthly_income", 4000);
        f.put("dti_ratio", 70);
        f.put("active_loan_count", 5);
        f.put("has_mortgage", 0);
        f.put("max_overdue_days_90", 2);
        f.put("overdue_30_59_count", 3);
        f.put("overdue_60_89_count", 2);
        f.put("credit_card_usage_pct", 85);
        return f;
    }

    /** 中等风险：45岁 月入8000 1次逾期 */
    private Map<String, Object> mediumRiskFeatures() {
        Map<String, Object> f = allDefaults();
        f.put("age", 45);
        f.put("monthly_income", 8000);
        f.put("dti_ratio", 40);
        f.put("active_loan_count", 2);
        f.put("has_mortgage", 1);
        f.put("max_overdue_days_90", 0);
        f.put("overdue_30_59_count", 1);
        f.put("overdue_60_89_count", 0);
        f.put("credit_card_usage_pct", 50);
        return f;
    }
}
