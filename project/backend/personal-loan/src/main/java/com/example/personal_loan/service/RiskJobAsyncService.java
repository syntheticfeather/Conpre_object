package com.example.personal_loan.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.personal_loan.dto.risk.RiskFeatureRequest;
import com.example.personal_loan.dto.risk.RiskJobCreatedEvent;
import com.example.personal_loan.entity.OutboxMessage;
import com.example.personal_loan.entity.RiskJob;
import com.example.personal_loan.enums.BusinessType;
import com.example.personal_loan.factory.OutboxMessageFactory;
import com.example.personal_loan.mapper.OutboxMapper;
import com.example.personal_loan.mapper.RiskJobMapper;
import com.example.personal_loan.service.assembler.RiskFeatureAssembler;

import lombok.extern.slf4j.Slf4j;

/**
 * L7.2 风控异步链路入口。
 * <p>
 * 申请提交时（{@code risk.async.enabled=true}）由 {@code ApplicationServiceImpl} 调用：
 * 在同一事务内写一条 status=QUEUED 的 {@code risk_job}，并写 Outbox 消息
 * {@code RISK_JOB_CREATED}，由 Outbox 轮询器投递到 {@code risk.exchange / risk.job.created}。
 * <p>
 * 设计约束：本服务<b>只负责入队和消息投递，不做任何打分</b>。
 * Python 侧 LLM 只输出 G/H 特征，最终分数由消费端 {@link RiskJobResultHandler}
 * 调用 {@link com.example.personal_loan.service.impl.FullRiskScoringService} 计算。
 * <p>
 * 与 L7.1 的关系：
 * <ul>
 *   <li>{@code risk.async.enabled=false}（默认）→ 沿用 L7.1 同步闭环</li>
 *   <li>{@code risk.async.enabled=true} → 走异步，申请提交不再等待 Python</li>
 * </ul>
 */
@Slf4j
@Service
public class RiskJobAsyncService {

    /** L7.1/L7.2 共用同一个 job_version=1 的幂等键 */
    private static final int JOB_VERSION = 1;

    private final RiskJobMapper riskJobMapper;
    private final OutboxMapper outboxMapper;
    private final OutboxMessageFactory outboxMessageFactory;
    private final RiskFeatureAssembler riskFeatureAssembler;

    @Value("${risk.async.enabled:false}")
    private boolean asyncEnabled;

    public RiskJobAsyncService(RiskJobMapper riskJobMapper,
                               OutboxMapper outboxMapper,
                               OutboxMessageFactory outboxMessageFactory,
                               RiskFeatureAssembler riskFeatureAssembler) {
        this.riskJobMapper = riskJobMapper;
        this.outboxMapper = outboxMapper;
        this.outboxMessageFactory = outboxMessageFactory;
        this.riskFeatureAssembler = riskFeatureAssembler;
    }

    /**
     * 写入 QUEUED 任务并投递创建事件。
     * <p>
     * 幂等：同一 application_id + job_version 已存在时直接跳过。
     *
     * @return true=已入队或已存在；false=开关关闭，未做任何事
     */
    @Transactional
    public boolean enqueue(Long userId, Long applicationId) {
        if (!asyncEnabled) {
            log.info("[L7.2] risk.async.enabled=false，跳过异步入队 userId={} appId={}",
                    userId, applicationId);
            return false;
        }

        RiskJob existing = riskJobMapper.selectByAppVersion(applicationId, JOB_VERSION);
        if (existing != null) {
            log.info("[L7.2] risk_job 已存在，跳过入队 appId={} jobId={} status={}",
                    applicationId, existing.getJobId(), existing.getStatus());
            return true;
        }

        RiskJob job = new RiskJob();
        job.setApplicationId(applicationId);
        job.setUserId(userId);
        job.setJobVersion(JOB_VERSION);
        job.setStatus("QUEUED");
        job.setStage("QUEUED");
        job.setAttempt(0);
        job.setFeatureSet("full");
        job.setLlmFeatureAvailable(0);
        job.setDocumentCount(countDocuments(userId, applicationId));
        job.setConvertedCount(0);
        // 分数 / modelVersion 由结果消费端写入，入队时不预设
        riskJobMapper.insert(job);

        RiskJobCreatedEvent event = RiskJobCreatedEvent.of(
                job.getJobId(), applicationId, userId, JOB_VERSION, 0);
        OutboxMessage outbox = outboxMessageFactory.create(
                BusinessType.RISK_JOB_CREATED, event, applicationId);
        outboxMapper.insert(outbox);

        log.info("[L7.2] 入队成功 appId={} jobId={} messageId={}",
                applicationId, job.getJobId(), outbox.getMessageId());
        return true;
    }

    public boolean isEnabled() {
        return asyncEnabled;
    }

    /** 材料数量仅用于展示/排查，失败不影响入队 */
    private int countDocuments(Long userId, Long applicationId) {
        try {
            RiskFeatureRequest request = riskFeatureAssembler.buildRequest(userId, applicationId);
            return request.getDocuments() == null ? 0 : request.getDocuments().size();
        } catch (Exception e) {
            log.warn("[L7.2] 统计材料数量失败 appId={}: {}", applicationId, e.getMessage());
            return 0;
        }
    }
}
