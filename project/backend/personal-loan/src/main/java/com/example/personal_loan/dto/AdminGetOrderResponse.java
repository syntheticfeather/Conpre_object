package com.example.personal_loan.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.personal_loan.enums.OrderStatus;
import com.example.personal_loan.enums.RepaidType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminGetOrderResponse {
    // 订单基础信息
    private Long orderId;
    private Long userId;

    private String productName;

    // 贷款信息
    private BigDecimal repaidAmount;
    private BigDecimal loanAmount;
    private BigDecimal interestRate;
    private Integer loanPeriod;
    private Integer term;
    private RepaidType repaidType;
    private LocalDateTime startTime;
    
    // 状态
    private OrderStatus status;
    private Integer overdueDays;
    
    // 还款进度
    private Integer currentTerm;
    
    // 合同（管理员可直接访问原始路径或下载链接）
    private String contractUrl;
}
