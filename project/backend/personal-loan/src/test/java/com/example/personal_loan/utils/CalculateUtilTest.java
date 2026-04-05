package com.example.personal_loan.utils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import com.example.personal_loan.entity.Order;
import com.example.personal_loan.enums.OrderStatus;
import com.example.personal_loan.enums.RepaidType;

class CalculateUtilTest {

    @Test
    void getTotalTransactionCount_shouldCountCompletedOrders() {
        Order completed = new Order();
        completed.setStatus(OrderStatus.已完成);

        Order normal = new Order();
        normal.setStatus(OrderStatus.正常);

        assertEquals(1, CalculateUtil.getTotalTransactionCount(List.of(completed, normal)));
    }

    @Test
    void calculateRepaymentTermCount_shouldReturnOneForOneTimeRepay() {
        Integer repaymentTerms = CalculateUtil.calculateRepaymentTermCount(
                12,
                RepaidType.一次性还本付息);

        assertEquals(1, repaymentTerms);
    }

    @Test
    void calculateCurrentTermPayment_shouldReturnFullAmountForOneTimeRepay() {
        Order order = new Order();
        order.setLoanAmount(new BigDecimal("12000.00"));
        order.setInterestRate(new BigDecimal("0.12"));
        order.setTerm(1);
        order.setCurrentTerm(0);
        order.setRepaidType(RepaidType.一次性还本付息);

        BigDecimal payment = CalculateUtil.calculateCurrentTermPayment(order);

        assertEquals(0, payment.compareTo(new BigDecimal("12120.00")));
    }

    @Test
    void calculateEqualPrincipal_shouldKeepPrincipalSumEqualToLoanAmount() {
        List<RepaymentPlanItem> plan = CalculateUtil.calculateRepaymentPlan(
                new BigDecimal("10000.00"),
                new BigDecimal("0.12"),
                3,
                RepaidType.等额本金);

        BigDecimal principalSum = plan.stream()
                .map(RepaymentPlanItem::getPrincipal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertEquals(0, principalSum.compareTo(new BigDecimal("10000.00")));
    }
}
