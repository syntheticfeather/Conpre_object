package com.example.personal_loan.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import com.example.personal_loan.entity.Order;
import com.example.personal_loan.entity.RepaymentSchedule;
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
    public static List<RepaymentSchedule> calculateRepaymentPlan(
            BigDecimal loanAmount,
            BigDecimal interestRate,
            Integer term,
            RepaidType repaidType,
            LocalDate startDate) {

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
             return calculateEqualPrincipal(loanAmount, ZERO, term, startDate);
        }

        // 3. 将年利率转换为月利率 (保留高精度，不在这里四舍五入到2位)
        BigDecimal monthlyRate = interestRate.divide(BigDecimal.valueOf(12), RATE_SCALE, RoundingMode.HALF_UP);

        List<RepaymentSchedule> plan = new ArrayList<>();

        switch (repaidType) {
        case 等额本息:
            plan.addAll(calculateEqualPrincipalInterest(loanAmount, monthlyRate, term, startDate));
            break;
        case 等额本金:
            plan.addAll(calculateEqualPrincipal(loanAmount, monthlyRate, term, startDate));
            break;
        case 先息后本:
            plan.addAll(calculateInterestFirst(loanAmount, monthlyRate, term, startDate));
            break;
        case 一次性还本付息:
            plan.addAll(calculateOneTimeRepay(loanAmount, interestRate, term, startDate));
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
        List<RepaymentSchedule> plan = calculateRepaymentPlan(
            order.getLoanAmount(),
            order.getInterestRate(),
            order.getTerm(),
            order.getRepaidType(),
            order.getStartTime().toLocalDate()
        );
        return plan.get(order.getCurrentTerm()).getTotalAmount();
    }

    private static List<RepaymentSchedule> calculateEqualPrincipalInterest(
        BigDecimal loanAmount, BigDecimal monthlyRate, Integer term, LocalDate startDate) {

        List<RepaymentSchedule> result = new ArrayList<>();
        BigDecimal remainingPrincipal = loanAmount;

        // 公式：每月还款额 = [贷款本金 × 月利率 × (1+月利率)^还款月数] ÷ [(1+月利率)^还款月数－1]
        BigDecimal powFactor = ONE.add(monthlyRate).pow(term);
        BigDecimal monthlyPayment = loanAmount
                .multiply(monthlyRate)
                .multiply(powFactor)
                .divide(powFactor.subtract(ONE), RATE_SCALE, RoundingMode.HALF_UP)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        // 开始循环生成每一期的数据
        // i 是当前期数
        for (int i = 1; i <= term; i++) {
            // 1. 计算当期利息
            BigDecimal interest = remainingPrincipal.multiply(monthlyRate)
                    .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

            BigDecimal principal;
            if (i == term) {
                principal = remainingPrincipal.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            } else {
                principal = monthlyPayment.subtract(interest).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            }

            BigDecimal total = principal.add(interest).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            remainingPrincipal = remainingPrincipal.subtract(principal);
            BigDecimal remainingInterest = remainingPrincipal.multiply(monthlyRate)
                    .multiply(BigDecimal.valueOf(term - i))
                    .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

            LocalDate dueDate = calculateDueDate(startDate, i);

            RepaymentSchedule schedule = new RepaymentSchedule();
            schedule.setTerm(i);
            schedule.setPrincipal(principal);
            schedule.setInterest(interest);
            schedule.setTotalAmount(total);
            schedule.setStatus("未还");
            schedule.setRemainingPrincipal(remainingPrincipal);
            schedule.setRemainingInterest(remainingInterest);
            schedule.setDueDate(dueDate);
            result.add(schedule);
        }
        return result;
    }

    private static List<RepaymentSchedule> calculateEqualPrincipal(
        BigDecimal loanAmount, BigDecimal monthlyRate, Integer term, LocalDate startDate) {

        BigDecimal perPeriodPrincipal = loanAmount.divide(BigDecimal.valueOf(term), MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal remainingPrincipal = loanAmount;

        List<RepaymentSchedule> result = new ArrayList<>();
        for (int i = 1; i <= term; i++) {
            BigDecimal interest = remainingPrincipal.multiply(monthlyRate).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            BigDecimal currentPrincipal;
            if (i == term) {
                currentPrincipal = remainingPrincipal.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            } else {
                currentPrincipal = perPeriodPrincipal.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            }
            BigDecimal total = currentPrincipal.add(interest).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            remainingPrincipal = remainingPrincipal.subtract(currentPrincipal);
            int remainingTerms = term - i;
            BigDecimal remainingInterest = remainingPrincipal.multiply(monthlyRate)
                    .multiply(BigDecimal.valueOf(remainingTerms + 1))
                    .divide(BigDecimal.valueOf(2), MONEY_SCALE, RoundingMode.HALF_UP);

            LocalDate dueDate = calculateDueDate(startDate, i);

            RepaymentSchedule schedule = new RepaymentSchedule();
            schedule.setTerm(i);
            schedule.setPrincipal(currentPrincipal);
            schedule.setInterest(interest);
            schedule.setTotalAmount(total);
            schedule.setStatus("未还");
            schedule.setRemainingPrincipal(remainingPrincipal);
            schedule.setRemainingInterest(remainingInterest);
            schedule.setDueDate(dueDate);
            result.add(schedule);
        }
        return result;
    }

    private static List<RepaymentSchedule> calculateInterestFirst(
            BigDecimal loanAmount, BigDecimal monthlyRate, Integer term, LocalDate startDate) {

        List<RepaymentSchedule> result = new ArrayList<>();
        BigDecimal perPeriodInterest = loanAmount.multiply(monthlyRate)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal remainingPrincipal = loanAmount;

        for (int i = 1; i < term; i++) {
            BigDecimal currentRemainingPrincipal = remainingPrincipal;
            int remainingTerms = term - i;
            BigDecimal remainingInterest = currentRemainingPrincipal.multiply(monthlyRate)
                    .multiply(BigDecimal.valueOf(remainingTerms))
                    .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

            LocalDate dueDate = calculateDueDate(startDate, i);

            RepaymentSchedule schedule = new RepaymentSchedule();
            schedule.setTerm(i);
            schedule.setPrincipal(ZERO);
            schedule.setInterest(perPeriodInterest);
            schedule.setTotalAmount(perPeriodInterest);
            schedule.setStatus("未还");
            schedule.setRemainingPrincipal(currentRemainingPrincipal);
            schedule.setRemainingInterest(remainingInterest);
            schedule.setDueDate(dueDate);
            result.add(schedule);
        }

        remainingPrincipal = BigDecimal.ZERO;
        BigDecimal lastTotal = loanAmount.add(perPeriodInterest).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        LocalDate dueDate = calculateDueDate(startDate, term);

        RepaymentSchedule schedule = new RepaymentSchedule();
        schedule.setTerm(term);
        schedule.setPrincipal(loanAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        schedule.setInterest(perPeriodInterest);
        schedule.setTotalAmount(lastTotal);
        schedule.setStatus("未还");
        schedule.setRemainingPrincipal(remainingPrincipal);
        schedule.setRemainingInterest(BigDecimal.ZERO);
        schedule.setDueDate(dueDate);
        result.add(schedule);

        return result;
    }

    private static List<RepaymentSchedule> calculateOneTimeRepay(
            BigDecimal loanAmount, BigDecimal monthlyRate, Integer term, LocalDate startDate) {

        BigDecimal totalInterest = loanAmount.multiply(monthlyRate).multiply(BigDecimal.valueOf(term))
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        BigDecimal totalPayment = loanAmount.add(totalInterest).setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        LocalDate dueDate = calculateDueDate(startDate, 1);

        RepaymentSchedule schedule = new RepaymentSchedule();
        schedule.setTerm(1);
        schedule.setPrincipal(loanAmount.setScale(MONEY_SCALE, RoundingMode.HALF_UP));
        schedule.setInterest(totalInterest);
        schedule.setTotalAmount(totalPayment);
        schedule.setStatus("未还");
        schedule.setRemainingPrincipal(BigDecimal.ZERO);
        schedule.setRemainingInterest(BigDecimal.ZERO);
        schedule.setDueDate(dueDate);

        return List.of(schedule);
    }

    private static LocalDate calculateDueDate(LocalDate startDate, int term) {
        LocalDate targetDate = startDate.plusMonths(term);
        int dayOfMonth = startDate.getDayOfMonth();
        int lastDayOfMonth = targetDate.lengthOfMonth();

        if (dayOfMonth > lastDayOfMonth) {
            return targetDate.with(TemporalAdjusters.lastDayOfMonth());
        }

        return targetDate.withDayOfMonth(dayOfMonth);
    }

    public static Integer calculateRepaymentTermCount(Integer term, RepaidType repaidType) {
        if (RepaidType.一次性还本付息.equals(repaidType)) {
            return 1;
        }
        return term;
    }
}
