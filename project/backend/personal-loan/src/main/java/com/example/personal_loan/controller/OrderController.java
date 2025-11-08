package com.example.personal_loan.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.personal_loan.entity.Order;
import com.example.personal_loan.service.OrderService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 获取用户的单个订单
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrder(
            HttpServletRequest request,
            @PathVariable Long orderId) {
                
        Long currentUserId = (Long) request.getAttribute("userId");
        Order order = orderService.getOrder(currentUserId, orderId);
        return ResponseEntity.ok(order);
    }

    /**
     * 获取用户的所有订单
     */
    @GetMapping
    public ResponseEntity<List<Order>> getOrders(
            HttpServletRequest request) {

        Long currentUserId = (Long) request.getAttribute("userId");
        List<Order> orders = orderService.getOrders(currentUserId);
        return ResponseEntity.ok(orders);
    }

    /**
     * 用户发起还款
     */
    @PostMapping("/{orderId}/repay")
    public ResponseEntity<Order> repay(
            @PathVariable Long orderId) {
        Order order = orderService.repay(orderId);
        return ResponseEntity.ok(order);
    }

    /**
     * 用户申请延期
     */
    @PostMapping("/{orderId}/postpone")
    public ResponseEntity<Boolean> postpone(
            @PathVariable Long orderId) {
        Boolean success = orderService.postpone(orderId);
        return ResponseEntity.ok(success);
    }
}