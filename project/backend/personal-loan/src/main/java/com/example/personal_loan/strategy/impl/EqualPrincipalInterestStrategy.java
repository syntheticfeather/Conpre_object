package com.example.personal_loan.strategy.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.example.personal_loan.entity.RepaymentSchedule;
import com.example.personal_loan.enums.RepaidType;
import com.example.personal_loan.strategy.RepaymentStrategy;

@Component
public class EqualPrincipalInterestStrategy implements RepaymentStrategy {

    private static final int MONEY_SCALE = 2;
    private static final int RATE_SCALE = 18;
    private static final BigDecimal ONE = BigDecimal.ONE;

    @Override
    public List<RepaymentSchedule> calculate(
            BigDecimal loanAmount,
            BigDecimal monthlyRate,
            Integer term,
            LocalDate startDate) {

        List<RepaymentSchedule> result = new ArrayList<>();
        BigDecimal remainingPrincipal = loanAmount;

        BigDecimal powFactor = ONE.add(monthlyRate).pow(term);
        BigDecimal monthlyPayment = loanAmount
                .multiply(monthlyRate)
                .multiply(powFactor)
                .divide(powFactor.subtract(ONE), RATE_SCALE, RoundingMode.HALF_UP)
                .setScale(MONEY_SCALE, RoundingMode.HALF_UP);

        for (int i = 1; i <= term; i++) {
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

    @Override
    public String getType() {
        return RepaidType.等额本息.name();
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