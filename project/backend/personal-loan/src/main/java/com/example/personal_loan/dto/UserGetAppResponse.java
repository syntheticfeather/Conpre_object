package com.example.personal_loan.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.personal_loan.enums.ApplicationStatus;
import com.example.personal_loan.enums.RepaidType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserGetAppResponse {
    private Long applicationId;      // 用于撤回操作
    
    private String productName;
    private BigDecimal loanAmount;
    private BigDecimal interestRate;
    private Integer loanPeriod;      // 期限（月）
    private Integer term;
    private RepaidType repaidType;   // 还款方式
    private ApplicationStatus status; 

    private LocalDateTime applyTime;
    
    // 通过时间，状态为APPROVED时返回
    private LocalDateTime reviewTime;

    // 可选：拒绝原因（仅当状态为拒绝时返回）
    private String rejectReason;
}
