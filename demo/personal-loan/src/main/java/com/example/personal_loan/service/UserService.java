package com.example.personal_loan.service;

import java.util.List;

import com.example.personal_loan.entity.User;

public interface UserService {
        // 用户登录(手机号+密码)
        User login(String phone, String password);

        // 添加用户
        User addUser(User user);
    
        // 删除用户
        void deleteUser(Long id);
    
        // 更新用户
        User updateUser(Long id,User user);
    
        // 根据ID获取用户
        User getUserById(Long id);
    
        // 获取所有用户
        List<User> getAllUsers();
}
