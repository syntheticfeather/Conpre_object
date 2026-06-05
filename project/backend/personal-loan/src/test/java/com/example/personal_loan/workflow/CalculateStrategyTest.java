package com.example.personal_loan.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.example.personal_loan.entity.RepaymentSchedule;
import com.example.personal_loan.enums.RepaidType;
import com.example.personal_loan.strategy.RepaymentStrategy;
import com.example.personal_loan.strategy.impl.EqualPrincipalInterestStrategy;
import com.example.personal_loan.strategy.impl.EqualPrincipalStrategy;
import com.example.personal_loan.strategy.impl.InterestFirstStrategy;
import com.example.personal_loan.strategy.impl.OneTimeRepayStrategy;

/**
 * 四种还款策略的正确性验证 — 用已知的金融公式验算。
 *
 * 关键约定：strategy.calculate() 接收的 monthlyRate 已经是月利率（小数形式），
 * 即年利率 / 100 / 12 之后的值。调用方（CalculateUtil）负责做这个转换。
 */
class CalculateStrategyTest {

    private static final int MONEY_SCALE = 2;

    // ── 等额本息 ──────────────────────────────────────

    @Test
    void equalPrincipalInterest_200k_4_5pct_36months() {
        RepaymentStrategy strategy = new EqualPrincipalInterestStrategy();
        BigDecimal loanAmount = bd("200000");
        BigDecimal monthlyRate = bd("0.00375");   // 4.5% / 100 / 12

        List<RepaymentSchedule> plan = strategy.calculate(
                loanAmount, monthlyRate, 36, LocalDate.of(2024, 1, 15));

        assertEquals(36, plan.size(), "36 期");

        // 每月月供应接近 5,949 元
        BigDecimal firstMonth = plan.get(0).getTotalAmount();
        assertTrue(firstMonth.compareTo(bd("5900")) > 0, "月供 > 5900");
        assertTrue(firstMonth.compareTo(bd("6100")) < 0, "月供 < 6100");

        // 本金之和 = 总贷款额
        BigDecimal principalSum = plan.stream()
                .map(RepaymentSchedule::getPrincipal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, principalSum.compareTo(loanAmount), "本金之和 = 贷款额");

        // 每期还款额相同（等额本息）
        BigDecimal month1 = plan.get(0).getTotalAmount();
        for (RepaymentSchedule s : plan) {
            assertEquals(0, month1.compareTo(s.getTotalAmount()), "每期还款额相同");
        }
    }

    @Test
    void equalPrincipalInterest_10k_12pct_12months() {
        RepaymentStrategy strategy = new EqualPrincipalInterestStrategy();
        BigDecimal loanAmount = bd("10000");
        BigDecimal monthlyRate = bd("0.01");  // 12% / 100 / 12

        List<RepaymentSchedule> plan = strategy.calculate(
                loanAmount, monthlyRate, 12, LocalDate.of(2024, 1, 1));

        assertEquals(12, plan.size());

        // 月供 = P * r * (1+r)^n / ((1+r)^n - 1)
        // = 10000 * 0.01 * 1.1268... / 0.1268... ≈ 888.49
        BigDecimal firstMonth = plan.get(0).getTotalAmount();
        assertTrue(firstMonth.compareTo(bd("880")) > 0, "月供 > 880");
        assertTrue(firstMonth.compareTo(bd("900")) < 0, "月供 < 900");
    }

    // ── 等额本金 ──────────────────────────────────────

    @Test
    void equalPrincipal_10k_12pct_3months() {
        RepaymentStrategy strategy = new EqualPrincipalStrategy();
        BigDecimal loanAmount = bd("10000");
        BigDecimal monthlyRate = bd("0.01");

        List<RepaymentSchedule> plan = strategy.calculate(
                loanAmount, monthlyRate, 3, LocalDate.of(2024, 1, 1));

        assertEquals(3, plan.size());

        // 每期本金 = 10000 / 3 ≈ 3333.33
        BigDecimal perPrincipal = loanAmount.divide(bd("3"), MONEY_SCALE, RoundingMode.HALF_UP);
        assertEquals(0, perPrincipal.compareTo(bd("3333.33")));

        // 本金之和 = 贷款额
        BigDecimal principalSum = plan.stream()
                .map(RepaymentSchedule::getPrincipal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, principalSum.compareTo(loanAmount), "本金之和 = 贷款额");

        // 月供递减
        BigDecimal prev = plan.get(0).getTotalAmount();
        for (int i = 1; i < plan.size(); i++) {
            BigDecimal curr = plan.get(i).getTotalAmount();
            assertTrue(curr.compareTo(prev) < 0, "月供递减");
            prev = curr;
        }
    }

