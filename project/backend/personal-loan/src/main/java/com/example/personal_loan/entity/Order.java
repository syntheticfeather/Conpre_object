package com.example.personal_loan.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.personal_loan.enums.OrderStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Order {
    private Long id;
    private Long userId;
    private Long productId;
    private Long applicationId; // 关联申请

    private BigDecimal repaidAmount;
    private BigDecimal outstandingAmount;
    private BigDecimal interestRate;

    private String repaidType;
    private OrderStatus status; // "NORMAL", "OVERDUE", "SETTLED"

    private String contract;  // 合同路径

    private Integer loanPeriod;
    private Integer currentTerm;
    private Integer overdueDays;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

}
