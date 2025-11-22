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
public class AdminGetAppResponse {
    private Long id;                      // 主键，用于数据库操作
    private Long userId;                  // 关联用户，可查用户详情
    private Long productId;               // 关联产品，可查产品配置
    private String userName;              // 用户名（来自 user 表）
    private String phoneNumber;           // 手机号（可选）
    private String productName;           // 产品名（来自 product 表）
    private BigDecimal loanAmount;
    private BigDecimal interestRate;
    private Integer loanPeriod;
    private Integer term;
    private RepaidType repaidType;
    private ApplicationStatus status;
    private LocalDateTime applyTime;
    private LocalDateTime reviewTime;
    private String rejectReason;
}
