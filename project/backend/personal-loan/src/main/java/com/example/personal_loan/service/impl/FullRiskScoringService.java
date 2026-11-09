package com.example.personal_loan.service.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.personal_loan.service.MlTrainingLogService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.personal_loan.service.RiskScoringService;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;

/**
 * Full 特征集 XGBoost 打分服务（L7.1）
 * <p>
 * 输入 = Java 结构化特征（MlTrainingLogServiceImpl 提供） + Python LLM G 组特征 + 确定性 H 组特征
 * 输出 = full 模型的违约概率。
 * <p>
 * full 模型文件路径：{@code resources/ml/risk_model_full.json}
 * 如果该文件不存在，会自动回退到 Java 现有 {@link RiskScoringService} 的 9 维 baseline 模型，
 * 保证业务不会因为模型缺失而中断。
 * <p>
 * 架构说明：
 * <ul>
 *   <li>fullFeatureNames 是 full 模型的特征列表（字段 + 顺序），必须和训练时完全一致</li>
 *   <li>LLM 特征和确定性特征的名字从 Python 端过来，直接用</li>
 *   <li>结构化特征名沿用 {@link MlTrainingLogService#MODEL_FEATURE_NAMES} 里的那 9 个</li>
 * </ul>
 */
@Slf4j
@Service
public class FullRiskScoringService {

    /**
     * full 模型特征列表（顺序 = Python 训练时的特征顺序）。
     * <p>
     * 默认是占位列表；启动时若存在 {@code classpath:ml/feature_names.json}，
     * 会用它覆盖本列表，保证训练/推理特征顺序一致。
     */
    private static String[] FULL_FEATURE_NAMES = {
            // —— 结构化特征 9 维（与 RiskScoringService.MODEL_FEATURES 相同）——
            "age", "monthly_income", "dti_ratio", "active_loan_count",
            "has_mortgage", "max_overdue_days_90", "overdue_30_59_count",
            "overdue_60_89_count", "credit_card_usage_pct",
            // —— LLM G 组特征（与 Python 侧命名保持一致）——
            "llm_verdict", "llm_confidence", "doc_income_consistency",
            "doc_income_stability", "doc_employment_risk", "doc_multi_lender_signal",
            "doc_material_mismatch", "doc_suspicious_document", "doc_debt_pressure",
            "doc_negative_public_record", "doc_missing_key_info", "llm_feature_available",
            // —— 确定性 H 组特征（与 Python 侧命名保持一致）——
            "doc_total_chars", "doc_count", "doc_type_coverage", "note_length", "has_ocr_text"
    };

    /** 缺失时的默认值 */
    private static final float DEFAULT_MISSING = 0.0f;

    private Booster fullModel;
    private boolean fullModelReady = false;
    /** "full" 或 "baseline" */
    private String activeModelType = "baseline";
    private String modelVersion = "full_v1";

    /** 回退用的 baseline 服务 */
    private final RiskScoringService baselineService;

    public FullRiskScoringService(RiskScoringService baselineService) {
        this.baselineService = baselineService;
    }

    @PostConstruct
    public void init() {
        // 优先尝试 full 模型
        InputStream fullIs = getClass().getClassLoader()
                .getResourceAsStream("ml/risk_model_full.json");
        if (fullIs != null) {
            try {
                fullModel = XGBoost.loadModel(fullIs);
                fullModelReady = true;
                activeModelType = "full";
                // 尝试加载 feature_names.json 覆盖默认顺序
                loadFeatureNames();
                log.info("Full XGBoost 模型加载成功 version={} featureCount={}",
                        modelVersion, FULL_FEATURE_NAMES.length);
                return;
            } catch (Exception e) {
                log.error("Full 模型加载失败，回退 baseline: {}", e.getMessage());
            }
        } else {
            log.warn("Full 模型文件 ml/risk_model_full.json 未找到，L7.1 将使用 baseline 9 维模型");
        }

        fullModelReady = false;
        activeModelType = "baseline";
        log.info("FullRiskScoringService 使用 baseline 模型");
    }

