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

import com.example.personal_loan.dto.ApiResult;
import com.example.personal_loan.dto.ApplicationDetailResponse;
import com.example.personal_loan.dto.ManualCheckRequest;
import com.example.personal_loan.dto.ManualCheckResponse;
import com.example.personal_loan.dto.PendingApprovalResponse;
import com.example.personal_loan.entity.PostponeRequest;
import com.example.personal_loan.service.ManualApproveService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;


@RestController
@RequestMapping("/api/approval")
@Slf4j
@Tag(name = "审批管理", description = "贷款审批相关接口")
public class ManualApproveController {

    @Autowired
    private ManualApproveService manualApproveService;

    @GetMapping(value = "/pending", produces = "application/json")
    @Operation(summary = "获取待审批列表", description = "管理员获取待审批的贷款申请列表")
    public ResponseEntity<ApiResult<List<PendingApprovalResponse>>> getPendingApprovals(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        log.info("/api/approval/pending success called for admin {} to get pending approvals list", userId);
        List<PendingApprovalResponse> list = manualApproveService.getApproves(userId);
        return ResponseEntity.ok(ApiResult.success(list));
    }

    @GetMapping(value = "/completed", produces = "application/json")
    @Operation(summary = "获取已完成审批列表", description = "管理员获取已完成审批的贷款申请列表")
    public ResponseEntity<ApiResult<List<PendingApprovalResponse>>> getCompletedApprovals(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        log.info("/api/approval/completed success called for admin {} to get completed approvals list", userId);
        List<PendingApprovalResponse> list = manualApproveService.completedApproves(userId);
        return ResponseEntity.ok(ApiResult.success(list));
    }

    @GetMapping(value = "/detail/{loanApplicationId}", produces = "application/json")
    @Operation(summary = "获取申请详情", description = "管理员获取贷款申请的详细信息")
    public ResponseEntity<ApiResult<ApplicationDetailResponse>> getApplicationDetail(
            HttpServletRequest request,
            @Parameter(description = "申请ID") @PathVariable Long loanApplicationId) {

        log.info("/api/approval/detail/{} success called for admin to get applicaton {} details", loanApplicationId, loanApplicationId);
        Long userId = (Long) request.getAttribute("userId");
        ApplicationDetailResponse detail = manualApproveService.getApprove(userId, loanApplicationId);
        return ResponseEntity.ok(ApiResult.success(detail));
    }

    @PostMapping(value = "/check", produces = "application/json")
    @Operation(summary = "审批操作", description = "管理员对贷款申请进行审批操作（批准/拒绝）")
    public ResponseEntity<ApiResult<ManualCheckResponse>> manualCheck(@Parameter(description = "审批请求") @RequestBody ManualCheckRequest request) {
        log.info("/api/approval/check success called for admin to check application");
        ManualCheckResponse response = manualApproveService.manualCheck(request.getLoanApplicationId(), request.getApproved(), request.getManualRejectReason());
        return ResponseEntity.ok(ApiResult.success(response,"操作成功"));
    }

    @GetMapping(value = "/postpone/pending", produces = "application/json")
    @Operation(summary = "获取待审核延期申请", description = "管理员获取所有待审核的延期申请")
    public ResponseEntity<ApiResult<List<PostponeRequest>>> getPendingPostponeRequests(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        log.info("/api/approval/postpone/pending success called for admin {} to get pending postpone requests", userId);
        List<PostponeRequest> requests = manualApproveService.getPendingPostponeRequests();
        return ResponseEntity.ok(ApiResult.success(requests));
    }

    @GetMapping(value = "/postpone/{requestId}", produces = "application/json")
    @Operation(summary = "获取延期申请详情", description = "管理员获取指定延期申请的详细信息")
    public ResponseEntity<ApiResult<PostponeRequest>> getPostponeRequest(
            HttpServletRequest request,
            @Parameter(description = "申请ID") @PathVariable Long requestId) {
        Long userId = (Long) request.getAttribute("userId");
        log.info("/api/approval/postpone/{} success called for admin {} to get postpone request details", requestId, userId);
        PostponeRequest postponeRequest = manualApproveService.getPostponeRequest(requestId);
        return ResponseEntity.ok(ApiResult.success(postponeRequest));
    }

    @PostMapping(value = "/postpone/{requestId}/approve", produces = "application/json")
    @Operation(summary = "审核通过延期申请", description = "管理员审核通过指定延期申请")
    public ResponseEntity<ApiResult<String>> approvePostpone(
            HttpServletRequest request,
            @Parameter(description = "申请ID") @PathVariable Long requestId) {
        Long userId = (Long) request.getAttribute("userId");
        log.info("/api/approval/postpone/{}/approve success called for admin {} to approve postpone request", requestId, userId);
        manualApproveService.approvePostpone(requestId);
        return ResponseEntity.ok(ApiResult.success("延期申请已通过"));
    }

    @PostMapping(value = "/postpone/{requestId}/reject", produces = "application/json")
    @Operation(summary = "审核拒绝延期申请", description = "管理员审核拒绝指定延期申请")
    public ResponseEntity<ApiResult<String>> rejectPostpone(
            HttpServletRequest request,
            @Parameter(description = "申请ID") @PathVariable Long requestId,
            @RequestBody(required = false) String reason) {
        Long userId = (Long) request.getAttribute("userId");
        log.info("/api/approval/postpone/{}/reject success called for admin {} to reject postpone request", requestId, userId);
        manualApproveService.rejectPostpone(requestId, reason);
        return ResponseEntity.ok(ApiResult.success("延期申请已拒绝"));
    }
}
