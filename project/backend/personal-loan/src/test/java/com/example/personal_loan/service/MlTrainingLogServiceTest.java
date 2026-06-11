package com.example.personal_loan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.personal_loan.entity.BlackUser;
import com.example.personal_loan.entity.ImmovablesCert;
import com.example.personal_loan.entity.LoanApplication;
import com.example.personal_loan.entity.MlTrainingLog;
import com.example.personal_loan.entity.TriCert;
import com.example.personal_loan.entity.UserCert;
import com.example.personal_loan.entity.WorkCert;
import com.example.personal_loan.enums.ApplicationStatus;
import com.example.personal_loan.enums.RepaidType;
import com.example.personal_loan.mapper.ApplicationMapper;
import com.example.personal_loan.mapper.BlackListMapper;
import com.example.personal_loan.mapper.ImmovablesCertMapper;
import com.example.personal_loan.mapper.MlTrainingLogMapper;
import com.example.personal_loan.mapper.OrderMapper;
import com.example.personal_loan.mapper.PostponeRequestMapper;
import com.example.personal_loan.mapper.RepaymentScheduleMapper;
import com.example.personal_loan.mapper.TriCertMapper;
import com.example.personal_loan.mapper.UserCertMapper;
import com.example.personal_loan.mapper.WorkCertMapper;
import com.example.personal_loan.service.RiskScoringService;
import com.example.personal_loan.service.impl.MlTrainingLogServiceImpl;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * ML 训练数据采集 — 单元测试（Mockito）
 *
 * 验证 collectFeatures() 的核心逻辑:
 *   1. 从各 mapper 加载数据
 *   2. 组装 50+ 特征到 JSON
 *   3. 写入 ml_training_log
 */
@ExtendWith(MockitoExtension.class)
class MlTrainingLogServiceTest {

    @Mock private MlTrainingLogMapper mlTrainingLogMapper;
    @Mock private UserCertMapper userCertMapper;
    @Mock private WorkCertMapper workCertMapper;
    @Mock private TriCertMapper triCertMapper;
    @Mock private ImmovablesCertMapper immovablesCertMapper;
    @Mock private BlackListMapper blackListMapper;
    @Mock private ApplicationMapper applicationMapper;
    @Mock private OrderMapper orderMapper;
    @Mock private RepaymentScheduleMapper repaymentScheduleMapper;
    @Mock private PostponeRequestMapper postponeRequestMapper;
    @Mock private RiskScoringService riskScoringService;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private MlTrainingLogServiceImpl service;

    private final Long testUserId = 5L;
    private final Long testAppId = 100L;

    @BeforeEach
    void setUp() {
        service = new MlTrainingLogServiceImpl(
            mlTrainingLogMapper, userCertMapper, workCertMapper,
            triCertMapper, immovablesCertMapper, blackListMapper,
            applicationMapper, orderMapper, repaymentScheduleMapper,
            postponeRequestMapper, riskScoringService, objectMapper
        );
    }

