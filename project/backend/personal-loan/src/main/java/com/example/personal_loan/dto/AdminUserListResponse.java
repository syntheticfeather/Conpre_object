package com.example.personal_loan.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserListResponse {
    private Long userId;
    private String userName;
    private String loanStatus;           // "正常" / "逾期" / "无借贷"
    private Integer totalTransactionCount; // 总交易次数
    private BigDecimal totalLoanAmount;  // 总借贷金额
    private BigDecimal totalRepaidAmount;  // 总还款金额
}
