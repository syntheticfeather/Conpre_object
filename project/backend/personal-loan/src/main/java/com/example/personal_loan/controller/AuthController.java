package com.example.personal_loan.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.personal_loan.controller.dto.LoginRequest;
import com.example.personal_loan.controller.dto.LoginResponse;
import com.example.personal_loan.entity.User;
import com.example.personal_loan.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        LoginResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody @Valid User user) {
        return ResponseEntity.ok(userService.register(user));
    }

    @GetMapping("/test")
    public String test() {
        return "Hello World!";
    }
}