    @Test
    @DisplayName("collectFeatures — 完整采集流程，验证特征 JSON 包含所有预期键")
    void testCollectFeatures() throws Exception {
        // ── 准备 Mock 数据 ──
        LoanApplication app = buildTestApplication();
        when(applicationMapper.selectById(testAppId)).thenReturn(app);

        UserCert userCert = buildTestUserCert();
        when(userCertMapper.selectByUserId(testUserId)).thenReturn(userCert);

        // 证书表（外键为 null → 不会调用 mapper，用 lenient 避免 UnnecessaryStubbing）
        lenient().when(workCertMapper.selectById(anyInt())).thenReturn(null);
        lenient().when(triCertMapper.selectById(anyInt())).thenReturn(null);
        lenient().when(immovablesCertMapper.selectById(anyInt())).thenReturn(null);
        when(blackListMapper.selectActiveByUserId(testUserId)).thenReturn(null);

        // 动态聚合（新用户无历史数据）
        when(orderMapper.selectAllByUserId(testUserId)).thenReturn(null);
        when(applicationMapper.selectByUserId(testUserId)).thenReturn(null);
        when(postponeRequestMapper.selectByUserId(testUserId)).thenReturn(null);

        // 打分服务（测试中模型不可用）
        when(riskScoringService.isReady()).thenReturn(false);

        // ── 执行 ──
        service.collectFeatures(testUserId, testAppId);

        // ── 捕获写入的 MlTrainingLog ──
        ArgumentCaptor<MlTrainingLog> captor = ArgumentCaptor.forClass(MlTrainingLog.class);
        verify(mlTrainingLogMapper).insert(captor.capture());
        MlTrainingLog captured = captor.getValue();

        assertThat(captured.getUserId()).isEqualTo(testUserId.intValue());
        assertThat(captured.getApplicationId()).isEqualTo(testAppId.intValue());
        assertThat(captured.getFeatures()).isNotBlank();

        // ── 验证 features JSON ──
        Map<String, Object> features = objectMapper.readValue(
            captured.getFeatures(), new TypeReference<Map<String, Object>>() {}
        );

        System.out.println("=== 采集到 " + features.size() + " 个特征 ===");
        features.forEach((k, v) -> System.out.println("  " + k + " = " + v));

        // 身份特征
        assertThat(features).containsKeys(
            "age", "gender", "region_code", "info_completeness"
        );

        // 静态特征（新用户全部为 0）
        assertThat(features).containsKeys(
            "has_employment_cert", "has_salary_cert",
            "has_social_security", "has_credit_report",
            "has_house", "has_car", "has_mortgage"
        );

        // 信用特征
        assertThat(features).containsKeys(
            "credit_score", "is_blacklisted", "black_level",
            "active_loan_count", "total_loan_count",
            "max_overdue_days", "overdue_order_count"
        );

        // 行为特征
        assertThat(features).containsKeys(
            "has_postpone_history", "postpone_count",
            "on_time_payment_ratio", "overdue_schedule_ratio"
        );

        // 申请特征
        assertThat(features).containsKeys(
            "applied_amount", "applied_term", "application_hour",
            "product_type", "repaid_type", "rejected_application_count",
            "approved_application_count", "rejection_rate"
        );

        System.out.println("=== collectFeatures 单元测试通过: " + features.size() + " 个特征 ===");
    }

    @Test
    @DisplayName("markCompleted — 标签成功补填")
    void testMarkCompleted() {
        when(mlTrainingLogMapper.updateResult(eq(testAppId), eq(0), any(LocalDateTime.class)))
            .thenReturn(1);

        service.markCompleted(testAppId);

        verify(mlTrainingLogMapper).updateResult(eq(testAppId), eq(0), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("markDefaulted — 违约标签成功补填")
    void testMarkDefaulted() {
        when(mlTrainingLogMapper.updateResult(eq(testAppId), eq(1), any(LocalDateTime.class)))
            .thenReturn(1);

        service.markDefaulted(testAppId);

        verify(mlTrainingLogMapper).updateResult(eq(testAppId), eq(1), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("collectFeatures — 申请不存在时不抛异常")
    void testCollectFeatures_AppNotFound() {
        when(applicationMapper.selectById(testAppId)).thenReturn(null);

        // 不应抛异常
        service.collectFeatures(testUserId, testAppId);
    }

    // ── 辅助方法 ──

    private LoanApplication buildTestApplication() {
        LoanApplication app = new LoanApplication();
        app.setId(testAppId);
        app.setUserId(testUserId);
        app.setProductId(1L);
        app.setStatus(ApplicationStatus.审核中);
        app.setLoanAmount(new java.math.BigDecimal("50000"));
        app.setInterestRate(new java.math.BigDecimal("0.0650"));
        app.setLoanPeriod(1);
        app.setTerm(12);
        app.setRepaidType(RepaidType.等额本息);
        app.setApplyTime(LocalDateTime.of(2026, 6, 11, 14, 30));
        return app;
    }

    private UserCert buildTestUserCert() {
        UserCert cert = new UserCert();
        cert.setUserId(testUserId);
        cert.setRealName("张三");
        cert.setIdCard("110101199003071234"); // 有效身份证: 男, 1990-03-07, 北京
        cert.setCreditScore(75);
        cert.setBankCardId("6222021234567890123");
        cert.setWorkCertId(null);   // 未上传
        cert.setTriCertId(null);
        cert.setImmovableCertId(null);
        return cert;
    }
}
