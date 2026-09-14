package com.example.personal_loan.dto;

import java.math.BigDecimal;

import com.example.personal_loan.entity.Order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserGetOrderResponse {
    private String productName;
    private Order order;

    /** 应还总额（该订单所有期数应还金额之和） */
    private BigDecimal totalAmountDue;
    /** 待还总额（未还期数的本金+利息之和） */
    private BigDecimal outstandingAmount;
    /** 待还本金（未还期数的本金之和） */
    private BigDecimal outstandingPrincipal;
    /** 待还利息（未还期数的利息之和） */
    private BigDecimal outstandingInterest;
}