    /**
     * 尝试从 resources/ml/feature_names.json 加载 full 模型的特征顺序。
     * 这保证了 Java 和 Python 对 full 模型的特征顺序理解一致。
     */
    private void loadFeatureNames() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("ml/feature_names.json")) {
            if (is == null) {
                log.warn("未找到 ml/feature_names.json，FullRiskScoringService 使用占位特征列表");
                return;
            }
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(is);
            if (!node.isArray()) {
                log.warn("ml/feature_names.json 不是数组，使用占位特征列表");
                return;
            }
            List<String> names = new ArrayList<>(node.size());
            for (JsonNode n : node) {
                names.add(n.asText());
            }
            FULL_FEATURE_NAMES = names.toArray(new String[0]);
            log.info("从 ml/feature_names.json 加载特征顺序，共 {} 维", FULL_FEATURE_NAMES.length);
        } catch (Exception e) {
            log.error("加载 ml/feature_names.json 失败，使用占位特征列表: {}", e.getMessage());
        }
    }

    /**
     * 合并三路特征 → full 模型 → 打分
     *
     * @param structured 结构化特征（MlTrainingLogService.extractModelFeatures 提供的 9 维）
     * @param llmFeatures Python 返回的 LLM 特征（G 组 + 含 llm_feature_available）
     * @param deterministicFeatures Python 返回的确定性特征（H 组）
     * @return 违约概率（0~1）；如果 full 模型不可用则走 baseline
     */
    public double predict(Map<String, Object> structured,
                          Map<String, Double> llmFeatures,
                          Map<String, Double> deterministicFeatures) {

        if (!fullModelReady || fullModel == null) {
            // 回退 baseline
            log.info("[FullRiskScoringService] full 模型不可用，回退 baseline 打分");
            return baselineService.predict(structured);
        }

        Map<String, Object> merged = new HashMap<>();
        if (structured != null) merged.putAll(structured);
        if (llmFeatures != null) {
            for (Map.Entry<String, Double> e : llmFeatures.entrySet()) {
                merged.put(e.getKey(), e.getValue());
            }
        }
        if (deterministicFeatures != null) {
            for (Map.Entry<String, Double> e : deterministicFeatures.entrySet()) {
                merged.put(e.getKey(), e.getValue());
            }
        }

        // 按 FULL_FEATURE_NAMES 顺序抽取
        float[] featureVec = new float[FULL_FEATURE_NAMES.length];
        for (int i = 0; i < FULL_FEATURE_NAMES.length; i++) {
            Object val = merged.get(FULL_FEATURE_NAMES[i]);
            featureVec[i] = toFloat(val);
        }

        try {
            DMatrix dmat = new DMatrix(featureVec, 1, featureVec.length, Float.NaN);
            float[][] result = fullModel.predict(dmat);
            dmat.dispose();
            return result[0][0];
        } catch (Exception e) {
            log.error("[FullRiskScoringService] full 模型预测失败，回退 baseline: {}", e.getMessage());
            return baselineService.predict(structured);
        }
    }

    /** 当前使用的模型类型："full" 或 "baseline" */
    public String getActiveModelType() {
        return activeModelType;
    }

    /** full 模型是否就绪 */
    public boolean isFullModelReady() {
        return fullModelReady;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    /** 返回 full 模型的特征名列表（调试用） */
    public List<String> getFullFeatureNames() {
        List<String> list = new ArrayList<>(FULL_FEATURE_NAMES.length);
        for (String n : FULL_FEATURE_NAMES) list.add(n);
        return list;
    }

    private float toFloat(Object val) {
        if (val instanceof Number) {
            return ((Number) val).floatValue();
        }
        if (val instanceof String) {
            try {
                return Float.parseFloat((String) val);
            } catch (NumberFormatException ignore) {
                return DEFAULT_MISSING;
            }
        }
        return DEFAULT_MISSING;
    }
}
