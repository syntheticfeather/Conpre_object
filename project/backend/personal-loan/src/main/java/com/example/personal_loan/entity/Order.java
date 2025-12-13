package com.example.personal_loan.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.personal_loan.enums.OrderStatus;
import com.example.personal_loan.enums.RepaidType;
import com.fasterxml.jackson.annotation.JsonFormat;

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

    private OrderStatus status; // "正常", "已逾期", "已完成"
    private BigDecimal repaidAmount;
    private BigDecimal loanAmount;
    private BigDecimal interestRate;

    private RepaidType repaidType; // "等额本息"、"等额本金"、"先息后本"、"一次性还清"
    private Integer loanPeriod;
    private Integer term;
    private Integer currentTerm;
    private String contract;  // 合同路径

    private Integer overdueDays;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
}
