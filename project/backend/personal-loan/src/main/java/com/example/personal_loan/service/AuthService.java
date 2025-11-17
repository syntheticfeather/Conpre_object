package com.example.personal_loan.service;

public interface AuthService {

    // 资产认证
    void assetAuth(); 

    // 银行账户认证
    void bankAccountAuth(); 

    // 个人征信认证
    void creditAuth(); 

    // 工作认证
    void occupationAuth();

    // 第三方信用分认证
    void thirdPartyAuth();

    // 计算贷款认证分数
    int calScore(); 

    int getScore(); 

}
