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
import com.example.personal_loan.dto.GetCertResponse;
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

    /**
     * 上传认证信息
     */
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
     * 获取已经上传的认证信息
     */
    @GetMapping("/cert-info")
    public ResponseEntity<ApiResponse<GetCertResponse>> getCertInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        log.info("/api/auth/cert-info success called for user {} get cert information", userId);
        try {
            GetCertResponse certInfo = authService.getCert(userId);
            return ResponseEntity.ok(ApiResponse.success(certInfo, "认证信息获取成功"));
        } catch (Exception e) {
            log.error("user {} get info failed", userId, e);
            return ResponseEntity.status(500).body(ApiResponse.fail(500, "系统内部错误"));
        }
    }
}
