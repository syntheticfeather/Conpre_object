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

import com.example.personal_loan.dto.AdminGetAppResponse;
import com.example.personal_loan.dto.ApiResponse;
import com.example.personal_loan.dto.ApplicationRequest;
import com.example.personal_loan.dto.UserGetAppResponse;
import com.example.personal_loan.service.ApplicationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/loan-applications")
@Slf4j
public class ApplicationController {
    @Autowired
    private ApplicationService applicationService;

    // ==================== 用户端 ====================
    /**
     * 用户提交贷款申请
     */
    @PostMapping
    public ResponseEntity<ApiResponse<String>> apply(
            HttpServletRequest request,
            @RequestBody @Valid ApplicationRequest applicationRequest) {
        
        Long userId = (Long) request.getAttribute("userId");
        log.info("/api/loan-applications success called for User {} submitting loan application for option {}", 
                 userId, applicationRequest.getOptionId());
        
        applicationService.addApplication(userId, applicationRequest);
        return ResponseEntity.ok(ApiResponse.success("Application submitted successfully, please wait for review"));
    }

    /**
     * 用户查看单个申请
     */
    @GetMapping("/my/{applicationId}")
    public ResponseEntity<ApiResponse<UserGetAppResponse>> userGetApplication(
            HttpServletRequest request,
            @PathVariable Long applicationId) {

        Long userId = (Long) request.getAttribute("userId");
        log.info("/api/loan-applications/my/{} success called for User {} get his application {}", applicationId, userId, applicationId);
        UserGetAppResponse response = applicationService.userGetApplication(userId, applicationId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 用户查看所有申请
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<UserGetAppResponse>>> getUserAllApplications(
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        log.info("/api/loan-applications/my success called for User {} to get all applications", userId);

        List<UserGetAppResponse> responses = applicationService.userGetAllApplications(userId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * 用户撤回申请
     */
    @PostMapping("/my/{applicationId}/withdraw")
    public ResponseEntity<ApiResponse<Void>> withdrawApplication(
            HttpServletRequest request,
            @PathVariable Long applicationId) {

        Long userId = (Long) request.getAttribute("userId");
        log.info("/api/loan-applications/my/{}/withdraw success called for User {} to withdraw application {}", applicationId, userId, applicationId);

        applicationService.withdrawApplication(userId, applicationId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ==================== 管理员端 ====================

    /**
     * 管理员获取任意用户的单个贷款申请详情
     */
    @GetMapping("/{applicationId}")
    public ResponseEntity<ApiResponse<AdminGetAppResponse>> getAdminApplication(
            @PathVariable Long applicationId) {

        log.info("/api/loan-applications/{} success called for admin to get application with applicationId {}", applicationId, applicationId);

        AdminGetAppResponse response = applicationService.adminGetApplication(applicationId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 管理员获取指定用户的所有贷款申请详情
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<AdminGetAppResponse>>> getAdminAllApplications(
            @PathVariable Long userId) {

        log.info("/api/loan-applications/user/{} success called for admin to get all applications of user {} ", userId,userId);

        List<AdminGetAppResponse> responses = applicationService.adminGetAllApplications(userId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

}
