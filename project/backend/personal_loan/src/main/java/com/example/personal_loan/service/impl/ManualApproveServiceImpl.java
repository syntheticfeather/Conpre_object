package com.example.personal_loan.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.personal_loan.dto.ApplicationDetailResponse;
import com.example.personal_loan.dto.PendingApprovalResponse;
import com.example.personal_loan.entity.LoanApplication;
import com.example.personal_loan.entity.User;
import com.example.personal_loan.enums.ApplicationStatus;
import com.example.personal_loan.exception.BusinessException;
import com.example.personal_loan.mapper.ApplicationMapper;
import com.example.personal_loan.service.ManualApproveService;
import com.example.personal_loan.service.UserService;

@Service
public class ManualApproveServiceImpl implements ManualApproveService{

    @Autowired
    private ApplicationMapper applicationMapper;

    @Autowired
    private UserService userService;

    // 获得本审核员所有需审核申请
    @Override
    public List<PendingApprovalResponse> getApproves(Long userId){
        // 权限校验
        User admin = userService.getUserById(userId);
        if (!admin.getRole().equals(1)) {
            throw new BusinessException(403, "无权限查看代办审核列表");
        }

        return applicationMapper.listPendingApprovals();
    }

    // 获得本审核员单个审核申请详情
    @Override
    public ApplicationDetailResponse getApprove(Long userId,Long loanApplicationId){
        // 权限校验
        User admin = userService.getUserById(userId);
        if (!admin.getRole().equals(1)) {
            throw new BusinessException(403, "无权限查看代办审核详情");
        }

        return applicationMapper.getApplicationDetail(loanApplicationId);
    }

    @Override
    @Transactional
    public Boolean manualCheck(Long loanApplicationId, Boolean approved){
        // 查询申请
        LoanApplication application = applicationMapper.selectById(loanApplicationId);
        if (application == null) {
            throw new BusinessException(404,"贷款申请不存在");
        }

        // 更新状态 (拒绝原因还未处理) (添加审核员id后期实现)
        ApplicationStatus newStatus = approved ? ApplicationStatus.APPROVED : ApplicationStatus.MANUAL_REJECTED;
        application.setStatus(newStatus);
        application.setReviewTime(LocalDateTime.now());

        // 更新申请记录
        int updated = applicationMapper.update(application);
        return updated > 0;
    }
}
