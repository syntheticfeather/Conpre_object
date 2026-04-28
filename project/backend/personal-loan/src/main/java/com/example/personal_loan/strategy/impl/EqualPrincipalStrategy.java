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
public class EqualPrincipalStrategy implements RepaymentStrategy {

    private static final int MONEY_SCALE = 2;

    @Override
    public List<RepaymentSchedule> calculate(
            BigDecimal loanAmount,
            BigDecimal monthlyRate,
            Integer term,
            LocalDate startDate) {

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

    @Override
    public String getType() {
        return RepaidType.等额本金.name();
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