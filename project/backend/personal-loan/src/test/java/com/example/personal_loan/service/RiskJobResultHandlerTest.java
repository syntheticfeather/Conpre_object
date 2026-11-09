package com.example.personal_loan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.example.personal_loan.dto.risk.RiskJobCompletedEvent;
import com.example.personal_loan.entity.RiskJob;
import com.example.personal_loan.mapper.RiskJobMapper;
import com.example.personal_loan.mapper.UserCertMapper;
import com.example.personal_loan.mq.NotificationOutboxPublisher;
import com.example.personal_loan.service.impl.FullRiskScoringService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * RiskJobResultHandler 单元测试（L7.2）
 * <p>
 * 覆盖结果落库、状态判定、分数计算、信用分回写、通知入 Outbox、幂等与异常。
 */
class RiskJobResultHandlerTest {

    private RiskJobMapper riskJobMapper;
    private UserCertMapper userCertMapper;
    private NotificationOutboxPublisher notificationOutboxPublisher;
    private MlTrainingLogService mlTrainingLogService;
    private FullRiskScoringService fullRiskScoringService;
    private RiskJobResultHandler handler;

    @BeforeEach
    void setUp() {
        riskJobMapper = mock(RiskJobMapper.class);
        userCertMapper = mock(UserCertMapper.class);
        notificationOutboxPublisher = mock(NotificationOutboxPublisher.class);
        mlTrainingLogService = mock(MlTrainingLogService.class);
        fullRiskScoringService = mock(FullRiskScoringService.class);

        when(mlTrainingLogService.extractModelFeatures(anyLong(), anyLong()))
                .thenReturn(Map.of("age", 30, "monthly_income", 8000));
        when(fullRiskScoringService.getModelVersion()).thenReturn("full_v1");
        when(fullRiskScoringService.getActiveModelType()).thenReturn("full");
        // 默认 full 模型给出违约概率 0.15
        when(fullRiskScoringService.predict(any(), any(), any())).thenReturn(0.15);

        handler = new RiskJobResultHandler(riskJobMapper, userCertMapper,
                notificationOutboxPublisher, new ObjectMapper(),
                mlTrainingLogService, fullRiskScoringService);
    }

    private RiskJob queuedJob() {
        RiskJob job = new RiskJob();
        job.setJobId(55L);
        job.setApplicationId(1001L);
        job.setUserId(7L);
        job.setJobVersion(1);
        job.setStatus("QUEUED");
        job.setStage("QUEUED");
        job.setFeatureSet("full");
        job.setLlmFeatureAvailable(0);
        job.setModelVersion("full_v0");
        return job;
    }

    private RiskJobCompletedEvent completedEvent(String status, boolean llmAvailable) {
        RiskJobCompletedEvent event = new RiskJobCompletedEvent();
        event.setEvent("RISK_JOB_COMPLETED");
        event.setJobId(55L);
        event.setApplicationId(1001L);
        event.setUserId(7L);
        event.setJobVersion(1);
        event.setStatus(status);
        event.setLlmFeatureAvailable(llmAvailable ? 1 : 0);
        event.setPromptVersion("prompt_v1");
        event.setConverterVersion("converter_v1");
        event.setModel("deepseek-v4-flash");
        event.setCached(true);
        event.setReasoning("ok");

        Map<String, Object> features = new HashMap<>();
        features.put("llm_verdict", 1.0);
        features.put("llm_confidence", 0.86);
        features.put("llm_feature_available", llmAvailable ? 1.0 : 0.0);
        event.setFeatures(features);

        Map<String, Object> deterministic = new HashMap<>();
        deterministic.put("doc_count", 5);
        deterministic.put("doc_total_chars", 2400);
        event.setDeterministicFeatures(deterministic);
        return event;
    }

