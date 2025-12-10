package com.example.personal_loan.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.personal_loan.dto.ApplicationDetailResponse;
import com.example.personal_loan.dto.ManualCheckResponse;
import com.example.personal_loan.dto.PendingApprovalResponse;
import com.example.personal_loan.entity.LoanApplication;
import com.example.personal_loan.entity.Order;
import com.example.personal_loan.entity.User;
import com.example.personal_loan.enums.ApplicationStatus;
import com.example.personal_loan.enums.OrderStatus;
import com.example.personal_loan.exception.BusinessException;
import com.example.personal_loan.mapper.ApplicationMapper;
import com.example.personal_loan.mapper.OrderMapper;
import com.example.personal_loan.service.ManualApproveService;
import com.example.personal_loan.service.UserService;

@Service
public class ManualApproveServiceImpl implements ManualApproveService{

    @Autowired
    private ApplicationMapper applicationMapper;

    @Autowired
    private OrderMapper orderMapper;

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
    public ManualCheckResponse manualCheck(Long loanApplicationId, Boolean approved, String manualRejectReason){
        // 查询申请
        LoanApplication application = applicationMapper.selectById(loanApplicationId);
        if (application == null) {
            throw new BusinessException(404,"贷款申请不存在");
        }
        ApplicationStatus newStatus;
        String currentRejectReason = application.getRejectReason(); // 可能已有 AI 的拒绝原因
        ManualCheckResponse response = new ManualCheckResponse();

        if (approved) {
            // 人工通过
            StringBuilder reasonBuilder = new StringBuilder();
            if (currentRejectReason != null && !currentRejectReason.trim().isEmpty()) {
                reasonBuilder.append(currentRejectReason);
            }
            reasonBuilder.append("Manual approve");
            application.setRejectReason(reasonBuilder.toString());
            newStatus = ApplicationStatus.APPROVED;
            application.setReviewTime(LocalDateTime.now());
            response.setReviewTime(LocalDateTime.now());
            response.setRejectReason(reasonBuilder.toString());

            // 创建订单
            Order order = new Order();
            order.setUserId(application.getUserId());
            order.setProductId(application.getProductId()); 
            order.setStatus(OrderStatus.正常); 
            order.setRepaidAmount(BigDecimal.ZERO);
            order.setLoanAmount(application.getLoanAmount()); 
            order.setInterestRate(application.getInterestRate()); 
            order.setRepaidType(application.getRepaidType()); 
            order.setLoanPeriod(application.getLoanPeriod());
            // contract 后续生成
            order.setContract(null); 
            order.setTerm(application.getLoanPeriod()); 
            order.setCurrentTerm(0); // 初始为0，尚未还款（？）
            order.setOverdueDays(0);
            order.setStartTime(LocalDateTime.now());
            orderMapper.insert(order); // 插入订单表
        } else {
            // 人工拒绝
            StringBuilder reasonBuilder = new StringBuilder();
            if (currentRejectReason != null && !currentRejectReason.trim().isEmpty()) {
                reasonBuilder.append(currentRejectReason);
            }
            if (manualRejectReason != null && !manualRejectReason.trim().isEmpty()) {
                reasonBuilder.append("人工拒绝: ").append(manualRejectReason);
            } else {
                reasonBuilder.append("人工拒绝: 未填写原因");
            }
            application.setRejectReason(reasonBuilder.toString());
            response.setRejectReason(reasonBuilder.toString());
            newStatus = ApplicationStatus.MANUAL_REJECTED;
        }

        application.setStatus(newStatus);
        applicationMapper.update(application);

        response.setLoanApplicationId(loanApplicationId);
        response.setStatus(newStatus);

        return response;
    }
}