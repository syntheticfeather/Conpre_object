SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
-- ============================================================
-- V1.3: ML 训练数据采集表
-- 每次贷款申请时快照全量特征，还款完成后补标签
-- ============================================================

CREATE TABLE ml_training_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
    user_id INT NOT NULL COMMENT '用户ID',
    application_id INT NOT NULL COMMENT '关联申请ID',

    -- 特征快照（JSON, ~50+ 特征键值对）
    features JSON NOT NULL COMMENT '全部特征值',

    -- 模型信息（未来 XGBoost 集成后填充）
    model_version VARCHAR(20) COMMENT '模型版本号',
    model_score DECIMAL(5,4) COMMENT '模型给出的违约概率',

    -- 标签（事后补填）
    actual_result TINYINT NULL COMMENT '实际结果: 0=正常结清, 1=违约',
    result_at DATETIME NULL COMMENT '标签确认时间',

    -- 时间戳
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '特征采集时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (application_id) REFERENCES loan_applications(id),
    INDEX idx_user_created (user_id, created_at),
    INDEX idx_result (actual_result, created_at),
    INDEX idx_app (application_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ML训练数据采集表';
