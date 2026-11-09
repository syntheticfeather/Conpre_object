package com.example.personal_loan.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.personal_loan.config.RabbitMQConfig;
import com.example.personal_loan.dto.risk.RiskJobCreatedEvent;
import com.example.personal_loan.entity.RiskJob;
import com.example.personal_loan.mapper.RiskJobMapper;
import com.example.personal_loan.utils.RabbitUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * L7.2 风控任务超时扫描器。
 * <p>
 * 定时扫描长时间处于 {@code QUEUED} 的 risk_job：
 * <ul>
 *   <li>未超过最大重试次数 → 重新发布 {@code RISK_JOB_CREATED} 事件，attempt + 1</li>
 *   <li>超过最大重试次数 → 标记为 {@code FAILED}，避免任务永久悬挂</li>
 * </ul>
 */
@Slf4j
@Component
public class RiskJobTimeoutScanner {

    @Autowired
    private RiskJobMapper riskJobMapper;

    @Autowired
    private RabbitUtil rabbitUtil;

    /** 任务超时时间（分钟），默认 30 分钟 */
    @Value("${risk.job.timeout-minutes:30}")
    private int timeoutMinutes;

    /** 最大重试次数，默认 3 次 */
    @Value("${risk.job.max-attempts:3}")
    private int maxAttempts;

    /**
     * 默认每 5 分钟扫描一次。
     */
    @Scheduled(fixedDelayString = "${risk.job.timeout-scan-interval-ms:300000}")
    public void scan() {
        try {
            List<RiskJob> timeoutJobs = riskJobMapper.selectTimeoutQueued(timeoutMinutes);
            if (timeoutJobs.isEmpty()) {
                return;
            }
            log.info("[L7.2] 扫描到 {} 条超时 QUEUED 任务", timeoutJobs.size());
            for (RiskJob job : timeoutJobs) {
                handle(job);
            }
        } catch (Exception e) {
            log.error("[L7.2] 超时扫描异常", e);
        }
    }

    private void handle(RiskJob job) {
        Long jobId = job.getJobId();
        int attempt = job.getAttempt() == null ? 0 : job.getAttempt();
        if (attempt >= maxAttempts) {
            markFailed(job);
            return;
        }
        republish(job);
    }

    private void republish(RiskJob job) {
        try {
            RiskJobCreatedEvent event = RiskJobCreatedEvent.of(
                    job.getJobId(), job.getApplicationId(), job.getUserId(),
                    job.getJobVersion(), job.getAttempt() + 1);
            rabbitUtil.sendToRisk(RabbitMQConfig.RISK_JOB_CREATED_ROUTING_KEY, event);

            int rows = riskJobMapper.updateTimeoutRetry(job.getJobId(), "TIMEOUT_RETRY");
            if (rows > 0) {
                log.info("[L7.2] 超时任务已重投 jobId={} appId={} attempt={}",
                        job.getJobId(), job.getApplicationId(), job.getAttempt() + 1);
            }
        } catch (Exception e) {
            log.error("[L7.2] 超时任务重投失败 jobId={}: {}", job.getJobId(), e.getMessage(), e);
        }
    }

    private void markFailed(RiskJob job) {
        try {
            int rows = riskJobMapper.updateFailedByTimeout(job.getJobId(), "TIMEOUT_EXCEEDED",
                    LocalDateTime.now());
            if (rows > 0) {
                log.warn("[L7.2] 超时任务已标记 FAILED jobId={} appId={}",
                        job.getJobId(), job.getApplicationId());
            }
        } catch (Exception e) {
            log.error("[L7.2] 超时任务标记 FAILED 失败 jobId={}: {}",
                    job.getJobId(), e.getMessage(), e);
        }
    }
}
