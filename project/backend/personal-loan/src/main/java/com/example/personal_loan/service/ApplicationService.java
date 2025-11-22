package com.example.personal_loan.service;

import java.util.List;

import com.example.personal_loan.dto.AdminGetAppResponse;
import com.example.personal_loan.dto.ApplicationRequest;
import com.example.personal_loan.dto.UserGetAppResponse;

public interface ApplicationService {
    void addApplication(Long userId, ApplicationRequest request);  // 添加申请

    void withdrawApplication(Long userId, Long applicationId); // 取消/撤回申请

    // void updateApplication(Long userId, Long proId); //更新申请

    UserGetAppResponse userGetApplication(Long userId, Long applicationId); // 用户获取单个申请

    AdminGetAppResponse adminGetApplication(Long applicationId);  // 管理员获取单个申请

    List<UserGetAppResponse> userGetAllApplications(Long userId); // 用户获取所有申请

    List<AdminGetAppResponse> adminGetAllApplications(Long userId); // 管理员获取所有申请
}
