package com.example.personal_loan.service;

/**
 * ML 训练数据采集服务
 */
public interface MlTrainingLogService {

    /**
     * 提取 XGBoost 模型需要的 9 个特征（轻量，不写 DB）
     * 用于在 addApplication 中做质量门槛 + 打分
     */
    java.util.Map<String, Object> extractModelFeatures(Long userId, Long applicationId);

    /**
     * 采集全量 58 特征并写入训练日志
     */
    void collectFeatures(Long userId, Long applicationId);

    /**
     * 补填标签（正常结清）
     * 应在订单状态变为"已完成"时调用
     *
     * @param applicationId 申请ID
     */
    void markCompleted(Long applicationId);

    /**
     * 补填标签（违约）
     * 应在确认逾期≥90天时调用
     *
     * @param applicationId 申请ID
     */
    void markDefaulted(Long applicationId);
}
