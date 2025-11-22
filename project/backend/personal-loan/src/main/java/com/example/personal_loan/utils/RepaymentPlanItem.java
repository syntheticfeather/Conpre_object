package com.example.personal_loan.utils;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RepaymentPlanItem {
    private Integer term;           // 期数
    private BigDecimal principal;   // 本期本金
    private BigDecimal interest;    // 本期利息
    private BigDecimal total;       // 本期总还款
}
