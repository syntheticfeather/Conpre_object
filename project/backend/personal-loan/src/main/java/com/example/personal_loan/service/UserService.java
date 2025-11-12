package com.example.personal_loan.service;

import java.util.List;

import com.example.personal_loan.controller.dto.LoginRequest;
import com.example.personal_loan.controller.dto.LoginResponse;
import com.example.personal_loan.controller.dto.RegisterRequest;
import com.example.personal_loan.controller.dto.RegisterResponse;
import com.example.personal_loan.dto.UserSearchDto;
import com.example.personal_loan.entity.User;

public interface UserService {
        // 用户登录(手机号+密码)
        LoginResponse login(LoginRequest request);

        // 用户注册
        RegisterResponse userRegister(RegisterRequest request);

        //管理员注册
        RegisterResponse adminRegister(RegisterRequest request);

        // 添加用户
        User addUser(User user);
    
        // 删除用户
        void deleteUser(Long id);

        // 批量删除
        void deleteUsers(List<Long> ids);
    
        // 更新用户
        User updateUser(Long id,User user);
    
        // 根据ID获取用户
        User getUserById(Long id);
    
        // 获取所有用户
        List<User> getAllUsers();

        // // 根据id，name搜索用户
        // List<User> searchUsers(Long id, String name);

        // 根据信誉分表达式搜索用户
        List<UserSearchDto> searchUsersByCreditScore(String expr);

        // 添加黑名单
        void addToBlackList(Long userId, int blackLevel);
}
