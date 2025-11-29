package com.example.personal_loan.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PendingApprovalResponse {
    private Long applicationId;
    private String userName;
    private String productName;
    private BigDecimal loanAmount;
    private Integer loanPeriod;
    private Integer term;
    private LocalDateTime applyTime;
}