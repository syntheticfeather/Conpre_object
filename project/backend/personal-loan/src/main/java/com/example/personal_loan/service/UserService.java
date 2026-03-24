package com.example.personal_loan.service;

import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;

import com.example.personal_loan.dto.BlackListDto;
import com.example.personal_loan.dto.LoginRequest;
import com.example.personal_loan.dto.LoginResponse;
import com.example.personal_loan.dto.RegisterRequest;
import com.example.personal_loan.dto.RegisterResponse;
import com.example.personal_loan.dto.UserDetailResponse;
import com.example.personal_loan.dto.UserListResponse;
import com.example.personal_loan.dto.UserSearchDto;
import com.example.personal_loan.dto.UserSelfResponse;
import com.example.personal_loan.entity.User;

public interface UserService {

    /*
    * 用户认证（登录注册）
    */

    // 用户登录(手机号+密码)
    LoginResponse login(LoginRequest request);

    // 刷新token
    Map<String, String> refreshToken(Map<String, String> refreshToken);

    // 用户注册
    RegisterResponse userRegister(RegisterRequest request);

    /*
    * 用户使用
    */

    // 获取个人信息
    UserSelfResponse getUserSelfInfo(Long userId);
    
    // 修改个人信息
    void updateUserSelfInfo(String newUserName, Long id);

    // 上传头像
    String uploadAvatar(Long userId, MultipartFile file);
    
    /*
    * 管理员使用
    */
   
    // 管理获取用户贷款状态及金额统计信息
    List<UserListResponse> adminGetAllUsersWithStats();
   
    // 添加黑名单
    void addToBlackList(Long adminId, Long userId, int blackLevel);

    // 解除黑名单
    void removeFromBlackList(Long adminId, Long userId);

    // 获取黑名单列表
    List<BlackListDto> getBlackList(Long adminId);
   
    // 获取单个用户信息
    UserDetailResponse adminGetUser(Long userId);

   
    List<UserSearchDto> searchUsersByCreditScore(String expr);

    /*
    * 其他
    */

   // 删除用户
   void deleteUser(Long id);

   // 批量删除
   void deleteUsers(List<Long> ids);

   // 更新用户
   User updateUser(Long id, User user);

   // 根据ID获取用户
   User getUserById(Long id);

}
