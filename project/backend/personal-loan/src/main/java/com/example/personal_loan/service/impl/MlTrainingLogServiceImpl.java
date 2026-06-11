package com.example.personal_loan.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.personal_loan.entity.BlackUser;
import com.example.personal_loan.entity.ImmovablesCert;
import com.example.personal_loan.entity.LoanApplication;
import com.example.personal_loan.entity.MlTrainingLog;
import com.example.personal_loan.entity.Order;
import com.example.personal_loan.entity.PostponeRequest;
import com.example.personal_loan.entity.RepaymentSchedule;
import com.example.personal_loan.entity.TriCert;
import com.example.personal_loan.entity.UserCert;
import com.example.personal_loan.entity.WorkCert;
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
import com.example.personal_loan.service.MlTrainingLogService;
import com.example.personal_loan.service.RiskScoringService;
import com.example.personal_loan.utils.IdCardUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * ML 训练数据采集服务实现
 *
 * 在每次贷款申请提交时: 1. 采集静态特征（用户画像、证书、资产） 2. 计算动态特征（历史借贷行为聚合） 3. 提取本次申请特征 4. 写入
 * ml_training_log 表
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MlTrainingLogServiceImpl implements MlTrainingLogService {

    private final MlTrainingLogMapper mlTrainingLogMapper;
    private final UserCertMapper userCertMapper;
    private final WorkCertMapper workCertMapper;
    private final TriCertMapper triCertMapper;
    private final ImmovablesCertMapper immovablesCertMapper;
    private final BlackListMapper blackListMapper;
    private final ApplicationMapper applicationMapper;
    private final OrderMapper orderMapper;
    private final RepaymentScheduleMapper repaymentScheduleMapper;
    private final PostponeRequestMapper postponeRequestMapper;
    private final RiskScoringService riskScoringService;
    private final ObjectMapper objectMapper;

    /** XGBoost 模型需要的 9 个特征名 */
    private static final String[] MODEL_FEATURE_NAMES = {
        "age", "monthly_income", "dti_ratio", "active_loan_count",
        "has_mortgage", "max_overdue_days_90", "overdue_30_59_count",
        "overdue_60_89_count", "credit_card_usage_pct"
    };

    // ═══════════════════════════════════════════════════════
    //  公开方法
    // ═══════════════════════════════════════════════════════

    @Override
    public Map<String, Object> extractModelFeatures(Long userId, Long applicationId) {
        LoanApplication app = applicationMapper.selectById(applicationId);
        if (app == null) return java.util.Collections.emptyMap();

        UserCert userCert = userCertMapper.selectByUserId(userId);
        WorkCert workCert = loadWorkCert(userCert);
        TriCert triCert = loadTriCert(userCert);
        ImmovablesCert immovableCert = loadImmovablesCert(userCert);
        BlackUser blackUser = blackListMapper.selectActiveByUserId(userId);

        Map<String, Object> all = new HashMap<>();
        all.putAll(computeStaticFeatures(userCert, workCert, triCert, immovableCert, blackUser));
        all.putAll(computeOrderAggregations(userId));
        all.putAll(computePerApplicationFeatures(app));

        // 只返回模型需要的 9 个特征
        Map<String, Object> modelFeatures = new HashMap<>();
        for (String name : MODEL_FEATURE_NAMES) {
            modelFeatures.put(name, all.getOrDefault(name, 0));
        }
        return modelFeatures;
    }

    @Override
    @Transactional
    public void collectFeatures(Long userId, Long applicationId) {
        try {
            LoanApplication app = applicationMapper.selectById(applicationId);
            if (app == null) {
                log.warn("collectFeatures: 申请不存在 id={}", applicationId);
                return;
            }

            Map<String, Object> features = new HashMap<>();

            // 加载静态数据
            UserCert userCert = userCertMapper.selectByUserId(userId);
            WorkCert workCert = loadWorkCert(userCert);
            TriCert triCert = loadTriCert(userCert);
            ImmovablesCert immovableCert = loadImmovablesCert(userCert);
            BlackUser blackUser = blackListMapper.selectActiveByUserId(userId);

            // 逐组计算特征
            features.putAll(computeStaticFeatures(userCert, workCert, triCert, immovableCert, blackUser));
            features.putAll(computeOrderAggregations(userId));
            features.putAll(computeApplicationAggregations(userId, app));
            features.putAll(computeRepaymentAggregations(userId));
            features.putAll(computePostponeAggregations(userId));
            features.putAll(computePerApplicationFeatures(app));

            // 从 user_certification 取 credit_score（由 ApplicationServiceImpl 提前写入）
            int creditScore = userCert != null && userCert.getCreditScore() != null
                ? userCert.getCreditScore() : 0;
            features.put("credit_score", creditScore);

            MlTrainingLog trainingLog = new MlTrainingLog();
            trainingLog.setUserId(userId.intValue());
            trainingLog.setApplicationId(applicationId.intValue());
            trainingLog.setFeatures(objectMapper.writeValueAsString(features));
            trainingLog.setModelVersion(riskScoringService.isReady()
                ? riskScoringService.getModelVersion() : "none");

            mlTrainingLogMapper.insert(trainingLog);
            log.info("collectFeatures: userId=" + userId + " appId=" + applicationId
                     + " features=" + features.size() + " credit_score=" + creditScore);

        } catch (Exception e) {
            // 采集失败不阻塞主流程
            log.error("collectFeatures 失败 userId={} appId={}: {}", userId, applicationId, e.getMessage(), e);
        }
    }

    @Override
    public void markCompleted(Long applicationId) {
        updateLabel(applicationId, 0);
    }

    @Override
    public void markDefaulted(Long applicationId) {
        updateLabel(applicationId, 1);
    }

    private void updateLabel(Long applicationId, int result) {
        try {
            int updated = mlTrainingLogMapper.updateResult(applicationId, result, LocalDateTime.now());
            if (updated > 0) {
                log.info("updateLabel: appId={} result={} rows={}", applicationId, result, updated);
            }
        } catch (Exception e) {
            log.error("updateLabel 失败 appId={}: {}", applicationId, e.getMessage(), e);
        }
    }

    // ═══════════════════════════════════════════════════════
    //  辅助: 加载证书表（通过 user_certification 的外键）
    // ═══════════════════════════════════════════════════════
    private WorkCert loadWorkCert(UserCert userCert) {
        if (userCert == null || userCert.getWorkCertId() == null) {
            return null;
        }
        return workCertMapper.selectById(userCert.getWorkCertId());
    }

    private TriCert loadTriCert(UserCert userCert) {
        if (userCert == null || userCert.getTriCertId() == null) {
            return null;
        }
        return triCertMapper.selectById(userCert.getTriCertId());
    }

    private ImmovablesCert loadImmovablesCert(UserCert userCert) {
        if (userCert == null || userCert.getImmovableCertId() == null) {
            return null;
        }
        return immovablesCertMapper.selectById(userCert.getImmovableCertId());
    }

    // ═══════════════════════════════════════════════════════
    //  A. 静态特征 (17个)
    // ═══════════════════════════════════════════════════════
    private Map<String, Object> computeStaticFeatures(
            UserCert userCert, WorkCert workCert, TriCert triCert,
            ImmovablesCert immovable, BlackUser black) {

        Map<String, Object> f = new HashMap<>();

        // ── 从身份证提取 ──
        if (userCert != null && userCert.getIdCard() != null
                && IdCardUtils.isValid(userCert.getIdCard())) {
            String idCard = userCert.getIdCard();
            String birthStr = IdCardUtils.getBirthDate(idCard);
            if (birthStr != null) {
                LocalDate birthDate = LocalDate.parse(birthStr, DateTimeFormatter.ISO_LOCAL_DATE);
                f.put("age", Period.between(birthDate, LocalDate.now()).getYears());
            }
            f.put("gender", IdCardUtils.isMale(idCard) ? 1 : 0);
            f.put("region_code", idCard.substring(0, 2));
        } else {
            f.put("age", 30);
            f.put("gender", 0);
            f.put("region_code", "00");
        }

        // ── 工作证明 ──
        f.put("has_employment_cert",
                workCert != null && workCert.getEmploymentCertPath() != null
                && !workCert.getEmploymentCertPath().isEmpty() ? 1 : 0);
        f.put("has_salary_cert",
                workCert != null && workCert.getSalaryCertPath() != null
                && !workCert.getSalaryCertPath().isEmpty() ? 1 : 0);

        // ── 第三方认证 ──
        f.put("has_social_security",
                triCert != null && triCert.getSocialSecurityPath() != null
                && !triCert.getSocialSecurityPath().isEmpty() ? 1 : 0);
        f.put("has_credit_report",
                triCert != null && triCert.getCreditReportPath() != null
                && !triCert.getCreditReportPath().isEmpty() ? 1 : 0);

        // ── 不动产 ──
        f.put("has_house",
                immovable != null && immovable.getPropertyCertPath() != null
                && !immovable.getPropertyCertPath().isEmpty() ? 1 : 0);
        f.put("has_car",
                immovable != null && immovable.getCarCertPath() != null
                && !immovable.getCarCertPath().isEmpty() ? 1 : 0);
        f.put("total_asset_value",
                immovable != null && immovable.getTotalValue() != null
                ? immovable.getTotalValue() : 0);
        f.put("has_mortgage",
                f.get("has_house").equals(1)
                && f.get("total_asset_value") instanceof Integer
                && (Integer) f.get("total_asset_value") > 0 ? 1 : 0);

        // ── 信用评分 ──
        f.put("credit_score",
                userCert != null && userCert.getCreditScore() != null
                ? userCert.getCreditScore() : 0);

        // ── 黑名单 ──
        f.put("is_blacklisted", black != null ? 1 : 0);
        f.put("black_level",
                black != null ? black.getBlackLevel() : 0);

        // ── 银行卡 ──
        f.put("has_bank_card",
                userCert != null && userCert.getBankCardId() != null
                && !userCert.getBankCardId().isEmpty() ? 1 : 0);

        // ── 信息完整度 ──
        int totalCerts = 8;  // employment, salary, social, credit, house, car, bank, idCard
        int completed = 0;
        if ((Integer) f.get("has_employment_cert") == 1) {
            completed++;
        }
        if ((Integer) f.get("has_salary_cert") == 1) {
            completed++;
        }
        if ((Integer) f.get("has_social_security") == 1) {
            completed++;
        }
        if ((Integer) f.get("has_credit_report") == 1) {
            completed++;
        }
        if ((Integer) f.get("has_house") == 1) {
            completed++;
        }
        if ((Integer) f.get("has_car") == 1) {
            completed++;
        }
        if ((Integer) f.get("has_bank_card") == 1) {
            completed++;
        }
        if (userCert != null && userCert.getIdCard() != null) {
            completed++;
        }
        f.put("info_completeness", Math.round(completed * 100.0 / totalCerts));

        return f;
    }

    // ═══════════════════════════════════════════════════════
    //  B. 动态特征 - orders 聚合 (11个)
    // ═══════════════════════════════════════════════════════
    private Map<String, Object> computeOrderAggregations(Long userId) {
        Map<String, Object> f = new HashMap<>();
        List<Order> orders = orderMapper.selectAllByUserId(userId);

        if (orders == null || orders.isEmpty()) {
            // 新用户: 全部默认为 0
            f.put("active_loan_count", 0);
            f.put("total_loan_count", 0);
            f.put("total_loan_amount", 0);
            f.put("total_repaid_amount", 0);
            f.put("repayment_ratio", 0.0);
            f.put("max_overdue_days", 0);
            f.put("total_overdue_days", 0);
            f.put("overdue_order_count", 0);
            f.put("max_loan_amount_single", 0);
            f.put("avg_interest_rate", 0.0);
            f.put("completed_order_count", 0);
            return f;
        }

        int activeCount = 0, overdueCount = 0, completedCount = 0;
        BigDecimal totalLoan = BigDecimal.ZERO;
        BigDecimal totalRepaid = BigDecimal.ZERO;
        int maxOverdue = 0, sumOverdue = 0;
        BigDecimal maxLoanSingle = BigDecimal.ZERO;
        BigDecimal sumRate = BigDecimal.ZERO;

        for (Order o : orders) {
            String status = o.getStatus() != null ? o.getStatus().name() : "";
            if (!"已完成".equals(status)) {
                activeCount++;
            }
            if ("已逾期".equals(status)) {
                overdueCount++;
            }
            if ("已完成".equals(status)) {
                completedCount++;
            }

            totalLoan = totalLoan.add(o.getLoanAmount() != null ? o.getLoanAmount() : BigDecimal.ZERO);
            totalRepaid = totalRepaid.add(o.getRepaidAmount() != null ? o.getRepaidAmount() : BigDecimal.ZERO);

            int od = o.getOverdueDays() != null ? o.getOverdueDays() : 0;
            maxOverdue = Math.max(maxOverdue, od);
            sumOverdue += od;

            if (o.getLoanAmount() != null && o.getLoanAmount().compareTo(maxLoanSingle) > 0) {
                maxLoanSingle = o.getLoanAmount();
            }
            if (o.getInterestRate() != null) {
                sumRate = sumRate.add(o.getInterestRate());
            }
        }

        f.put("active_loan_count", activeCount);
        f.put("total_loan_count", orders.size());
        f.put("total_loan_amount", totalLoan.setScale(2, RoundingMode.HALF_UP).doubleValue());
        f.put("total_repaid_amount", totalRepaid.setScale(2, RoundingMode.HALF_UP).doubleValue());
        f.put("repayment_ratio",
                totalLoan.compareTo(BigDecimal.ZERO) > 0
                ? totalRepaid.divide(totalLoan, 4, RoundingMode.HALF_UP).doubleValue() : 0.0);
        f.put("max_overdue_days", maxOverdue);
        f.put("total_overdue_days", sumOverdue);
        f.put("overdue_order_count", overdueCount);
        f.put("max_loan_amount_single", maxLoanSingle.setScale(2, RoundingMode.HALF_UP).doubleValue());
        f.put("avg_interest_rate",
                orders.size() > 0
                ? sumRate.divide(BigDecimal.valueOf(orders.size()), 6, RoundingMode.HALF_UP).doubleValue() : 0.0);
        f.put("completed_order_count", completedCount);

        return f;
    }

    // ═══════════════════════════════════════════════════════
    //  C. 动态特征 - loan_applications 聚合 (8个)
    // ═══════════════════════════════════════════════════════
    private Map<String, Object> computeApplicationAggregations(Long userId, LoanApplication currentApp) {
        Map<String, Object> f = new HashMap<>();
        List<LoanApplication> apps = applicationMapper.selectByUserId(userId);

        if (apps == null || apps.isEmpty()) {
            f.put("total_application_count", 1);  // 包含当前申请
            f.put("rejected_application_count", 0);
            f.put("rejection_rate", 0.0);
            f.put("approved_application_count", 0);
            f.put("avg_application_amount", currentApp.getLoanAmount() != null
                    ? currentApp.getLoanAmount().doubleValue() : 0.0);
            f.put("last_application_days_ago", 0);
            f.put("avg_review_time_minutes", 0);
            return f;
        }

        int total = apps.size();
        int rejected = 0, approved = 0;
        BigDecimal sumAmount = BigDecimal.ZERO;
        long sumReviewMinutes = 0;
        int reviewedCount = 0;

        for (LoanApplication a : apps) {
            String status = a.getStatus() != null ? a.getStatus().name() : "";
            if ("AI拒绝".equals(status) || "人工拒绝".equals(status)) {
                rejected++;
            }
            if ("AI通过".equals(status) || "人工通过".equals(status)) {
                approved++;
            }

            if (a.getLoanAmount() != null) {
                sumAmount = sumAmount.add(a.getLoanAmount());
            }

            if (a.getApplyTime() != null && a.getReviewTime() != null) {
                sumReviewMinutes += ChronoUnit.MINUTES.between(a.getApplyTime(), a.getReviewTime());
                reviewedCount++;
            }
        }

        // 最近一次申请距今天数
        long lastDays = 0;
        if (apps.size() >= 2 && apps.get(1).getApplyTime() != null) {
            // apps 按 apply_time DESC 排序, index 0 是当前, index 1 是上一次
            lastDays = ChronoUnit.DAYS.between(apps.get(1).getApplyTime().toLocalDate(), LocalDate.now());
        }

        f.put("total_application_count", total);
        f.put("rejected_application_count", rejected);
        f.put("rejection_rate", total > 0 ? (double) rejected / total : 0.0);
        f.put("approved_application_count", approved);
        f.put("avg_application_amount",
                total > 0
                        ? sumAmount.divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP).doubleValue() : 0.0);
        f.put("last_application_days_ago", (int) lastDays);
        f.put("avg_review_time_minutes",
                reviewedCount > 0 ? (int) (sumReviewMinutes / reviewedCount) : 0);

        return f;
    }

    // ═══════════════════════════════════════════════════════
    //  D. 动态特征 - repayment_schedule 聚合 (8个)
    // ═══════════════════════════════════════════════════════
    private Map<String, Object> computeRepaymentAggregations(Long userId) {
        Map<String, Object> f = new HashMap<>();
        List<Order> orders = orderMapper.selectAllByUserId(userId);

        if (orders == null || orders.isEmpty()) {
            f.put("on_time_payment_count", 0);
            f.put("late_payment_count", 0);
            f.put("overdue_schedule_count", 0);
            f.put("total_schedule_count", 0);
            f.put("on_time_payment_ratio", 0.0);
            f.put("overdue_schedule_ratio", 0.0);
            f.put("avg_days_past_due", 0);
            f.put("max_days_past_due", 0);
            return f;
        }

        int onTime = 0, late = 0, overdueSch = 0, totalSch = 0;
        long sumPastDue = 0;
        int maxPastDue = 0;
        int lateCountForAvg = 0;

        for (Order order : orders) {
            List<RepaymentSchedule> schedules = repaymentScheduleMapper.selectByOrderId(order.getId());
            if (schedules == null) {
                continue;
            }

            for (RepaymentSchedule sch : schedules) {
                totalSch++;
                String status = sch.getStatus();
                if ("已还".equals(status)) {
                    if (sch.getDueDate() != null && sch.getActualPayDate() != null) {
                        long daysLate = ChronoUnit.DAYS.between(sch.getDueDate(), sch.getActualPayDate());
                        if (daysLate > 0) {
                            late++;
                            sumPastDue += daysLate;
                            maxPastDue = (int) Math.max(maxPastDue, daysLate);
                            lateCountForAvg++;
                        } else {
                            onTime++;
                        }
                    } else {
                        onTime++;  // 无实际日期按按时算
                    }
                } else if ("逾期".equals(status)) {
                    overdueSch++;
                }
            }
        }

        f.put("on_time_payment_count", onTime);
        f.put("late_payment_count", late);
        f.put("overdue_schedule_count", overdueSch);
        f.put("total_schedule_count", totalSch);
        f.put("on_time_payment_ratio", totalSch > 0 ? (double) onTime / totalSch : 0.0);
        f.put("overdue_schedule_ratio", totalSch > 0 ? (double) overdueSch / totalSch : 0.0);
        f.put("avg_days_past_due", lateCountForAvg > 0 ? (int) (sumPastDue / lateCountForAvg) : 0);
        f.put("max_days_past_due", maxPastDue);

        return f;
    }

    // ═══════════════════════════════════════════════════════
    //  E. 动态特征 - postpone_request 聚合 (3个)
    // ═══════════════════════════════════════════════════════
    private Map<String, Object> computePostponeAggregations(Long userId) {
        Map<String, Object> f = new HashMap<>();
        List<PostponeRequest> postpones = postponeRequestMapper.selectByUserId(userId);

        if (postpones == null || postpones.isEmpty()) {
            f.put("has_postpone_history", 0);
            f.put("postpone_count", 0);
            f.put("approved_postpone_count", 0);
            return f;
        }

        int approved = 0;
        for (PostponeRequest p : postpones) {
            if ("已通过".equals(p.getStatus())) {
                approved++;
            }
        }

        f.put("has_postpone_history", 1);
        f.put("postpone_count", postpones.size());
        f.put("approved_postpone_count", approved);
        return f;
    }

    // ═══════════════════════════════════════════════════════
    //  F. 本次申请特征 (7个)
    // ═══════════════════════════════════════════════════════
    private Map<String, Object> computePerApplicationFeatures(LoanApplication app) {
        Map<String, Object> f = new HashMap<>();

        f.put("applied_amount", app.getLoanAmount() != null
                ? app.getLoanAmount().doubleValue() : 0.0);
        f.put("applied_loan_period", app.getLoanPeriod() != null ? app.getLoanPeriod() : 0);
        f.put("applied_term", app.getTerm() != null ? app.getTerm() : 0);
        f.put("product_type", app.getProductId() != null ? app.getProductId().intValue() : 0);
        f.put("repaid_type", app.getRepaidType() != null ? app.getRepaidType().name() : "unknown");

        // 申请时间特征
        if (app.getApplyTime() != null) {
            int hour = app.getApplyTime().getHour();
            f.put("application_hour", hour);
            f.put("is_late_night_apply", (hour >= 0 && hour < 6) ? 1 : 0);
        } else {
            f.put("application_hour", 12);
            f.put("is_late_night_apply", 0);
        }

        return f;
    }
}
