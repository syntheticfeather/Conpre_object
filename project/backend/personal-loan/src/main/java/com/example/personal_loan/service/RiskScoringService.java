package com.example.personal_loan.service;

import java.util.Map;

/**
 * XGBoost 风控打分服务
 */
public interface RiskScoringService {

    /** 计算违约概率 (0~1) */
    double predict(Map<String, Object> features);

    /** 将违约概率映射为 0-750 信用分 */
    int toCreditScore(double defaultProbability);

    /** 统计 9 个模型特征中有几个是真实值（非均值填充） */
    int countRealFeatures(Map<String, Object> features);

    /** 模型是否已加载 */
    boolean isReady();

    /** 模型版本号 */
    String getModelVersion();
}
