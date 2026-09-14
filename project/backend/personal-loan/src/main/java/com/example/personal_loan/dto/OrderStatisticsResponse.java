package com.example.personal_loan.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户订单统计响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatisticsResponse {
    /** 待还总额（本金 + 利息） */
    private BigDecimal totalOutstandingAmount;
    /** 待还本金 */
    private BigDecimal totalOutstandingPrincipal;
    /** 待还利息 */
    private BigDecimal totalOutstandingInterest;
}
