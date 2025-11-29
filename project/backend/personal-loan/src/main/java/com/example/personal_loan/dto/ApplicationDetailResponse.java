package com.example.personal_loan.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationDetailResponse {
    // 用户基本信息
    private String userName;  
    private String phone; 
    private LocalDateTime createTime; 

    // 认证材料
    private String idCard; 
    private Integer workCertId;  
    private Integer triCertId; 
    private Integer immovableCertId;  
    private Integer creditScore; 

    // 贷款申请信息
    private String productName; 
    private BigDecimal loanAmount; 
    private Integer loanPeriod; 
    private Integer term;  
}