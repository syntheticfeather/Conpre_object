package com.example.personal_loan.service;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.personal_loan.config.RabbitMQConfig;
import com.example.personal_loan.dto.risk.RiskJobCreatedEvent;
import com.example.personal_loan.entity.RiskJob;
import com.example.personal_loan.mapper.RiskJobMapper;
import com.example.personal_loan.utils.RabbitUtil;

/**
 * RiskJobTimeoutScanner 单元测试（L7.2 超时补偿）
 */
class RiskJobTimeoutScannerTest {

    private RiskJobMapper riskJobMapper;
    private RabbitUtil rabbitUtil;
    private RiskJobTimeoutScanner scanner;

    @BeforeEach
    void setUp() {
        riskJobMapper = mock(RiskJobMapper.class);
        rabbitUtil = mock(RabbitUtil.class);
        scanner = new RiskJobTimeoutScanner();
        ReflectionTestUtils.setField(scanner, "riskJobMapper", riskJobMapper);
        ReflectionTestUtils.setField(scanner, "rabbitUtil", rabbitUtil);
        ReflectionTestUtils.setField(scanner, "timeoutMinutes", 30);
        ReflectionTestUtils.setField(scanner, "maxAttempts", 3);
    }

    private RiskJob queuedJob(long jobId, int attempt) {
        RiskJob job = new RiskJob();
        job.setJobId(jobId);
        job.setApplicationId(1001L);
        job.setUserId(7L);
        job.setJobVersion(1);
        job.setStatus("QUEUED");
        job.setAttempt(attempt);
        job.setCreatedAt(LocalDateTime.now().minusMinutes(31));
        return job;
    }

    @Test
    @DisplayName("无超时任务时不做任何操作")
    void noTimeoutJobs_noAction() {
        when(riskJobMapper.selectTimeoutQueued(30)).thenReturn(Collections.emptyList());

        scanner.scan();

        verify(rabbitUtil, never()).sendToRisk(any(), any());
        verify(riskJobMapper, never()).updateTimeoutRetry(anyLong(), any());
        verify(riskJobMapper, never()).updateFailedByTimeout(anyLong(), any(), any());
    }

    @Test
    @DisplayName("超时任务未达最大重试 → 重投 created 事件并更新 attempt")
    void timeoutJob_underMaxAttempts_republishes() {
        when(riskJobMapper.selectTimeoutQueued(30)).thenReturn(List.of(queuedJob(55L, 1)));
        when(riskJobMapper.updateTimeoutRetry(55L, "TIMEOUT_RETRY")).thenReturn(1);

        scanner.scan();

        verify(rabbitUtil).sendToRisk(eq(RabbitMQConfig.RISK_JOB_CREATED_ROUTING_KEY), any(RiskJobCreatedEvent.class));
        verify(riskJobMapper).updateTimeoutRetry(55L, "TIMEOUT_RETRY");
        verify(riskJobMapper, never()).updateFailedByTimeout(anyLong(), any(), any());
    }

    @Test
    @DisplayName("超时任务达到最大重试 → 标记 FAILED")
    void timeoutJob_reachedMaxAttempts_markFailed() {
        when(riskJobMapper.selectTimeoutQueued(30)).thenReturn(List.of(queuedJob(55L, 3)));
        when(riskJobMapper.updateFailedByTimeout(anyLong(), eq("TIMEOUT_EXCEEDED"), any(LocalDateTime.class)))
                .thenReturn(1);

        scanner.scan();

        verify(rabbitUtil, never()).sendToRisk(any(), any());
        verify(riskJobMapper, never()).updateTimeoutRetry(anyLong(), any());
        verify(riskJobMapper).updateFailedByTimeout(eq(55L), eq("TIMEOUT_EXCEEDED"), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("扫描异常被捕获，不抛出")
    void scanException_caught() {
        when(riskJobMapper.selectTimeoutQueued(anyInt())).thenThrow(new RuntimeException("db down"));

        assertThatNoException().isThrownBy(() -> scanner.scan());
    }
}
