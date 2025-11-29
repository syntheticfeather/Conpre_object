package com.example.personal_loan.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.personal_loan.dto.AddToBlackListRequest;
import com.example.personal_loan.dto.AdminGetUserResponse;
import com.example.personal_loan.dto.AdminUserListResponse;
import com.example.personal_loan.dto.ApiResponse;
import com.example.personal_loan.dto.BlackListDto;
import com.example.personal_loan.dto.UserSearchDto;
import com.example.personal_loan.dto.UserSelfResponse;
import com.example.personal_loan.dto.UserUpdateRequest;
import com.example.personal_loan.entity.User;
import com.example.personal_loan.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/users")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    // ========== 用户接口 ==========

    /**
     * 用户查看自己的信息
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserSelfResponse>> getCurrentUserInfo(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        log.info("/api/users/me success called for user {} to get his info", userId);
        UserSelfResponse response = userService.getUserSelfInfo(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 用户更新自己的信息（仅限 userName 和 avatar）
     */
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<UserSelfResponse>> updateCurrentUser(
            HttpServletRequest request,
            @RequestBody @Valid UserUpdateRequest userUpdateRequest) {
        Long userId = (Long) request.getAttribute("userId");
        log.info("/api/users/me success called for user {} to update his info", userId);

        UserSelfResponse updated = userService.updateUserSelfInfo(userUpdateRequest,userId);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    
    // ========== 管理员接口 ==========

    /**
     * 加入黑名单
     */
    @PostMapping("/blacklist/add")
    public ResponseEntity<ApiResponse<String>> addToBlackList(@RequestBody AddToBlackListRequest request) {
        log.info("/api/users/blacklist/add success called for admin to add user {} to black list", request.getUserId());
        userService.addToBlackList(request.getUserId(), request.getBlackLevel());
        return ResponseEntity.ok(ApiResponse.success(null,"用户已加入黑名单"));
    }

    /**
     * 解除黑名单
     */
    @PostMapping("/blacklist/remove")
    public ResponseEntity<ApiResponse<String>> removeFromBlackList(@RequestParam Long userId) {
        log.info("/api/users/blacklist/remove success called for admin to remove user {} from black list", userId);
        userService.removeFromBlackList(userId);
        return ResponseEntity.ok(ApiResponse.success(null,"用户已解除黑名单"));
    }

    /**
     * 获取当前黑名单列表
     */
    @GetMapping("/blacklist/list")
    public ResponseEntity<ApiResponse<List<BlackListDto>>> getBlackList() {
        log.info("/api/users/blacklist/list success called for admin to get black list");
        List<BlackListDto> list = userService.getBlackList();
        return ResponseEntity.ok(ApiResponse.success(list));
    }
    
    // 用户管理列表，查看贷款状态和金额统计信息
    @GetMapping("/admin/stats")
    public ResponseEntity<ApiResponse<List<AdminUserListResponse>>> getAllUsersWithStats() {
        log.info("/api/users/admin/stats success called for admin to get all users with stats");
        List<AdminUserListResponse> userStatsList = userService.adminGetAllUsersWithStats();
        return ResponseEntity.ok(ApiResponse.success(userStatsList));
    }

    /**
     * 获取指定用户的详细信息（含黑名单等级、信誉分）
     */
    @GetMapping("/admin/{userId}")
    public ResponseEntity<ApiResponse<AdminGetUserResponse>> getAdminUserDetail(
            @PathVariable Long userId) {
        
        log.info("/api/users/admin/{} success called for admin to get user {} info", userId,userId);
        
        AdminGetUserResponse response = userService.adminGetUser(userId);
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    
    @GetMapping("/search-by-credit")
    public ResponseEntity<List<UserSearchDto>> searchByCreditScore(@RequestParam String expr) {
        return ResponseEntity.ok(userService.searchUsersByCreditScore(expr));
    }
    
    /*
    * 刷新token
    * 需zff检测     
    */
   @PostMapping("/refresh-token")
   public ResponseEntity<Map<String, String>> refreshToken(@RequestBody Long id) {
       String newAccessToken = userService.refreshToken(id);
       return ResponseEntity.ok(Map.of("token", newAccessToken));
    }

    // 未使用
    @PostMapping
    public ResponseEntity<User> addUser(@RequestBody @Valid User user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.addUser(user));
    }
    
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
 
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }
    
    @PatchMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody @Valid User user) {
        return ResponseEntity.ok(userService.updateUser(id, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // @GetMapping("/search")
    // public List<User> searchUsers(@RequestParam(required = false) Long id,
    //                           @RequestParam(required = false) String name) {
    //     return userService.searchUsers(id, name);
    // }
}
