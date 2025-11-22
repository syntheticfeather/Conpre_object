package com.example.personal_loan.utils;

import java.util.List;


import java.math.BigDecimal;
import java.util.ArrayList;
import com.example.personal_loan.entity.Order;
import com.example.personal_loan.enums.RepaidType;


public final class CalculateUtil {

    private CalculateUtil() {
        // 工具类禁止实例化
    }

    /**
     * 计算总交易笔数：状态为 SETTLED 的订单数量
     */
    public static Integer getTotalTransactionCount(List<Order> orders) {
        return (int) orders.stream()
            .filter(order -> "SETTLED".equals(order.getStatus()))
            .count();
    }

    /**
     * 计算总贷款金额：所有订单的 loanAmount 求和
     */
    public static BigDecimal getTotalLoanAmount(List<Order> orders) {
        return orders.stream()
            .map(Order::getLoanAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 计算总还款金额：所有订单的 repaidAmount 求和
     */
    public static BigDecimal getTotalRepaidAmount(List<Order> orders) {
        return orders.stream()
            .map(Order::getRepaidAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 根据还款方式和参数，计算每期应还金额（简化版）
     *
     * @param loanAmount 贷款本金
     * @param interestRate 年化利率（如 8.5% → 0.085）
     * @param term 总期数（月）
     * @param repaidType 还款方式
     * @return 每期还款明细列表（含本金、利息、总金额）
     */
    public static List<RepaymentPlanItem> calculateRepaymentPlan(
            BigDecimal loanAmount,
            BigDecimal interestRate,
            Integer term,
            RepaidType repaidType) {

        if (loanAmount == null || interestRate == null || term == null || term <= 0) {
            throw new IllegalArgumentException("参数不能为空");
        }

        List<RepaymentPlanItem> plan = new ArrayList<>();

        switch (repaidType) {
            case 等额本息:
                plan.addAll(calculateEqualPrincipalInterest(loanAmount, interestRate, term));
                break;
            case 等额本金:
                plan.addAll(calculateEqualPrincipal(loanAmount, interestRate, term));
                break;
            case 先息后本:
                plan.addAll(calculateInterestFirst(loanAmount, interestRate, term));
                break;
            case 一次性还本付息:
                plan.addAll(calculateOneTimeRepay(loanAmount, interestRate, term));
                break;
            default:
                throw new IllegalArgumentException("不支持的还款方式：" + repaidType);
        }

        return plan;
    }

    // 根据还款方式计算当期应还金额
    public static BigDecimal calculateCurrentTermPayment(Order order) {
        List<RepaymentPlanItem> plan = CalculateUtil.calculateRepaymentPlan(
            order.getLoanAmount(),
            order.getInterestRate(),
            order.getTerm(),
            order.getRepaidType()
        );
        int currentTermIndex = order.getCurrentTerm(); // 当前期从 0 开始？还是 1？
        // 假设 currentTerm 表示“已还期数”，下一期是 currentTerm + 1
        if (currentTermIndex < plan.size()) {
            return plan.get(currentTermIndex).getTotal();
        }
        return BigDecimal.ZERO;
    }

    // ==================== 具体还款方式实现 ====================

    private static List<RepaymentPlanItem> calculateEqualPrincipalInterest(
            BigDecimal loanAmount, BigDecimal interestRate, Integer term) {
        // 等额本息：每月还款额固定
        BigDecimal monthlyRate = interestRate.divide(BigDecimal.valueOf(12), 6, BigDecimal.ROUND_HALF_UP);
        BigDecimal factor = monthlyRate.multiply(BigDecimal.ONE.add(monthlyRate).pow(term))
                .divide(BigDecimal.ONE.add(monthlyRate).pow(term).subtract(BigDecimal.ONE), 6, BigDecimal.ROUND_HALF_UP);

        BigDecimal monthlyPayment = loanAmount.multiply(factor);
        BigDecimal remainingPrincipal = loanAmount;

        List<RepaymentPlanItem> result = new ArrayList<>();
        for (int i = 1; i <= term; i++) {
            BigDecimal interest = remainingPrincipal.multiply(monthlyRate);
            BigDecimal principal = monthlyPayment.subtract(interest);
            remainingPrincipal = remainingPrincipal.subtract(principal);

            result.add(new RepaymentPlanItem(i, principal, interest, monthlyPayment));
        }
        return result;
    }

    private static List<RepaymentPlanItem> calculateEqualPrincipal(
            BigDecimal loanAmount, BigDecimal interestRate, Integer term) {
        // 等额本金：每月本金固定，利息递减
        BigDecimal monthlyPrincipal = loanAmount.divide(BigDecimal.valueOf(term), 2, BigDecimal.ROUND_HALF_UP);
        BigDecimal remainingPrincipal = loanAmount;
        BigDecimal monthlyRate = interestRate.divide(BigDecimal.valueOf(12), 6, BigDecimal.ROUND_HALF_UP);

        List<RepaymentPlanItem> result = new ArrayList<>();
        for (int i = 1; i <= term; i++) {
            BigDecimal interest = remainingPrincipal.multiply(monthlyRate);
            BigDecimal total = monthlyPrincipal.add(interest);
            remainingPrincipal = remainingPrincipal.subtract(monthlyPrincipal);

            result.add(new RepaymentPlanItem(i, monthlyPrincipal, interest, total));
        }
        return result;
    }

    private static List<RepaymentPlanItem> calculateInterestFirst(
            BigDecimal loanAmount, BigDecimal interestRate, Integer term) {
        // 先息后本：每月只还利息，最后一期还本金+利息
        BigDecimal monthlyRate = interestRate.divide(BigDecimal.valueOf(12), 6, BigDecimal.ROUND_HALF_UP);
        BigDecimal monthlyInterest = loanAmount.multiply(monthlyRate);
        BigDecimal lastPayment = loanAmount.add(monthlyInterest); // 最后一期还本金+当月利息

        List<RepaymentPlanItem> result = new ArrayList<>();
        for (int i = 1; i < term; i++) {
            result.add(new RepaymentPlanItem(i, BigDecimal.ZERO, monthlyInterest, monthlyInterest));
        }
        result.add(new RepaymentPlanItem(term, loanAmount, monthlyInterest, lastPayment));
        return result;
    }

    private static List<RepaymentPlanItem> calculateOneTimeRepay(
            BigDecimal loanAmount, BigDecimal interestRate, Integer term) {
        // 一次性还清：只有一期，到期还本金+全部利息
        BigDecimal totalInterest = loanAmount.multiply(interestRate).multiply(BigDecimal.valueOf(term)).divide(BigDecimal.valueOf(12), 2, BigDecimal.ROUND_HALF_UP);
        BigDecimal totalPayment = loanAmount.add(totalInterest);

        return List.of(new RepaymentPlanItem(1, loanAmount, totalInterest, totalPayment));
    }
}