package com.example.personal_loan.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.personal_loan.dto.ApiResult;
import com.example.personal_loan.entity.Notification;
import com.example.personal_loan.exception.BusinessException;
import com.example.personal_loan.service.NotificationService;
import com.example.personal_loan.service.NotificationSseService;
import com.example.personal_loan.service.UserService;

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
    
    @Autowired
    private UserService userService;

    /**
     * 用户
     */
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
    
    // 标记通知已读（用户管理员共用）
    @PatchMapping(value = "/{notificationId}/read", produces = "application/json")
    @Operation(summary = "标记通知已读", description = "用户将指定通知标记为已读")
    public ResponseEntity<ApiResult<String>> markAsRead(
            HttpServletRequest request,
            @Parameter(description = "通知ID") @PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        log.info("/api/notifications/{}/read success called for notification {}", notificationId);
        return ResponseEntity.ok(ApiResult.success("已标记通知为已读"));
    }


    /**
     * 管理员
     */
    @GetMapping(value = "/admin/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "订阅管理员通知流", description = "管理员通过 SSE 订阅实时通知")
    public SseEmitter adminStream(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        // 验证是否为管理员
        if (!userService.getUserById(userId).getRole().equals(1)) {
            throw new BusinessException(403, "无权限订阅管理员通知流");
        }
        // 使用管理员虚拟ID订阅
        log.info("/api/notifications/admin/stream success called for admin {}", userId);
        return notificationSseService.subscribe(999999L);
    }
    
    @GetMapping(value = "/admin", produces = "application/json")
    @Operation(summary = "获取管理员通知", description = "管理员获取系统通知列表")
    public ResponseEntity<ApiResult<List<Notification>>> getAdminNotifications(
            HttpServletRequest request,
            @Parameter(description = "返回条数,最大100") @RequestParam(defaultValue = "20") int limit) {
        Long userId = (Long) request.getAttribute("userId");
        // 验证是否为管理员
        if (!userService.getUserById(userId).getRole().equals(1)) {
            throw new BusinessException(403, "无权限访问管理员通知");
        }
        List<Notification> list = notificationService.getAdminNotifications(limit);
        log.info("/api/notifications/admin success called for admin {}", userId);
        return ResponseEntity.ok(ApiResult.success(list));
    }

    // 删除一条通知
    @DeleteMapping(value = "/{notificationId}", produces = "application/json")
    @Operation(summary = "删除通知", description = "管理员删除指定通知")
    public ResponseEntity<ApiResult<String>> deleteNotification(
            HttpServletRequest request,
            @Parameter(description = "通知ID") @PathVariable Long notificationId) {
        notificationService.deleteNotification(notificationId);
        log.info("/api/notifications/{}/delete success called for notification {}", notificationId);
        return ResponseEntity.ok(ApiResult.success("删除成功"));
    }

    // 批量删除通知
    @DeleteMapping(value = "/batch", produces = "application/json")
    @Operation(summary = "批量删除通知", description = "管理员批量删除指定通知")
    public ResponseEntity<ApiResult<String>> deleteDeleteNotifications(
            HttpServletRequest request,
            @Parameter(description = "通知ID列表") @RequestBody List<Long> notificationIds) {
        notificationService.batchDeleteNotifications(notificationIds);
        log.info("/api/notifications/batch/delete success called for notifications {}", notificationIds);
        return ResponseEntity.ok(ApiResult.success("批量删除成功"));
    }

    
}
