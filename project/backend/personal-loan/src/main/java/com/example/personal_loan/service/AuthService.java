package com.example.personal_loan.service;

public interface AuthService {

    // 不动产认证
    void immovablesAuth(); 

    // 银行账户认证
    void bankAccountAuth(); 

    // 身份证号验证
    void idCardAuth();

    // 个人征信认证
    // void creditAuth(); 

    // 工作认证
    void occupationAuth();

    // 第三方信用分认证
    void thirdPartyAuth();

    // 计算贷款认证分数
    int calScore(); 

    int getCert(); 

}
