package com.example.personal_loan.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.personal_loan.dto.AdminGetAppResponse;
import com.example.personal_loan.dto.ApiResult;
import com.example.personal_loan.dto.ApplicationRequest;
import com.example.personal_loan.dto.UserGetAppResponse;
import com.example.personal_loan.service.ApplicationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/loan-applications")
@Slf4j
@Tag(name = "贷款申请管理", description = "贷款申请相关接口")
public class ApplicationController {
    @Autowired
    private ApplicationService applicationService;

    // ==================== 用户端 ====================
    /**
     * 用户提交贷款申请
     */
    @PostMapping(produces = "application/json")
    @Operation(summary = "提交贷款申请", description = "用户提交贷款申请")
    public ResponseEntity<ApiResult<String>> apply(
            HttpServletRequest request,
            @Parameter(description = "贷款申请信息") @RequestBody @Valid ApplicationRequest applicationRequest) {
        
        Long userId = (Long) request.getAttribute("userId");
        log.info("/api/loan-applications success called for User {} submitting loan application for option {}", 
                 userId, applicationRequest.getOptionId());
        
        applicationService.addApplication(userId, applicationRequest);
        return ResponseEntity.ok(ApiResult.success("Application submitted successfully, please wait for review"));
    }

    /**
     * 用户查看单个申请
     */
    @GetMapping(value = "/my/{applicationId}", produces = "application/json")
    @Operation(summary = "查看单个申请", description = "用户查看自己的单个贷款申请详情")
    public ResponseEntity<ApiResult<UserGetAppResponse>> userGetApplication(
            HttpServletRequest request,
            @Parameter(description = "申请ID") @PathVariable Long applicationId) {

        Long userId = (Long) request.getAttribute("userId");
        log.info("/api/loan-applications/my/{} success called for User {} get his application {}", applicationId, userId, applicationId);
        UserGetAppResponse response = applicationService.userGetApplication(userId, applicationId);
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * 用户查看所有申请
     */
    @GetMapping(value = "/my", produces = "application/json")
    @Operation(summary = "查看所有申请", description = "用户查看自己的所有贷款申请")
    public ResponseEntity<ApiResult<List<UserGetAppResponse>>> getUserAllApplications(
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        log.info("/api/loan-applications/my success called for User {} to get all applications", userId);

        List<UserGetAppResponse> responses = applicationService.userGetAllApplications(userId);
        return ResponseEntity.ok(ApiResult.success(responses));
    }

    /**
     * 用户撤回申请
     */
    @PostMapping(value = "/my/{applicationId}/withdraw", produces = "application/json")
    @Operation(summary = "撤回申请", description = "用户撤回自己的贷款申请")
    public ResponseEntity<ApiResult<Void>> withdrawApplication(
            HttpServletRequest request,
            @Parameter(description = "申请ID") @PathVariable Long applicationId) {

        Long userId = (Long) request.getAttribute("userId");
        log.info("/api/loan-applications/my/{}/withdraw success called for User {} to withdraw application {}", applicationId, userId, applicationId);

        applicationService.withdrawApplication(userId, applicationId);
        return ResponseEntity.ok(ApiResult.success(null));
    }

    // ==================== 管理员端 ====================

    /**
     * 管理员获取任意用户的单个贷款申请详情
     */
    @GetMapping(value = "/{applicationId}", produces = "application/json")
    @Operation(summary = "获取申请详情", description = "管理员获取任意用户的单个贷款申请详情")
    public ResponseEntity<ApiResult<AdminGetAppResponse>> getAdminApplication(
            @Parameter(description = "申请ID") @PathVariable Long applicationId) {

        log.info("/api/loan-applications/{} success called for admin to get application with applicationId {}", applicationId, applicationId);

        AdminGetAppResponse response = applicationService.adminGetApplication(applicationId);
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * 管理员获取指定用户的所有贷款申请详情
     */
    @GetMapping(value = "/user/{userId}", produces = "application/json")
    @Operation(summary = "获取用户申请列表", description = "管理员获取指定用户的所有贷款申请详情")
    public ResponseEntity<ApiResult<List<AdminGetAppResponse>>> getAdminAllApplications(
            @Parameter(description = "用户ID") @PathVariable Long userId) {

        log.info("/api/loan-applications/user/{} success called for admin to get all applications of user {} ", userId,userId);

        List<AdminGetAppResponse> responses = applicationService.adminGetAllApplications(userId);
        return ResponseEntity.ok(ApiResult.success(responses));
    }

}
