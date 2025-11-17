package com.example.personal_loan.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.personal_loan.controller.dto.LoginRequest;
import com.example.personal_loan.controller.dto.LoginResponse;
import com.example.personal_loan.controller.dto.RegisterRequest;
import com.example.personal_loan.controller.dto.RegisterResponse;
import com.example.personal_loan.dto.UserSearchDto;
import com.example.personal_loan.entity.BlackUser;
import com.example.personal_loan.entity.User;
import com.example.personal_loan.exception.BusinessException;
import com.example.personal_loan.exception.InvalidCredentialsException;
import com.example.personal_loan.mapper.BlackListMapper;
import com.example.personal_loan.mapper.UserMapper;
import com.example.personal_loan.service.UserService;
import com.example.personal_loan.utils.JwtUtil;

import lombok.extern.slf4j.Slf4j;



@Service
@Slf4j
public class UserServiceImpl implements UserService {



    @Autowired
    private UserMapper userMapper;

    @Autowired
    private BlackListMapper blacklistMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public LoginResponse login(LoginRequest request) {

        User user = userMapper.findByPhone(request.getPhone());
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("用户名或密码错误");
        }

        String token = jwtUtil.generateAccessToken(user.getPhone(), user.getId().toString());

        String refreshToken = jwtUtil.generateRefreshToken(user.getId().toString());

        return new LoginResponse(token);
    
    }

    @Override
    public String refreshToken(String refreshToken){
        if (!jwtUtil.validateRefreshToken(refreshToken)) {
            throw new InvalidCredentialsException("无效或已过期的 refresh token");
        }

        Long userId = jwtUtil.getUserIdFromRefreshToken(refreshToken);
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("404", "用户不存在");
        }

        // 可选：检查用户是否被禁用、加入黑名单等
        // 生成新的 access token（不发新的 refresh token）
        return jwtUtil.generateAccessToken(user.getPhone(), user.getId().toString());
    }

    @Override
    public RegisterResponse userRegister(RegisterRequest request) {
        request.setPassword(passwordEncoder.encode(request.getPassword()));
        User user = new User(request.getName(), request.getPassword(), null, request.getPhone());
        user.setRole(0);    // 用户的注册，默认权限为0
        User newUser = addUser(user);
        return new RegisterResponse(newUser.getId(), newUser.getName(), newUser.getCreateTime());
    }

    @Override
    public RegisterResponse adminRegister(RegisterRequest request) {
        request.setPassword(passwordEncoder.encode(request.getPassword()));
        User user = new User(request.getName(), request.getPassword(), null, request.getPhone());
        user.setRole(1);    // 管理员权限设为1
        User newUser = addUser(user);
        return new RegisterResponse(newUser.getId(), newUser.getName(), newUser.getCreateTime());
    }

    @Override
    public User addUser(User user) {

        if (userMapper.findByPhone(user.getPhone()) != null) {
            throw new BusinessException("400", "该手机号已被注册");
        }

        if (userMapper.findByIdCard(user.getIdCard()) != null) {
            throw new BusinessException("400", "身份证号已被注册");
        }

        userMapper.insert(user);
        user.setCreateTime(LocalDateTime.now());
        return user;
    }

    @Override
    public void deleteUser(Long id) {
        userMapper.delete(id);
    }

    @Override
    @Transactional // 可选：根据业务决定是否加事务
    public void deleteUsers(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            userMapper.delete(id);
        }
    }

    @Override
    public User updateUser(Long id, User user) {

        User old = userMapper.findById(user.getId());

        // 设置id,更安全?
        user.setId(id);

        // 如果手机号被修改，校验唯一性
        if (user.getPhone() != null && !user.getPhone().equals(old.getPhone())) {
            if (userMapper.findByPhone(user.getPhone()) != null) {
                throw new BusinessException("400", "手机号已存在");
            } else {
                old.setPhone(user.getPhone());   // 更新手机号
            }
        }
        // 校验身份证号唯一性
        if (user.getIdCard() != null && !user.getIdCard().equals(old.getIdCard())) {
            if (userMapper.findByIdCardExcludeId(user.getIdCard(), user.getId()) != 0) {
                throw new BusinessException("400", "身份证号已存在");
            } else {
                old.setIdCard(user.getIdCard());  // 更新身份证号
            }
        }
        // 更新其他字段
        if (user.getPassword() != null) {
            old.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        if (user.getName() != null) {
            old.setName(user.getName());
        }
        if (user.getRole() != null) {
            old.setRole(user.getRole());
        }

        userMapper.update(old);
        return old;
    }

    @Override
    public User getUserById(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException("404", "该用户不存在");
        }
        return user;
    }

    @Override
    public List<User> getAllUsers() {
        return userMapper.findAll();
    }

    @Override
    public List<UserSearchDto> searchUsersByCreditScore(String expr) {
        CreditExpr parsed = parseCreditExpression(expr.trim());
        if (parsed == null) {
            throw new BusinessException("400", "无效的搜索");
        }

        return userMapper.selectUsersByCreditScore(parsed.operator, parsed.value);
    }

    @Override
    public void addToBlackList(Long userId, int blackLevel) {
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("404", "用户不存在");
        }

        // 检查是否已在黑名单
        if (blacklistMapper.selectByUserId(userId) != null) {
            throw new BusinessException("400", "用户已在黑名单中");
        }

        // 检查level范围 ？
        BlackUser blackUser = new BlackUser();
        blackUser.setUserId(userId);
        blackUser.setBlackLevel(blackLevel);
        blacklistMapper.insert(blackUser);
    }

    // 内部类
    private static class CreditExpr {

        private final String operator; // ">", ">=", "=", "<", "<="
        private final Integer value;

        public CreditExpr(String operator, Integer value) {
            this.operator = operator;
            this.value = value;
        }
    }

    private CreditExpr parseCreditExpression(String expr) {

        if (expr == null || expr.isEmpty()) {
            return null;
        }

        String[] ops = {">=", "<=", ">", "<", "="};
        for (String op : ops) {
            if (expr.startsWith(op)) {
                String numPart = expr.substring(op.length()).trim();
                try {
                    Integer val = Integer.valueOf(numPart);
                    // 可选：校验范围（根据实际业务）
                    return new CreditExpr(op, val);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }
}
