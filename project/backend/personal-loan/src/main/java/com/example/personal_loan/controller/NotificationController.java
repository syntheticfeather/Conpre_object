package com.example.personal_loan.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.personal_loan.dto.ApiResult;
import com.example.personal_loan.entity.Notification;
import com.example.personal_loan.service.NotificationService;
import com.example.personal_loan.service.NotificationSseService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/notifications")
@Slf4j
@Tag(name = "通知管理", description = "站内通知相关接口")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationSseService notificationSseService;

    @GetMapping(value = "/my", produces = "application/json")
    @Operation(summary = "获取我的通知", description = "用户获取自己的站内通知列表")
    public ResponseEntity<ApiResult<List<Notification>>> getMyNotifications(
            HttpServletRequest request,
            @Parameter(description = "返回条数,最大100") @RequestParam(defaultValue = "20") int limit) {
        Long userId = (Long) request.getAttribute("userId");
        List<Notification> list = notificationService.getMyNotifications(userId, limit);
        log.info("/api/notifications/my success called for user {} to get notifications", userId);
        return ResponseEntity.ok(ApiResult.success(list));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "订阅通知流", description = "用户通过 SSE 订阅实时通知")
    public SseEmitter stream(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        log.info("/api/notifications/stream success called for user {}", userId);
        return notificationSseService.subscribe(userId);
    }

    @PatchMapping(value = "/{notificationId}/read", produces = "application/json")
    @Operation(summary = "标记通知已读", description = "用户将指定通知标记为已读")
    public ResponseEntity<ApiResult<String>> markAsRead(
            HttpServletRequest request,
            @Parameter(description = "通知ID") @PathVariable Long notificationId) {
        Long userId = (Long) request.getAttribute("userId");
        notificationService.markAsRead(userId, notificationId);
        log.info("/api/notifications/{}/read success called for user {}", notificationId, userId);
        return ResponseEntity.ok(ApiResult.success("已标记通知为已读"));
    }
}
