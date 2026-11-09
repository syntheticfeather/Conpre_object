package com.example.personal_loan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.example.personal_loan.dto.risk.RiskFeatureRequest;
import com.example.personal_loan.entity.OutboxMessage;
import com.example.personal_loan.entity.RiskJob;
import com.example.personal_loan.enums.BusinessType;
import com.example.personal_loan.factory.OutboxMessageFactory;
import com.example.personal_loan.mapper.OutboxMapper;
import com.example.personal_loan.mapper.RiskJobMapper;
import com.example.personal_loan.service.assembler.RiskFeatureAssembler;

/**
 * RiskJobAsyncService 单元测试（L7.2）
 */
class RiskJobAsyncServiceTest {

    private RiskJobMapper riskJobMapper;
    private OutboxMapper outboxMapper;
    private OutboxMessageFactory outboxMessageFactory;
    private RiskFeatureAssembler assembler;
    private RiskJobAsyncService service;

    @BeforeEach
    void setUp() {
        riskJobMapper = mock(RiskJobMapper.class);
        outboxMapper = mock(OutboxMapper.class);
        outboxMessageFactory = mock(OutboxMessageFactory.class);
        assembler = mock(RiskFeatureAssembler.class);

        service = new RiskJobAsyncService(riskJobMapper, outboxMapper,
                outboxMessageFactory, assembler);
    }

    private void enable() {
        ReflectionTestUtils.setField(service, "asyncEnabled", true);
    }

    private RiskFeatureRequest requestWithDocs(int count) {
        RiskFeatureRequest request = new RiskFeatureRequest();
        List<RiskFeatureRequest.DocumentItem> docs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            RiskFeatureRequest.DocumentItem doc = new RiskFeatureRequest.DocumentItem();
            doc.setType("doc_" + i);
            doc.setSourcePath("p/" + i);
            docs.add(doc);
        }
        request.setDocuments(docs);
        return request;
    }

    @Test
    @DisplayName("开关关闭时不写库、不发消息")
    void disabled_noSideEffects() {
        assertThat(service.enqueue(1L, 1001L)).isFalse();
        verify(riskJobMapper, never()).insert(any());
        verify(outboxMapper, never()).insert(any());
    }

    @Test
    @DisplayName("开关开启 → 写 QUEUED 任务并写 Outbox")
    void enabled_writesQueuedJobAndOutbox() {
        enable();
        when(riskJobMapper.selectByAppVersion(1001L, 1)).thenReturn(null);
        when(assembler.buildRequest(1L, 1001L)).thenReturn(requestWithDocs(3));
        when(riskJobMapper.insert(any(RiskJob.class))).thenAnswer(invocation -> {
            ((RiskJob) invocation.getArgument(0)).setJobId(55L);
            return 1;
        });
        OutboxMessage outbox = new OutboxMessage();
        outbox.setMessageId("risk_job_created_1001_1");
        when(outboxMessageFactory.create(eq(BusinessType.RISK_JOB_CREATED), any(), eq(1001L)))
                .thenReturn(outbox);

        boolean queued = service.enqueue(1L, 1001L);

        assertThat(queued).isTrue();
        org.mockito.ArgumentCaptor<RiskJob> captor =
                org.mockito.ArgumentCaptor.forClass(RiskJob.class);
        verify(riskJobMapper).insert(captor.capture());
        RiskJob job = captor.getValue();
        assertThat(job.getStatus()).isEqualTo("QUEUED");
        assertThat(job.getJobVersion()).isEqualTo(1);
        assertThat(job.getDocumentCount()).isEqualTo(3);
        // 入队时不预设 modelVersion，由结果消费端写入
        assertThat(job.getModelVersion()).isNull();
        verify(outboxMapper).insert(outbox);
    }

    @Test
    @DisplayName("同一申请已存在 job_version=1 → 幂等跳过")
    void duplicateApplication_skipped() {
        enable();
        RiskJob existing = new RiskJob();
        existing.setJobId(9L);
        existing.setStatus("QUEUED");
        when(riskJobMapper.selectByAppVersion(1001L, 1)).thenReturn(existing);

        boolean queued = service.enqueue(1L, 1001L);

        assertThat(queued).isTrue();
        verify(riskJobMapper, never()).insert(any());
        verify(outboxMapper, never()).insert(any());
    }

    @Test
    @DisplayName("材料统计异常不影响入队")
    void documentCountFailure_stillQueued() {
        enable();
        when(riskJobMapper.selectByAppVersion(any(), anyInt())).thenReturn(null);
        when(assembler.buildRequest(1L, 1001L)).thenThrow(new RuntimeException("db down"));
        when(riskJobMapper.insert(any(RiskJob.class))).thenReturn(1);
        OutboxMessage outbox = new OutboxMessage();
        when(outboxMessageFactory.create(eq(BusinessType.RISK_JOB_CREATED), any(), eq(1001L)))
                .thenReturn(outbox);

        assertThat(service.enqueue(1L, 1001L)).isTrue();
        verify(outboxMapper).insert(outbox);
    }
}
