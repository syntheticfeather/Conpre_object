package com.example.personal_loan.strategy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.example.personal_loan.entity.RepaymentSchedule;

public interface RepaymentStrategy {
    List<RepaymentSchedule> calculate(
            BigDecimal loanAmount,
            BigDecimal monthlyRate,
            Integer term,
            LocalDate startDate);

    String getType();
}