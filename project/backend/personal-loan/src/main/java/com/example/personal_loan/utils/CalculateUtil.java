package com.example.personal_loan.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.example.personal_loan.entity.Order;
import com.example.personal_loan.enums.OrderStatus;
import com.example.personal_loan.enums.RepaidType;


public final class CalculateUtil {

    // 精度：2位小数，四舍五入
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal ONE = BigDecimal.ONE;

    // 金额精度：2位
    private static final int MONEY_SCALE = 2;
    // 利率精度：建议至少18位，防止中间计算精度丢失
    private static final int RATE_SCALE = 18;

    private CalculateUtil() {
        // 工具类禁止实例化
    }

    /**
     * 计算总交易笔数：状态为 SETTLED 的订单数量
     */
    public static Integer getTotalTransactionCount(List<Order> orders) {
        return (int) orders.stream()
            .filter(order -> OrderStatus.已完成.equals(order.getStatus()))
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
     * 根据还款方式和参数，计算还款计划
     *
     * @param loanAmount 贷款本金
     * @param interestRate 年化利率（如 6.5% → 0.065）
     * @param term 总期数（还款次数）
     * @param repaidType 还款方式
     * @return 每期还款明细列表（含本金、利息、总金额）
     */
    public static List<RepaymentPlanItem> calculateRepaymentPlan(
            BigDecimal loanAmount,
            BigDecimal interestRate,
            Integer term,
            RepaidType repaidType) {

        // 1. 参数校验
        if (loanAmount == null || interestRate == null || term == null || repaidType == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        if (loanAmount.compareTo(ZERO) <= 0 || term <= 0) {
            throw new IllegalArgumentException("本金和期数必须大于0");
        }
        if (interestRate.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException("利率不能为负");
        }
        // 2. 利率处理：如果是0利率，直接简化处理
        if (interestRate.compareTo(ZERO) == 0) {
             // 0利率时，所有方式本质上都是等额本金（只还本金）
             return calculateEqualPrincipal(loanAmount, ZERO, term);
        }

        // 3. 将年利率转换为月利率 (保留高精度，不在这里四舍五入到2位)
        BigDecimal monthlyRate = interestRate.divide(BigDecimal.valueOf(12), RATE_SCALE, RoundingMode.HALF_UP);

        List<RepaymentPlanItem> plan = new ArrayList<>();

        switch (repaidType) {
        case 等额本息:
            plan.addAll(calculateEqualPrincipalInterest(loanAmount, monthlyRate, term));
            break;
        case 等额本金:
            plan.addAll(calculateEqualPrincipal(loanAmount, monthlyRate, term));
            break;
        case 先息后本:
            plan.addAll(calculateInterestFirst(loanAmount, monthlyRate, term));
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
        if (order == null) {
            throw new IllegalArgumentException("order不能为空");
        }
        // 如果已经还完所有期数，返回0
        if (order.getCurrentTerm() >= order.getTerm()) {
            return BigDecimal.ZERO;
        }
        // 获取还款计划
        List<RepaymentPlanItem> plan = CalculateUtil.calculateRepaymentPlan(
            order.getLoanAmount(),
            order.getInterestRate(),
            order.getTerm(),
            order.getRepaidType()
        );
        // 返回下一期应还金额
        return plan.get(order.getCurrentTerm()).getTotal();
    }

    // ==================== 具体还款方式实现 ====================

    /**
     * 计算当期等额本息还款计划
     *
     * @param loanAmount 贷款本金
     * @param monthlyRate 月利率
     * @param term 贷款期数
     * @return 每期还款明细列表（含本金、利息、总金额）
     */
    private static List<RepaymentPlanItem> calculateEqualPrincipalInterest(
        BigDecimal loanAmount, BigDecimal monthlyRate, Integer term) {

        List<RepaymentPlanItem> result = new ArrayList<>();
        BigDecimal remainingPrincipal = loanAmount;

        // 公式：每月还款额 = [贷款本金 × 月利率 × (1+月利率)^还款月数] ÷ [(1+月利率)^还款月数－1]
        BigDecimal powFactor = ONE.add(monthlyRate).pow(term);
        BigDecimal monthlyPayment = loanAmount
                .multiply(monthlyRate)
                .multiply(powFactor)
                .divide(powFactor.subtract(ONE), RATE_SCALE, RoundingMode.HALF_UP) // 计算月供时保留高精度
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP); // 最后再转为金额精度

        // 开始循环生成每一期的数据
        // i 是当前期数
        for (int i = 1; i <= term; i++) {
            // 1. 计算当期利息
            BigDecimal interest = remainingPrincipal.multiply(monthlyRate)
                    .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

            // 2. 计算当期本金
            BigDecimal principal;
            if (i == term) {
                // 【关键修正】最后一期本金 = 剩余所有本金，确保贷款能还清
                principal = remainingPrincipal.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            } else {
                principal = monthlyPayment.subtract(interest).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            }

            // 3. 计算总额
            BigDecimal total = principal.add(interest).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

            // 4. 更新剩余本金
            remainingPrincipal = remainingPrincipal.subtract(principal);

            result.add(new RepaymentPlanItem(i, principal, interest, total));
        }
        return result;
    }

    /**
     * 计算等额本金还款计划
     *
     * @param loanAmount 贷款本金
     * @param monthlyRate 月利率
     * @param term 贷款期数
     * @return 每期还款明细列表（含本金、利息、总金额）
     */
    private static List<RepaymentPlanItem> calculateEqualPrincipal(
        BigDecimal loanAmount, BigDecimal monthlyRate, Integer term) {

        // 每期应还本金 = 总本金 / 期数
        BigDecimal perPeriodPrincipal = loanAmount.divide(BigDecimal.valueOf(term), MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal remainingPrincipal = loanAmount;

        List<RepaymentPlanItem> result = new ArrayList<>();
        for (int i = 1; i <= term; i++) {
            // 1. 计算当期利息
            BigDecimal interest = remainingPrincipal.multiply(monthlyRate).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            // 2. 计算当期本金
            BigDecimal currentPrincipal;
            if (i == term) {
                // 【关键修正】最后一期还完所有剩余本金
                currentPrincipal = remainingPrincipal.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            } else {
                currentPrincipal = perPeriodPrincipal.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            }
            // 3. 计算总额
            BigDecimal total = currentPrincipal.add(interest).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            // 4. 更新剩余本金
            remainingPrincipal = remainingPrincipal.subtract(currentPrincipal);

            result.add(new RepaymentPlanItem(i, currentPrincipal, interest, total));
        }
        return result;
    }

    /**
     * 计算先息后本还款计划
     *
     * @param loanAmount 贷款本金
     * @param monthlyRate 月利率
     * @param term 贷款期数
     * @return 每期还款明细列表（含本金、利息、总金额）
     */
    private static List<RepaymentPlanItem> calculateInterestFirst(
            BigDecimal loanAmount, BigDecimal monthlyRate, Integer term) {
        
        List<RepaymentPlanItem> result = new ArrayList<>();
        // 每期利息 = 本金 * 月利率
        BigDecimal perPeriodInterest = loanAmount.multiply(monthlyRate)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        for (int i = 1; i < term; i++) {
            // 前 N-1 期只还利息
            result.add(new RepaymentPlanItem(i, ZERO, perPeriodInterest, perPeriodInterest));
        }
        
        // 最后一期：还本金 + 最后一期利息
        BigDecimal lastTotal = loanAmount.add(perPeriodInterest).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        result.add(new RepaymentPlanItem(term, loanAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP), perPeriodInterest, lastTotal));
        
        return result;
    }

    /**
     * 计算一次性还本付息还款计划
     *
     * @param loanAmount 贷款本金
     * @param monthlyRate 月利率
     * @param term 贷款期数
     * @return 每期还款明细列表（含本金、利息、总金额）
     */
    private static List<RepaymentPlanItem> calculateOneTimeRepay(
            BigDecimal loanAmount, BigDecimal monthlyRate, Integer term) {
        
        // 1. 计算总利息
        // 公式：总利息 = 本金 × 月利率 × 月数
        // 因为是一次性还本付息，通常按单利计算
        BigDecimal totalInterest = loanAmount.multiply(monthlyRate).multiply(BigDecimal.valueOf(term))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        
        // 2. 计算到期还款总额
        BigDecimal totalPayment = loanAmount.add(totalInterest).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        // 3. 返回结果
        // 只有 1 笔交易，发生在第 1 期（即到期那一期的期末）
        return List.of(new RepaymentPlanItem(1, loanAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP), totalInterest, totalPayment));
    }

    /**
     * 计算还款期数
     *
     * @param term 总期数（还款次数）
     * @param repaidType 还款方式
     * @return 还款期数
     */
    public static Integer calculateRepaymentTermCount( Integer term, RepaidType repaidType) {
        // 对于一次性还本付息，还款次数为1
        if (RepaidType.一次性还本付息.equals(repaidType)) {
            return 1;
        }
        // 其他还款方式返回传入的term
        return term;
    }
}
