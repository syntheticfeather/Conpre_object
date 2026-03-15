package com.example.personal_loan.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.personal_loan.dto.AddToBlackListRequest;
import com.example.personal_loan.dto.ApiResult;
import com.example.personal_loan.dto.BlackListDto;
import com.example.personal_loan.dto.UserDetailResponse;
import com.example.personal_loan.dto.UserListResponse;
import com.example.personal_loan.dto.UserSearchDto;
import com.example.personal_loan.dto.UserSelfResponse;
import com.example.personal_loan.dto.UserUpdateRequest;
import com.example.personal_loan.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/users")
@Slf4j
@Tag(name = "用户管理", description = "用户相关接口")
public class UserController {

    @Autowired
    private UserService userService;

    // ========== 用户接口 ==========

    /**
     * 用户查看自己的信息
     */
    @GetMapping(value = "/me", produces = "application/json")
    @Operation(summary = "获取当前用户信息", description = "用户查看自己的详细信息")
    public ResponseEntity<ApiResult<UserSelfResponse>> getCurrentUserInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        log.info("/api/users/me success called for user {} to get his info", userId);
        UserSelfResponse response = userService.getUserSelfInfo(userId);
        return ResponseEntity.ok(ApiResult.success(response));
    }

    /**
     * 用户更新自己的信息（仅限 userName 和 avatar）
     */
    @PatchMapping(value = "/me", produces = "application/json")
    @Operation(summary = "更新用户信息", description = "用户更新自己的信息（仅限用户名和头像）")
    public ResponseEntity<ApiResult<UserSelfResponse>> updateCurrentUser(
            HttpServletRequest request,
            @Parameter(description = "用户更新信息") @RequestBody @Valid UserUpdateRequest userUpdateRequest) {
        Long userId = (Long) request.getAttribute("userId");
        log.info("/api/users/me success called for user {} to update his info", userId);

        UserSelfResponse updated = userService.updateUserSelfInfo(userUpdateRequest,userId);
        return ResponseEntity.ok(ApiResult.success(updated));
    }

    /**
     * 用户上传头像
     */
    @PostMapping(value="/avatar", produces="application/json")
    @Operation(summary = "上传头像", description = "用户上传头像图片")
    public ResponseEntity<ApiResult<String>> uploadAvatar(
            HttpServletRequest request,
            @Parameter(description = "头像文件") @RequestParam("file") MultipartFile file) {

        Long userId = (Long) request.getAttribute("userId");
        log.info("/api/users/avatar success called for user {} to upload avatar", userId);
        
        String avatarUrl = userService.uploadAvatar(userId, file);
        return ResponseEntity.ok(ApiResult.success(avatarUrl));
    }

    
    // ========== 管理员接口 ==========

    /**
     * 加入黑名单
     */
    @PostMapping(value = "/blacklist/add",produces="application/json")
    @Operation(summary = "加入黑名单", description = "管理员将用户加入黑名单")
    public ResponseEntity<ApiResult<String>> addToBlackList(HttpServletRequest httpRequest, @Parameter(description = "加入黑名单请求") @RequestBody AddToBlackListRequest request) {
        log.info("/api/users/blacklist/add success called for admin to add user {} to black list", request.getUserId());
        Long adminId = (Long) httpRequest.getAttribute("userId");
        userService.addToBlackList(adminId, request.getUserId(), request.getBlackLevel());
        return ResponseEntity.ok(ApiResult.success(null,"用户已加入黑名单"));
    }

    /**
     * 解除黑名单
     */
    @PostMapping(value = "/blacklist/remove", produces = "application/json")
    @Operation(summary = "解除黑名单", description = "管理员将用户从黑名单中解除")
    public ResponseEntity<ApiResult<String>> removeFromBlackList(HttpServletRequest httpRequest, @Parameter(description = "用户ID") @RequestParam Long userId) {
        log.info("/api/users/blacklist/remove success called for admin to remove user {} from black list", userId);
        Long adminId = (Long) httpRequest.getAttribute("userId");
        userService.removeFromBlackList(adminId, userId);
        return ResponseEntity.ok(ApiResult.success(null,"用户已解除黑名单"));
    }

    /**
     * 获取当前黑名单列表
     */
    @GetMapping(value = "/blacklist/list", produces = "application/json")
    @Operation(summary = "获取黑名单列表", description = "管理员获取当前黑名单用户列表")
    public ResponseEntity<ApiResult<List<BlackListDto>>> getBlackList(HttpServletRequest httpRequest) {
        log.info("/api/users/blacklist/list success called for admin to get black list");
        Long adminId = (Long) httpRequest.getAttribute("userId");
        List<BlackListDto> list = userService.getBlackList(adminId);
        return ResponseEntity.ok(ApiResult.success(list));
    }
    
    /**
     * 用户管理列表，查看贷款状态和金额统计信息
     */
    @GetMapping(value = "/stats", produces = "application/json")
    @Operation(summary = "获取用户统计信息", description = "管理员获取用户列表及其贷款状态和金额统计信息")
    public ResponseEntity<ApiResult<List<UserListResponse>>> getAllUsersWithStats() {
        log.info("/api/users/stats success called for admin to get all users with stats");
        List<UserListResponse> userStatsList = userService.adminGetAllUsersWithStats();
        return ResponseEntity.ok(ApiResult.success(userStatsList));
    }

    /**
     * 获取指定用户的详细信息
     */
    @GetMapping(value = "/{userId}/detail", produces = "application/json")
    @Operation(summary = "获取用户详情", description = "管理员获取指定用户的详细信息")
    public ResponseEntity<ApiResult<UserDetailResponse>> getAdminUserDetail(
            @Parameter(description = "用户ID") @PathVariable Long userId) {
        
        log.info("/api/users/{}/detail success called for admin to get user {} info", userId,userId);
        
        UserDetailResponse response = userService.adminGetUser(userId);
        
        return ResponseEntity.ok(ApiResult.success(response));
    }
    
    @GetMapping(value = "/search-by-credit", produces = "application/json")
    @Operation(summary = "按信用分搜索用户", description = "按信用分表达式搜索用户")
    public ResponseEntity<List<UserSearchDto>> searchByCreditScore(@Parameter(description = "信用分表达式") @RequestParam String expr) {
        return ResponseEntity.ok(userService.searchUsersByCreditScore(expr));
    }
    
    /*
    * 刷新token
    */
   @PostMapping(value = "/refresh-token", produces = "application/json")
   @Operation(summary = "刷新token", description = "刷新用户的访问令牌")
   public ResponseEntity<Map<String, String>> refreshToken(@Parameter(description = "用户ID") @RequestBody Long id) {
       String newAccessToken = userService.refreshToken(id);
       return ResponseEntity.ok(Map.of("token", newAccessToken));
    }

}
