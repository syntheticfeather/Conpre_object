package com.example.personal_loan.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.example.personal_loan.entity.Order;
import com.example.personal_loan.entity.RepaymentSchedule;
import com.example.personal_loan.enums.OrderStatus;
import com.example.personal_loan.enums.RepaidType;
import com.example.personal_loan.strategy.RepaymentStrategy;

@Component
public class CalculateUtil {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final int RATE_SCALE = 18;

    private final Map<String, RepaymentStrategy> strategyMap;

    @Autowired
    public CalculateUtil(List<RepaymentStrategy> strategies) {
        this.strategyMap = new HashMap<>();
        for (RepaymentStrategy strategy : strategies) {
            strategyMap.put(strategy.getType(), strategy);
        }
    }

    public static Integer getTotalTransactionCount(List<Order> orders) {
        return (int) orders.stream()
            .filter(order -> OrderStatus.已完成.equals(order.getStatus()))
            .count();
    }

    public static BigDecimal getTotalLoanAmount(List<Order> orders) {
        return orders.stream()
            .map(Order::getLoanAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal getTotalRepaidAmount(List<Order> orders) {
        return orders.stream()
            .map(Order::getRepaidAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public List<RepaymentSchedule> calculateRepaymentPlan(
            BigDecimal loanAmount,
            BigDecimal interestRate,
            Integer term,
            RepaidType repaidType,
            LocalDate startDate) {

        // 参数校验
        if (loanAmount == null || interestRate == null || term == null || repaidType == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        if (loanAmount.compareTo(ZERO) <= 0 || term <= 0) {
            throw new IllegalArgumentException("本金和期数必须大于0");
        }
        if (interestRate.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException("利率不能为负");
        }

        // 获取策略
        RepaymentStrategy strategy = strategyMap.get(repaidType.name());
        if (strategy == null) {
            throw new IllegalArgumentException("不支持的还款方式：" + repaidType);
        }

        // 将年利率转换为月利率
        BigDecimal monthlyRate = interestRate.divide(BigDecimal.valueOf(12), RATE_SCALE, RoundingMode.HALF_UP);

        return strategy.calculate(loanAmount, monthlyRate, term, startDate);
    }

    public BigDecimal calculateCurrentTermPayment(Order order) {
        if (order == null) {
            throw new IllegalArgumentException("order不能为空");
        }
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

    public static Integer calculateRepaymentTermCount(Integer term, RepaidType repaidType) {
        if (RepaidType.一次性还本付息.equals(repaidType)) {
            return 1;
        }
        return term;
    }
}