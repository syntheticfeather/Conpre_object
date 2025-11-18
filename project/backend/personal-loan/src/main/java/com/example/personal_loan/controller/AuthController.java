package com.example.personal_loan.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.personal_loan.controller.dto.LoginRequest;
import com.example.personal_loan.controller.dto.LoginResponse;
import com.example.personal_loan.controller.dto.RegisterRequest;
import com.example.personal_loan.controller.dto.RegisterResponse;
import com.example.personal_loan.dto.ApiResponse;
import com.example.personal_loan.service.UserService;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    @Autowired
    private UserService userService;

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

    @PostMapping("/register/admin")
    public ResponseEntity<RegisterResponse> adminRegister(@RequestBody @Valid RegisterRequest request) {
        return ResponseEntity.ok(userService.adminRegister(request));
    }
}
