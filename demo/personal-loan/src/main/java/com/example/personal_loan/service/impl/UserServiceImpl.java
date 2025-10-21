package com.example.personal_loan.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.personal_loan.dao.UserMapper;
import com.example.personal_loan.entity.User;
import com.example.personal_loan.exception.BusinessException;
import com.example.personal_loan.exception.ErrorCode;
import com.example.personal_loan.service.UserService;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public User login(String phone, String password){

        User user = userMapper.findByPhone(phone);

        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        if (!user.getPassword().equals(password)) { // 后续建议加密
            throw new BusinessException(ErrorCode.PASSWORD_ERROR);
        }
        return user;
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
            }
        }
        // 校验身份证号唯一性
        if (user.getIdCard() != null && !user.getIdCard().equals(old.getIdCard())) {
            if (userMapper.findByIdCardExcludeId(user.getIdCard(), user.getId()) != 0) {
                throw new BusinessException(ErrorCode.ID_CARD_EXISTS);
            }
        }

        userMapper.update(user);

        return userMapper.findById(id);
    
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
}
