package com.example.personal_loan.service;

import org.springframework.web.multipart.MultipartFile;

import com.example.personal_loan.dto.GetCertResponse;
import com.example.personal_loan.entity.ImmovablesCert;
import com.example.personal_loan.entity.TriCert;
import com.example.personal_loan.entity.WorkCert;

public interface AuthService {

    // // 银行账户认证
    // void bankAccountAuth(Long userId, String bankCardId); 
    
    // // 身份证号验证
    // void idCardAuth(Long userId, String idCard);
    
    // // 不动产认证
    // void immovablesAuth(Long userId, MultipartFile propertyFile, MultipartFile carFile); 
    
    // // 工作认证
    // void occupationAuth(Long userId, MultipartFile employmentFile, MultipartFile salaryFile);
    
    // // 第三方信用分认证
    // void thirdPartyAuth(Long userId, MultipartFile socialSecurityFile, MultipartFile creditReportFile);

    
    // 提交所有认证信息
    void submitAllAuth(
            Long userId,
            String idCard,
            String bankCardId,
            MultipartFile propertyFile,
            MultipartFile carFile,
            MultipartFile employmentFile,
            MultipartFile salaryFile,
            MultipartFile socialSecurityFile,
            MultipartFile creditReportFile); 
    
    // 计算贷款认证分数
    int calScore(Long userId); 
    
    // 获取已经上传的认证信息
    GetCertResponse getCert(Long userId); 
    
    // 根据 workCertId 查询工作认证信息
    WorkCert getWorkCertById(Integer workCertId);
    
    // 根据 triCertId 查询第三方认证信息
    TriCert getTriCertById(Integer triCertId);
    
    // 根据 immovableCertId 查询不动产认证信息
    ImmovablesCert getImmovablesCertById(Integer immovableCertId);
    
    // 个人征信认证
    // void creditAuth(); 

}
