package com.example.personal_loan.service.impl;

import java.io.InputStream;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.personal_loan.service.RiskScoringService;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import ml.dmlc.xgboost4j.java.Booster;
import ml.dmlc.xgboost4j.java.DMatrix;
import ml.dmlc.xgboost4j.java.XGBoost;

/**
 * XGBoost 风控打分服务实现
 *
 * 启动时加载 resources/ml/risk_model.json，
 * 每次调用 predict() 时从特征 map 中提取模型需要的 9 个特征，返回违约概率。
 */
@Slf4j
@Service
public class RiskScoringServiceImpl implements RiskScoringService {

    private Booster model;
    private boolean ready = false;
    private String modelVersion = "gms_v1";

    /** 模型需要的特征（顺序固定，与训练时一致） */
    private static final String[] MODEL_FEATURES = {
        "age", "monthly_income", "dti_ratio", "active_loan_count",
        "has_mortgage", "max_overdue_days_90", "overdue_30_59_count",
        "overdue_60_89_count", "credit_card_usage_pct"
    };

    /** 用于填充缺失特征值的均值 */
    private static final double[] FEATURE_MEANS = {
        52.0,    // age
        6670.0,  // monthly_income
        15.0,    // dti_ratio
        8.0,     // active_loan_count
        1.0,     // has_mortgage
        0.27,    // max_overdue_days_90
        0.42,    // overdue_30_59_count
        0.24,    // overdue_60_89_count
        6.0,     // credit_card_usage_pct
    };

    @PostConstruct
    public void init() {
        try {
            InputStream is = getClass().getClassLoader()
                .getResourceAsStream("ml/risk_model.json");
            if (is == null) {
                log.warn("模型文件 ml/risk_model.json 未找到，风控打分不可用");
                return;
            }
            model = XGBoost.loadModel(is);
            ready = true;
            log.info("XGBoost 模型加载成功 version={}", modelVersion);
        } catch (Exception e) {
            log.error("XGBoost 模型加载失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public double predict(Map<String, Object> features) {
        if (!ready || model == null) {
            return -1;
        }

        try {
            // 从特征 map 中提取 9 个模型特征，构建特征向量
            float[] featureVec = new float[MODEL_FEATURES.length];
            for (int i = 0; i < MODEL_FEATURES.length; i++) {
                Object val = features.get(MODEL_FEATURES[i]);
                if (val instanceof Number) {
                    featureVec[i] = ((Number) val).floatValue();
                } else if (val instanceof String) {
                    featureVec[i] = safeParseFloat((String) val);
                } else {
                    featureVec[i] = (float) FEATURE_MEANS[i]; // 缺失用均值
                }
            }

            // XGBoost 预测
            DMatrix dmat = new DMatrix(featureVec, 1, featureVec.length, Float.NaN);
            float[][] result = model.predict(dmat);
            dmat.dispose();

            // 二分类: result[0][0] 是违约概率
            return result[0][0];

        } catch (Exception e) {
            log.error("XGBoost 预测失败: {}", e.getMessage(), e);
            return -1;
        }
    }

    @Override
    public boolean isReady() {
        return ready;
    }

    @Override
    public int toCreditScore(double defaultProbability) {
        if (defaultProbability < 0) return 0;
        return (int) Math.round((1.0 - defaultProbability) * 750);
    }

    @Override
    public int countRealFeatures(Map<String, Object> features) {
        int real = 0;

        // age 真实: 用户填了身份证且不是默认值 30
        Object age = features.get("age");
        if (age instanceof Number && ((Number) age).intValue() != 30) real++;

        // has_mortgage 真实: immovables_cert 存在（由调用方保证 features 中值为 1 而非默认 0）
        Object mortgage = features.get("has_mortgage");
        if (mortgage instanceof Number && ((Number) mortgage).intValue() == 1) real++;

        // active_loan_count 真实: 有 orders 记录
        Object loans = features.get("active_loan_count");
        if (loans instanceof Number && ((Number) loans).intValue() > 0) real++;

        // dti_ratio 真实: 有 orders 记录
        Object dti = features.get("dti_ratio");
        if (dti instanceof Number && ((Number) dti).doubleValue() > 0) real++;

        // overdue 特征真实: 有逾期记录
        Object overdue90 = features.get("max_overdue_days_90");
        if (overdue90 instanceof Number && ((Number) overdue90).intValue() > 0) real++;

        Object overdue30 = features.get("overdue_30_59_count");
        if (overdue30 instanceof Number && ((Number) overdue30).intValue() > 0) real++;

        Object overdue60 = features.get("overdue_60_89_count");
        if (overdue60 instanceof Number && ((Number) overdue60).intValue() > 0) real++;

        // monthly_income & credit_card_usage_pct: DB 无，永远不真实

        return real;
    }

    @Override
    public String getModelVersion() {
        return modelVersion;
    }

    private float safeParseFloat(String s) {
        try { return Float.parseFloat(s); }
        catch (NumberFormatException e) { return 0.0f; }
    }
}
