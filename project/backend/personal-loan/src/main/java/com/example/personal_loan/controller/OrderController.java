package com.example.personal_loan.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.personal_loan.dto.AdminGetOrderResponse;
import com.example.personal_loan.dto.ApiResponse;
import com.example.personal_loan.dto.UserGetOrderResponse;
import com.example.personal_loan.service.OrderService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/orders")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    // ========== 用户端接口 ==========

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<UserGetOrderResponse>> getUserOrder(
            HttpServletRequest request,
            @PathVariable Long orderId) {

        Long userId = (Long) request.getAttribute("userId");
        log.info("/api/orders/{} success called for user {} to get order {} ", orderId, userId, orderId);
        UserGetOrderResponse response = orderService.userGetOrder(userId, orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<UserGetOrderResponse>>> getAllUserOrders(
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        List<UserGetOrderResponse> responses = orderService.userGetAllOrders(userId);
        log.info("/api/orders/my success called for user {} to get all orders", userId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PostMapping("/{orderId}/repay")
    public ResponseEntity<ApiResponse<UserGetOrderResponse>> repayOrder(
            HttpServletRequest request,
            @PathVariable Long orderId) {
    
        Long currentUserId = (Long) request.getAttribute("userId"); 
        UserGetOrderResponse response = orderService.repay(orderId);
        log.info("/api/orders/{}/repay success called for user {} to repay order {}", orderId, currentUserId, orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ========== 管理员接口 ==========

    @GetMapping("/admin/{orderId}")
    public ResponseEntity<ApiResponse<AdminGetOrderResponse>> getAdminOrder(
            @PathVariable Long orderId) {
        AdminGetOrderResponse response = orderService.adminGetOrder(orderId);
        log.info("/api/orders/admin/{} success called for admin to get order {} ", orderId, orderId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/admin/user/{userId}")
    public ResponseEntity<ApiResponse<List<AdminGetOrderResponse>>> getAdminAllOrdersByUser(
            @PathVariable Long userId) {
        List<AdminGetOrderResponse> responses = orderService.adminGetAllOrdersByUser(userId);
        log.info("/api/orders/admin/user/{} success called for admin to get user {} 's orders",userId,  userId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}