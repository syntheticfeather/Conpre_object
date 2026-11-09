package com.example.personal_loan.dto.risk;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 风控任务创建事件（L7.2）。
 * <p>
 * 由 Java 写入 Outbox 后投递到 {@code risk.exchange} / {@code risk.job.created}，
 * 由 Python Mock Worker 消费。消息只放 ID 与版本，不放材料原文 / PII。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskJobCreatedEvent {

    /** 事件类型，固定 RISK_JOB_CREATED */
    @JsonProperty("event")
    private String event;

    @JsonProperty("job_id")
    private Long jobId;

    @JsonProperty("application_id")
    private Long applicationId;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("job_version")
    private Integer jobVersion;

    @JsonProperty("attempt")
    private Integer attempt;

    public static RiskJobCreatedEvent of(Long jobId, Long applicationId, Long userId,
                                        Integer jobVersion, Integer attempt) {
        return new RiskJobCreatedEvent("RISK_JOB_CREATED", jobId, applicationId, userId,
                jobVersion, attempt);
    }
}
