package com.example.personal_loan.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

/**
 * 风控任务结果实体（L7.1/L7.2 共用）
 * <p>
 * 记录每次风控打分的完整快照，包括 LLM 特征、full 模型分数、降级状态等。
 * L7.1 同步版每次申请提交后立即写入一条 COMPLETED/DEGRADED 记录。
 */
@Data
public class RiskJob {
    /** 任务ID（自增主键） */
    private Long jobId;

    /** 关联的贷款申请ID */
    private Long applicationId;

    /** 关联的用户ID */
    private Long userId;

    /** 任务版本，用于幂等/重跑 */
    private Integer jobVersion;

    /**
     * 任务状态
     * <ul>
     *   <li>COMPLETED：正常完成（LLM 特征可用）</li>
     *   <li>DEGRADED：降级完成（LLM 特征不可用，用默认值）</li>
     *   <li>FAILED：失败（不可恢复错误）</li>
     *   <li>QUEUED / PROCESSING：L7.2 异步状态</li>
     * </ul>
     */
    private String status;

    /** 当前阶段（L7.1 同步版可空） */
    private String stage;

    /** 当前重试次数 */
    private Integer attempt;

    /** 失败原因错误码（如 TIMEOUT、INVALID_PATH 等） */
    private String errorCode;

    /** 最终 XGBoost 分数（违约概率 0~1） */
    private BigDecimal score;

    /** 特征集: full / baseline / llm */
    private String featureSet;

    /** LLM 特征是否可用: 0=降级/不可用, 1=正常 */
    private Integer llmFeatureAvailable;

    /** 完整特征向量 JSON */
    private String featuresJson;

    /** XGBoost 模型版本 */
    private String modelVersion;

    /** LLM prompt 版本 */
    private String promptVersion;

    /** 文档转换器版本 */
    private String converterVersion;

    /** 提交的材料总数 */
    private Integer documentCount;

    /** 成功转换/处理的材料数 */
    private Integer convertedCount;

    /** LLM 模型名称 */
    private String model;

    /** 是否命中特征缓存 */
    private Integer cached;

    /** LLM 推理说明（可能为空） */
    private String reasoning;

    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime updatedAt;
}
