package com.example.personal_loan.entity;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * ML 训练数据采集实体
 * 每次贷款申请时快照全量特征，还款完成后补标签
 */
@Data
public class MlTrainingLog {
    private Long id;
    private Integer userId;
    private Integer applicationId;

    /** JSON 字符串: 全部特征键值对 */
    private String features;

    /** 模型版本号 */
    private String modelVersion;

    /** 模型给出的违约概率 (0~1) */
    private java.math.BigDecimal modelScore;

    /** 实际结果: 0=正常结清, 1=违约 */
    private Integer actualResult;

    /** 标签确认时间 */
    private LocalDateTime resultAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
