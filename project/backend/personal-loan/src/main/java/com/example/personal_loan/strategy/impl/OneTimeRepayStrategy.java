package com.example.personal_loan.strategy.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.personal_loan.entity.RepaymentSchedule;
import com.example.personal_loan.enums.RepaidType;
import com.example.personal_loan.strategy.RepaymentStrategy;

@Component
public class OneTimeRepayStrategy implements RepaymentStrategy {

    private static final int MONEY_SCALE = 2;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);

    @Override
    public List<RepaymentSchedule> calculate(
            BigDecimal loanAmount,
            BigDecimal monthlyRate,
            Integer term,
            LocalDate startDate) {

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
        schedule.setRemainingPrincipal(ZERO);
        schedule.setRemainingInterest(ZERO);
        schedule.setDueDate(dueDate);

        return List.of(schedule);
    }

    @Override
    public String getType() {
        return RepaidType.一次性还本付息.name();
    }

    private LocalDate calculateDueDate(LocalDate startDate, int term) {
        LocalDate targetDate = startDate.plusMonths(term);
        int dayOfMonth = startDate.getDayOfMonth();
        int lastDayOfMonth = targetDate.lengthOfMonth();
        if (dayOfMonth > lastDayOfMonth) {
            return targetDate.with(TemporalAdjusters.lastDayOfMonth());
        }
        return targetDate.withDayOfMonth(dayOfMonth);
    }
}