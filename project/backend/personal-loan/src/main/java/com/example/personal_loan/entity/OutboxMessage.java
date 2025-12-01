package com.example.personal_loan.entity;


import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OutboxMessage {

    private Long id;

    /**
     * 全局唯一消息ID，用于幂等和追踪
     * 示例: "msg_550e8400-e29b-41d4-a716-446655440000"
     * 或: "loan_app_123_1712345678901"
     */
    private String messageId;

    /**
     * 业务类型，用于分类和监控
     * 示例: "LOAN_APPLICATION"
     * 是否要写成枚举类？
     */
    private String businessType;

    /**
     * 关联的业务主键
     * 示例: loanApplication.getId()
     */
    private Long businessId;

    /**
     * RabbitMQ routing key
     * 示例: "loan.application.submitted"
     */
    private String topic;

    /**
     * 消息内容（JSON 字符串）
     * 注意：MyBatis 会自动处理 JSON <-> String 转换
     */
    private String payload;

    /**
     * 状态: PENDING / SENT / FAILED
     */
    private String status;

    private LocalDateTime createdAt;
    private LocalDateTime sentAt;
}