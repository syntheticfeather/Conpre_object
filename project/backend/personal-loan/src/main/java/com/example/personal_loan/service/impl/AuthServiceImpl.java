package com.example.personal_loan.service.impl;

import org.springframework.stereotype.Service;

import com.example.personal_loan.dao.UserMapper;
import com.example.personal_loan.service.AuthService;

// 还未实现
@Service
public class AuthServiceImpl implements AuthService{

    UserMapper userMapper; // 用户认证信息类

    // ThirdSerivce thirdService; // 三方认证信息类

    // ManualApproveSendService manualApproveService ; // 人工审核服务类

    // ValuationSystem valuationSystem; // 计算贷款分数的机器学习模型API类

    // 贷款审核
    @Override
    public boolean approve(Long id) {
        return true;
    }

    // 资产认证
    public void AssetAuth(){
        
    } 

    // 银行账户认证
    public void BankAccountAuth(){ 
        
    }

    // 个人征信认证
    public void creditAuth(){ 

    }

    // 工作认证
    public void OccupationAuth(){

    }

    // 第三方信用分认证
    public void ThirdPartyAuth(){

    }

    // 计算贷款分数
    public int CalScore(){ 
        return 0;
    }

    // 审核贷款申请
    public Boolean approve(){ 
        return true;
    }

    // AI审核
    public Boolean autoApprove(){ 
        return true;
    } 

    // 转交人工审核
    public void sendToManualApprove(){ 

    } 
}
