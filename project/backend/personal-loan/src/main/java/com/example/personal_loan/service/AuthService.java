package com.example.personal_loan.service;

import org.springframework.web.multipart.MultipartFile;

public interface AuthService {

    // 银行账户认证
    void bankAccountAuth(Long userId, String bankCardId); 
    
    // 身份证号验证
    void idCardAuth(Long userId, String idCard);
    
    // 不动产认证
    void immovablesAuth(Long userId, MultipartFile propertyFile, MultipartFile carFile); 
    
    // 工作认证
    void occupationAuth(Long userId, MultipartFile employmentFile, MultipartFile salaryFile);
    
    // 第三方信用分认证
    void thirdPartyAuth(Long userId, MultipartFile socialSecurityFile, MultipartFile creditReportFile);
    
    // 计算贷款认证分数
    int calScore(Long userId); 
    
    int getCert(Long userId); 
    
    // 个人征信认证
    // void creditAuth(); 

}