    @Test
    @DisplayName("ok + LLM 可用 → COMPLETED，Java 端计算分数并回写信用分与通知")
    void okAndLlmAvailable_completed() {
        when(riskJobMapper.selectById(55L)).thenReturn(queuedJob());
        when(userCertMapper.updateCreditScore(anyLong(), anyInt())).thenReturn(1);

        handler.apply(completedEvent("ok", true));

        ArgumentCaptor<RiskJob> captor = ArgumentCaptor.forClass(RiskJob.class);
        verify(riskJobMapper).updateCompleted(captor.capture());
        RiskJob saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo("COMPLETED");
        assertThat(saved.getStage()).isEqualTo("COMPLETED");
        // 0.15 -> (1 - 0.15) * 750 = 637.5 -> 638
        assertThat(saved.getScore()).isEqualByComparingTo(BigDecimal.valueOf(0.15));
        assertThat(saved.getLlmFeatureAvailable()).isEqualTo(1);
        assertThat(saved.getModelVersion()).isEqualTo("full_v1");
        assertThat(saved.getPromptVersion()).isEqualTo("prompt_v1");
        assertThat(saved.getConverterVersion()).isEqualTo("converter_v1");
        assertThat(saved.getCached()).isEqualTo(1);
        assertThat(saved.getFinishedAt()).isNotNull();

        verify(userCertMapper).updateCreditScore(eq(7L), eq(638));
        verify(notificationOutboxPublisher)
                .enqueueNotification(7L, 1001L, "LOAN_APPLICATION_STATUS");
    }

    @Test
    @DisplayName("features 与 deterministic_features 合并结构化特征后落库")
    void featuresMerged() {
        when(riskJobMapper.selectById(55L)).thenReturn(queuedJob());
        when(userCertMapper.updateCreditScore(anyLong(), anyInt())).thenReturn(1);

        handler.apply(completedEvent("ok", true));

        ArgumentCaptor<RiskJob> captor = ArgumentCaptor.forClass(RiskJob.class);
        verify(riskJobMapper).updateCompleted(captor.capture());
        String json = captor.getValue().getFeaturesJson();
        assertThat(json)
                .contains("llm_verdict")
                .contains("llm_confidence")
                .contains("doc_count")
                .contains("doc_total_chars")
                .contains("age")
                .contains("monthly_income");
    }

    @Test
    @DisplayName("status=error → DEGRADED，仍用 full 模型计算降级分")
    void errorStatus_degraded() {
        when(riskJobMapper.selectById(55L)).thenReturn(queuedJob());

        handler.apply(completedEvent("error", false));

        ArgumentCaptor<RiskJob> captor = ArgumentCaptor.forClass(RiskJob.class);
        verify(riskJobMapper).updateCompleted(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("DEGRADED");
        assertThat(captor.getValue().getScore()).isEqualByComparingTo(BigDecimal.valueOf(0.15));
    }

    @Test
    @DisplayName("ok 但 LLM 特征不可用 → DEGRADED")
    void okWithoutLlmFeature_degraded() {
        when(riskJobMapper.selectById(55L)).thenReturn(queuedJob());

        handler.apply(completedEvent("ok", false));

        ArgumentCaptor<RiskJob> captor = ArgumentCaptor.forClass(RiskJob.class);
        verify(riskJobMapper).updateCompleted(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("DEGRADED");
        assertThat(captor.getValue().getLlmFeatureAvailable()).isZero();
    }

    @Test
    @DisplayName("status=failed → FAILED，且不回写信用分")
    void failedStatus_failed() {
        when(riskJobMapper.selectById(55L)).thenReturn(queuedJob());

        handler.apply(completedEvent("failed", false));

        ArgumentCaptor<RiskJob> captor = ArgumentCaptor.forClass(RiskJob.class);
        verify(riskJobMapper).updateCompleted(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("FAILED");
        assertThat(captor.getValue().getScore()).isNull();
        assertThat(captor.getValue().getStage()).isEqualTo("FAILED");
        verify(userCertMapper, never()).updateCreditScore(anyLong(), anyInt());
    }

    @Test
    @DisplayName("任务已处于终态 → 重复消息直接忽略")
    void finalStatus_ignored() {
        RiskJob done = queuedJob();
        done.setStatus("COMPLETED");
        when(riskJobMapper.selectById(55L)).thenReturn(done);

        handler.apply(completedEvent("ok", true));

        verify(riskJobMapper, never()).updateCompleted(any());
        verify(userCertMapper, never()).updateCreditScore(anyLong(), anyInt());
        verify(notificationOutboxPublisher, never()).enqueueNotification(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("risk_job 不存在 → 抛异常交给消费者重试")
    void jobNotFound_throws() {
        when(riskJobMapper.selectById(55L)).thenReturn(null);

        assertThatThrownBy(() -> handler.apply(completedEvent("ok", true)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("risk_job 不存在");
    }

    @Test
    @DisplayName("缺少 job_id → 抛参数异常")
    void missingJobId_throws() {
        RiskJobCompletedEvent event = new RiskJobCompletedEvent();

        assertThatThrownBy(() -> handler.apply(event))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("job_id");
    }
}
