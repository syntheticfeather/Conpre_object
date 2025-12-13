package com.example.personal_loan.service;

import java.util.List;

import com.example.personal_loan.dto.ApplicationDetailResponse;
import com.example.personal_loan.dto.ManualCheckResponse;
import com.example.personal_loan.dto.PendingApprovalResponse;

public interface ManualApproveService {
    List<PendingApprovalResponse> getApproves(Long userId); // 获得本审核员所有需审核申请

    ApplicationDetailResponse getApprove(Long userId,Long loanApplicationId); // 获得本审核员单个审核申请详情

    ManualCheckResponse manualCheck(Long loanApplicationId, Boolean approved, String manualRejectReason); // 返回审核结果

    List<PendingApprovalResponse> completedApproves(Long userId); // 已办审核列表

}
