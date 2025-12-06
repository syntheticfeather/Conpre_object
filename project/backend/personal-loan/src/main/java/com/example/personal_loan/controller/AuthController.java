package com.example.personal_loan.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.personal_loan.dto.ApiResponse;
import com.example.personal_loan.dto.LoginRequest;
import com.example.personal_loan.dto.LoginResponse;
import com.example.personal_loan.dto.RegisterRequest;
import com.example.personal_loan.dto.RegisterResponse;
import com.example.personal_loan.service.AuthService;
import com.example.personal_loan.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid LoginRequest request) {
        log.info("login api success called");
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response,"登录成功"));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> userRegister(@RequestBody @Valid RegisterRequest request) {
        log.info("register api success called");
        return ResponseEntity.ok(ApiResponse.success(userService.userRegister(request),"注册成功"));
    }

    // --- 认证上传接口 ---

    @PostMapping("/immovables")
    public ResponseEntity<ApiResponse<Void>> immovablesAuth(
            HttpServletRequest request,
            @RequestPart(required = false) MultipartFile propertyFile,
            @RequestPart(required = false) MultipartFile carFile) {

        Long userId = (Long) request.getAttribute("userId");
        log.info("/api/auth/immovables success called with user {}",userId);
        authService.immovablesAuth(userId, propertyFile, carFile);
        return ResponseEntity.ok(ApiResponse.success((Void) null, "不动产认证提交成功"));
    }

    @PostMapping("/work")
    public ResponseEntity<ApiResponse<Void>> occupationAuth(
            HttpServletRequest request,
            @RequestPart MultipartFile employmentFile,
            @RequestPart MultipartFile salaryFile) {

        log.info("/api/auth/work success called");
        Long userId = (Long) request.getAttribute("userId");
        authService.occupationAuth(userId, employmentFile, salaryFile);
        return ResponseEntity.ok(ApiResponse.success((Void) null, "工作认证提交成功"));
    }

    @PostMapping("/third-party")
    public ResponseEntity<ApiResponse<Void>> thirdPartyAuth(
            HttpServletRequest request,
            @RequestPart MultipartFile socialSecurityFile,
            @RequestPart MultipartFile creditReportFile) {

        log.info("/api/auth/third-party success called");
        Long userId = (Long) request.getAttribute("userId");
        authService.thirdPartyAuth(userId, socialSecurityFile, creditReportFile);
        return ResponseEntity.ok(ApiResponse.success((Void) null, "第三方认证提交成功"));
    }

    @PostMapping("/bank-card")
    public ResponseEntity<ApiResponse<Void>> bankAccountAuth(
            HttpServletRequest request,
            @RequestParam String bankCardId) {

        log.info("/api/auth/bank-card success called");
        Long userId = (Long) request.getAttribute("userId");
        authService.bankAccountAuth(userId, bankCardId);
        return ResponseEntity.ok(ApiResponse.success((Void) null, "银行卡认证成功"));
    }

    @PostMapping("/id-card")
    public ResponseEntity<ApiResponse<Void>> idCardAuth(
            HttpServletRequest request,
            @RequestParam String idCard) {

        log.info("/api/auth/id-card success called");
        Long userId = (Long) request.getAttribute("userId");
        authService.idCardAuth(userId, idCard);
        return ResponseEntity.ok(ApiResponse.success((Void) null, "身份证认证成功"));
    }

    /**
     * 计算贷款分数
     */
    @GetMapping("/score")
    public ResponseEntity<ApiResponse<Integer>> calScore(HttpServletRequest request) {
        log.info("/api/auth/score success called");
        Long userId = (Long) request.getAttribute("userId");
        int score = authService.calScore(userId);
        return ResponseEntity.ok(ApiResponse.success(score));
    }
}