    // ── 先息后本 ──────────────────────────────────────

    @Test
    void interestFirst_10k_12pct_6months() {
        RepaymentStrategy strategy = new InterestFirstStrategy();
        BigDecimal loanAmount = bd("10000");
        BigDecimal monthlyRate = bd("0.01");

        List<RepaymentSchedule> plan = strategy.calculate(
                loanAmount, monthlyRate, 6, LocalDate.of(2024, 1, 1));

        assertEquals(6, plan.size());

        // 前 5 期：只还利息 = 10000 * 0.01 = 100
        for (int i = 0; i < 5; i++) {
            assertEquals(0, bd("100.00").compareTo(plan.get(i).getInterest()), "第" + (i + 1) + "期利息=100");
            assertEquals(0, bd("0.00").compareTo(plan.get(i).getPrincipal()), "第" + (i + 1) + "期本金=0");
            assertEquals(0, bd("100.00").compareTo(plan.get(i).getTotalAmount()), "第" + (i + 1) + "期总金额=100");
        }

        // 最后一期：本金 + 利息 = 10000 + 100 = 10100
        RepaymentSchedule last = plan.get(5);
        assertEquals(0, loanAmount.compareTo(last.getPrincipal()), "末期本金=贷款额");
        assertEquals(0, bd("100.00").compareTo(last.getInterest()), "末期利息=100");
        assertEquals(0, bd("10100.00").compareTo(last.getTotalAmount()), "末期总金额=10100");
        assertEquals(0, bd("0.00").compareTo(last.getRemainingPrincipal()), "末期剩余本金=0");
    }

    // ── 一次性还本付息 ──────────────────────────────

    @Test
    void oneTimeRepay_10k_12pct_6months() {
        RepaymentStrategy strategy = new OneTimeRepayStrategy();
        BigDecimal loanAmount = bd("10000");
        BigDecimal monthlyRate = bd("0.01");

        List<RepaymentSchedule> plan = strategy.calculate(
                loanAmount, monthlyRate, 6, LocalDate.of(2024, 1, 1));

        assertEquals(1, plan.size(), "只有 1 期");

        // 利息 = 本金 * 月利率 * 期数 = 10000 * 0.01 * 6 = 600
        RepaymentSchedule s = plan.get(0);
        assertEquals(0, bd("600.00").compareTo(s.getInterest()), "总利息=600");
        assertEquals(0, loanAmount.compareTo(s.getPrincipal()), "本金=贷款额");
        assertEquals(0, bd("10600.00").compareTo(s.getTotalAmount()), "总还款=10600");
    }

    // ── 边界情况 ────────────────────────────────────

    @Test
    void zeroInterest_shouldWork() {
        BigDecimal loanAmount = bd("50000");
        BigDecimal monthlyRate = bd("0.0");

        // 所有策略在 0 利率下不应抛异常
        for (RepaymentStrategy s : List.of(
                new EqualPrincipalInterestStrategy(),
                new EqualPrincipalStrategy(),
                new InterestFirstStrategy(),
                new OneTimeRepayStrategy())) {
            List<RepaymentSchedule> plan = s.calculate(loanAmount, monthlyRate, 12, LocalDate.now());
            assertTrue(plan.size() > 0);
            // 总利息应为 0
            BigDecimal totalInterest = plan.stream()
                    .map(RepaymentSchedule::getInterest)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertEquals(0, totalInterest.compareTo(bd("0.00")), s.getClass().getSimpleName() + " 0利率");
        }
    }

    // ── helper ─────────────────────────────────────

    private static BigDecimal bd(String s) {
        return new BigDecimal(s).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}
