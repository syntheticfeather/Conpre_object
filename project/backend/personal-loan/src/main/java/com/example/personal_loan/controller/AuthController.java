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

    @PostMapping("/submit-all")
    public ResponseEntity<ApiResponse<Void>> submitAllAuth(
            HttpServletRequest request,
            @RequestParam String idCard,
            @RequestParam String bankCardId,
            @RequestPart(required=false) MultipartFile propertyFile,
            @RequestPart(required=false) MultipartFile carFile,
            @RequestPart(required=false) MultipartFile employmentFile,
            @RequestPart(required=false) MultipartFile salaryFile,
            @RequestPart(required=false) MultipartFile socialSecurityFile,
            @RequestPart(required=false) MultipartFile creditReportFile) {

        Long userId = (Long) request.getAttribute("userId");
        log.info("/api/auth/submit-all success called for user {} to authorize", userId);

        authService.submitAllAuth(
                userId, idCard, bankCardId,
                propertyFile, carFile,
                employmentFile, salaryFile,
                socialSecurityFile, creditReportFile
        );

        return ResponseEntity.ok(ApiResponse.success(null, "全部认证材料提交成功"));
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
