package com.example.personal_loan.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.personal_loan.dto.ApiResponse;
import com.example.personal_loan.dto.ApplicationDetailResponse;
import com.example.personal_loan.dto.ManualCheckRequest;
import com.example.personal_loan.dto.PendingApprovalResponse;
import com.example.personal_loan.service.ManualApproveService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;


@RestController
@RequestMapping("/approval")
@Slf4j
public class ManualApproveController {

    @Autowired
    private ManualApproveService manualApproveService;

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<PendingApprovalResponse>>> getPendingApprovals(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        log.info("/api/approval/pending success called for admin {} to get pending approvals list", userId);
        List<PendingApprovalResponse> list = manualApproveService.getApproves(userId);
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @GetMapping("/detail/{loanApplicationId}")
    public ResponseEntity<ApiResponse<ApplicationDetailResponse>> getApplicationDetail(
            HttpServletRequest request,
            @PathVariable Long loanApplicationId) {

        log.info("/api/approval/detail/{} success called for admin to get applicaton {} details", loanApplicationId, loanApplicationId);
        Long userId = (Long) request.getAttribute("userId");
        ApplicationDetailResponse detail = manualApproveService.getApprove(userId, loanApplicationId);
        return ResponseEntity.ok(ApiResponse.success(detail));
    }

    @PostMapping("/check")
    public ResponseEntity<ApiResponse<String>> manualCheck(@RequestBody ManualCheckRequest request) {
        log.info("/api/approval/check success called for admin to check application");
        Boolean result = manualApproveService.manualCheck(request.getLoanApplicationId(), request.getApproved());
        return ResponseEntity.ok(ApiResponse.success(null,"操作成功"));
    }
}