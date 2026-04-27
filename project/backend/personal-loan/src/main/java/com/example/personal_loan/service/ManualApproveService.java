package com.example.personal_loan.service;

import java.util.List;

import com.example.personal_loan.dto.ApplicationDetailResponse;
import com.example.personal_loan.dto.ManualCheckResponse;
import com.example.personal_loan.dto.PendingApprovalResponse;
import com.example.personal_loan.entity.PostponeRequest;

public interface ManualApproveService {
    List<PendingApprovalResponse> getApproves(Long userId);

    ApplicationDetailResponse getApprove(Long userId,Long loanApplicationId);

    ManualCheckResponse manualCheck(Long loanApplicationId, Boolean approved, String manualRejectReason);

    List<PendingApprovalResponse> completedApproves(Long userId);

    List<PostponeRequest> getPendingPostponeRequests();

    PostponeRequest getPostponeRequest(Long requestId);

    void approvePostpone(Long requestId);

    void rejectPostpone(Long requestId, String reason);

}
