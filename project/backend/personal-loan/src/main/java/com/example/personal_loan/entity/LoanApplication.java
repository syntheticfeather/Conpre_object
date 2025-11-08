package com.example.personal_loan.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplication {
    private Long id;
    private Long userId;
    private Long productId;
    private BigDecimal loanAmount;
    private Integer period;
    private String repaidType; // 枚举值字符串
    private BigDecimal interestRate;
    private String status; // "SUBMITTED", "UNDER_REVIEW", ...
    private String rejectReason;
    private LocalDateTime applyTime;
    private LocalDateTime reviewTime;

}