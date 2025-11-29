package com.example.personal_loan.service;

import java.util.List;

import com.example.personal_loan.entity.LoanApplication;

public interface ManualApproveService {
    List<LoanApplication> getApproves(Long userId); // 获得本审核员所有需审核申请

    LoanApplication getApprove(Long userId,Long loanApplicationId); // 获得本审核员单个审核申请详情

    Boolean ManualCheck(); // 返回审核结果
}
