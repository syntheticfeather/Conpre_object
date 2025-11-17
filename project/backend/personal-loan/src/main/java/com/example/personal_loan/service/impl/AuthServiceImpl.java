package com.example.personal_loan.service.impl;

import org.springframework.stereotype.Service;

import com.example.personal_loan.mapper.UserMapper;
import com.example.personal_loan.service.AuthService;

@Service
public class AuthServiceImpl implements AuthService{

    UserMapper userMapper; // 用户认证信息类

    // ThirdSerivce thirdService; // 三方认证信息类

    // ManualApproveSendService manualApproveService ; // 人工审核服务类

    // ValuationSystem valuationSystem; // 计算贷款分数的机器学习模型API类

    // 资产认证
    @Override
    public void assetAuth(){
        
    } 

    @Override
    public void bankAccountAuth(){ 
        
    }

    // 个人征信认证
    @Override
    public void creditAuth(){ 

    }

    // 工作认证
    @Override
    public void occupationAuth(){

    }

    // 第三方信用分认证
    @Override
    public void thirdPartyAuth(){

    }

    // 计算贷款分数
    @Override
    public int calScore(){ 
        return 0;
    }

    @Override
    public int getScore(){ 
        return 0;
    }
}
