package com.example.personal_loan.service;

public interface AuthService {
    // 贷款审核
    boolean approve(Long id);

    // 资产认证
    void AssetAuth(); 

    // 银行账户认证
    void BankAccountAuth(); 

    // 个人征信认证
    void creditAuth(); 

    // 工作认证
    void OccupationAuth();

    // 第三方信用分认证
    void ThirdPartyAuth();

    // 计算贷款分数
    int CalScore(); 

    // 审核贷款申请
    Boolean approve(); 


}
