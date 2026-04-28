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
public class InterestFirstStrategy implements RepaymentStrategy {

    private static final int MONEY_SCALE = 2;
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(MONEY_SCALE, RoundingMode.HALF_UP);

    @Override
    public List<RepaymentSchedule> calculate(
            BigDecimal loanAmount,
            BigDecimal monthlyRate,
            Integer term,
            LocalDate startDate) {

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

    @Override
    public String getType() {
        return RepaidType.先息后本.name();
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