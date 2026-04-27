package com.example.personal_loan.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.personal_loan.dto.ApiResult;
import com.example.personal_loan.dto.UserGetOrderResponse;
import com.example.personal_loan.dto.UserOrderListResponse;
import com.example.personal_loan.entity.RepaymentSchedule;
import com.example.personal_loan.service.OrderService;
import com.example.personal_loan.service.RepaymentScheduleService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/orders")
@Slf4j
@Tag(name = "订单管理", description = "贷款订单相关接口")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private RepaymentScheduleService repaymentScheduleService;

    @GetMapping(value = "/{orderId}", produces = "application/json")
    @Operation(summary = "获取订单详情", description = "用户获取指定贷款订单的详细信息")
    public ResponseEntity<ApiResult<UserGetOrderResponse>> getUserOrder(
            HttpServletRequest request,
            @Parameter(description = "订单ID") @PathVariable Long orderId) {

        Long userId = (Long) request.getAttribute("userId");
        log.info("/api/orders/{} success called for user {} to get order {} ", orderId, userId, orderId);
        UserGetOrderResponse response = orderService.userGetOrder(userId, orderId);
        return ResponseEntity.ok(ApiResult.success(response));
    }

    @GetMapping(value = "/my", produces = "application/json")
    @Operation(summary = "获取所有订单", description = "用户获取自己的所有贷款订单")
    public ResponseEntity<ApiResult<List<UserOrderListResponse>>> getAllUserOrders(
            HttpServletRequest request) {

        Long userId = (Long) request.getAttribute("userId");
        List<UserOrderListResponse> responses = orderService.userGetAllOrders(userId);
        log.info("/api/orders/my success called for user {} to get all orders", userId);
        return ResponseEntity.ok(ApiResult.success(responses));
    }

    @PostMapping(value = "/{orderId}/repay", produces = "application/json")
    @Operation(summary = "还款操作", description = "用户对指定贷款订单进行还款操作")
    public ResponseEntity<ApiResult<String>> repayOrder(
            HttpServletRequest request,
            @Parameter(description = "订单ID") @PathVariable Long orderId) {

        Long currentUserId = (Long) request.getAttribute("userId");
        orderService.repay(orderId);
        log.info("/api/orders/{}/repay success called for user {} to repay order {}", orderId, currentUserId, orderId);
        return ResponseEntity.ok(ApiResult.success("已发起还款"));
    }

    @GetMapping(value = "/{orderId}/repayment-plan", produces = "application/json")
    @Operation(summary = "获取还款计划", description = "用户获取指定贷款订单的还款计划")
    public ResponseEntity<ApiResult<List<RepaymentSchedule>>> getRepaymentPlan(
            HttpServletRequest request,
            @Parameter(description = "订单ID") @PathVariable Long orderId) {

        Long userId = (Long) request.getAttribute("userId");
        log.info("/api/orders/{}/repayment-plan success called for user {} to get repayment plan for order {}", orderId, userId, orderId);
        List<RepaymentSchedule> plan = repaymentScheduleService.getRepaymentSchedule(orderId);
        return ResponseEntity.ok(ApiResult.success(plan));
    }

    @PostMapping(value = "/{orderId}/postpone", produces = "application/json")
    @Operation(summary = "申请延期还款", description = "用户对指定贷款订单申请延期还款")
    public ResponseEntity<ApiResult<String>> postponeOrder(
            HttpServletRequest request,
            @Parameter(description = "订单ID") @PathVariable Long orderId) {

        Long userId = (Long) request.getAttribute("userId");
        orderService.postpone(orderId);
        log.info("/api/orders/{}/postpone success called for user {} to postpone order {}", orderId, userId, orderId);
        return ResponseEntity.ok(ApiResult.success("已发起延期还款申请"));
    }

    @PostMapping(value = "/{orderId}/early-repay", produces = "application/json")
    @Operation(summary = "提前还款", description = "用户对指定贷款订单进行提前还款")
    public ResponseEntity<ApiResult<String>> earlyRepayOrder(
            HttpServletRequest request,
            @Parameter(description = "订单ID") @PathVariable Long orderId) {

        Long userId = (Long) request.getAttribute("userId");
        orderService.earlyRepay(orderId);
        log.info("/api/orders/{}/early-repay success called for user {} to early repay order {}", orderId, userId, orderId);
        return ResponseEntity.ok(ApiResult.success("已发起提前还款"));
    }
}
