SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
-- ============================================================
-- V1.5: 风控任务结果表（L7.1 同步闭环 / L7.2 异步链路共用）
-- 记录每次风控打分的完整快照：LLM 特征、full 模型分数、降级状态
-- 字段类型与 users / loan_applications / ml_training_log 保持一致
-- ============================================================

CREATE TABLE risk_job (
    job_id                 BIGINT      PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    application_id         INT         NOT NULL COMMENT '关联的贷款申请ID',
    user_id                INT         NOT NULL COMMENT '关联的用户ID',
    job_version            INT         NOT NULL DEFAULT 1 COMMENT '任务版本，用于幂等/重跑',
    status                 VARCHAR(20) NOT NULL COMMENT '任务状态: COMPLETED / DEGRADED / FAILED / QUEUED / PROCESSING',
    stage                  VARCHAR(20) COMMENT '当前阶段（L7.1同步版可空）',
    attempt                INT         NOT NULL DEFAULT 0 COMMENT '当前重试次数',
    error_code             VARCHAR(50) COMMENT '失败原因错误码',
    score                  DECIMAL(5,4) COMMENT '最终 XGBoost 分数（违约概率）',
    feature_set            VARCHAR(20) DEFAULT 'full' COMMENT '特征集: full / baseline / llm',
    llm_feature_available  TINYINT     DEFAULT 0 COMMENT 'LLM 特征是否可用: 0=降级/不可用, 1=正常',
    features_json          JSON        COMMENT '完整特征向量 JSON',
    model_version          VARCHAR(20) COMMENT 'XGBoost 模型版本',
    prompt_version         VARCHAR(20) COMMENT 'LLM prompt 版本',
    converter_version      VARCHAR(20) COMMENT '文档转换器版本',
    document_count         INT         DEFAULT 0 COMMENT '提交的材料总数',
    converted_count        INT         DEFAULT 0 COMMENT '成功转换/处理的材料数',
    model                  VARCHAR(50) COMMENT 'LLM 模型名称',
    cached                 TINYINT     DEFAULT 0 COMMENT '是否命中特征缓存',
    reasoning              TEXT        COMMENT 'LLM 推理说明（可能为空）',

    created_at             DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    started_at             DATETIME COMMENT '开始处理时间',
    finished_at            DATETIME COMMENT '处理完成时间',
    updated_at             DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (application_id) REFERENCES loan_applications(id),
    UNIQUE KEY uk_app_version (application_id, job_version),
    INDEX idx_status_updated (status, updated_at),
    INDEX idx_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='风控任务结果表（L7.1/L7.2 共用）';
