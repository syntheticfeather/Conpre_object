package com.example.personal_loan.service;

import java.util.List;

import com.example.personal_loan.dto.ApplicationRequest;
import com.example.personal_loan.dto.UserAppListResponse;
import com.example.personal_loan.entity.LoanApplication;

public interface ApplicationService {
    void addApplication(Long userId, ApplicationRequest request);  // 添加申请

    void withdrawApplication(Long userId, Long applicationId); // 取消/撤回申请

    LoanApplication userGetApplication(Long userId, Long applicationId); // 用户获取单个申请

    List<UserAppListResponse> userGetAllApplications(Long userId); // 用户获取所有申请
    
}
