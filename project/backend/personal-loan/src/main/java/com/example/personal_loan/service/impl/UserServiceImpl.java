package com.example.personal_loan.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.personal_loan.controller.dto.LoginRequest;
import com.example.personal_loan.controller.dto.LoginResponse;
import com.example.personal_loan.dao.BlackListMapper;
import com.example.personal_loan.dao.UserMapper;
import com.example.personal_loan.entity.BlackUser;
import com.example.personal_loan.entity.User;
import com.example.personal_loan.exception.BusinessException;
import com.example.personal_loan.exception.ErrorCode;
import com.example.personal_loan.service.UserService;
import com.example.personal_loan.utils.JwtUtil;

@Service
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
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }


        String token = jwtUtil.generateToken(user.getPhone(),user.getId().toString());

        return new LoginResponse(token, user.getPhone());
        
    }

    @Override
    public User register(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return addUser(user);
    }

    @Override
    public User addUser(User user){
        
        if (userMapper.findByPhone(user.getPhone()) != null) {
            throw new BusinessException(ErrorCode.PHONE_EXISTS);
        }
       
        if (userMapper.findByIdCard(user.getIdCard()) != null) {
            throw new BusinessException(ErrorCode.ID_CARD_EXISTS);
        }
        
        userMapper.insert(user);
        return user;
    }
    
    @Override
    public void deleteUser(Long id){
        userMapper.delete(id);
    }
    
    @Override
    public User updateUser(Long id,User user) {
        
        User old = userMapper.findById(user.getId());

        // 设置id,更安全?
        user.setId(id);
    
        // 如果手机号被修改，校验唯一性
        if (user.getPhone() != null && !user.getPhone().equals(old.getPhone())) {
            if (userMapper.findByPhone(user.getPhone()) != null) {
                throw new BusinessException(ErrorCode.PHONE_EXISTS);
            }else{
                old.setPhone(user.getPhone());   // 更新手机号
            }
        }
        // 校验身份证号唯一性
        if (user.getIdCard() != null && !user.getIdCard().equals(old.getIdCard())) {
            if (userMapper.findByIdCardExcludeId(user.getIdCard(), user.getId()) != 0) {
                throw new BusinessException(ErrorCode.ID_CARD_EXISTS);
            }else{
                old.setIdCard(user.getIdCard());  // 更新身份证号
            }
        }
        // 更新其他字段
        if(user.getPassword()!=null){
            old.setPassword(passwordEncoder.encode(user.getPassword()));
        }
        if(user.getName()!=null){
            old.setName(user.getName());
        }
        if(user.getCreditScore()!=null){
            old.setCreditScore(user.getCreditScore());
        }
        if(user.getRole()!=null){
            old.setRole(user.getRole());
        }

        userMapper.update(old);
        return old;
    }   
    
    @Override
    public User getUserById(Long id){
        User user = userMapper.findById(id);
        if(user==null){
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }
    
    @Override
    public List<User> getAllUsers(){
        return userMapper.findAll();
    }

    @Override
    public void addToBlackList(Long userId, int blackLevel){
        User user = userMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        
        // 检查是否已在黑名单
        if (blacklistMapper.selectByUserId(userId) != null) {
            throw new BusinessException("用户已在黑名单中");
        }

        // 检查level范围 ？
        
        BlackUser blackUser = new BlackUser();
        blackUser.setUserId(userId);
        blackUser.setBlackLevel(blackLevel);
        blacklistMapper.insert(blackUser);
    }
}
