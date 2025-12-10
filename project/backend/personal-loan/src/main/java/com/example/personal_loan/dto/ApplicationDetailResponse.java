package com.example.personal_loan.dto;

import com.example.personal_loan.entity.LoanApplication;
import com.example.personal_loan.entity.User;
import com.example.personal_loan.entity.UserCert;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 改成3个变量
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationDetailResponse {
    // 用户基本信息
    private User user;

    // 认证材料
    private UserCert userCert; 

    // 贷款申请信息
    private LoanApplication application;
}