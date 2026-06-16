package com.example.personal_loan.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.personal_loan.aop.RedisLocked;
import com.example.personal_loan.dto.ApplicationDetailResponse;
import com.example.personal_loan.dto.ManualCheckResponse;
import com.example.personal_loan.dto.PendingApprovalResponse;
import com.example.personal_loan.entity.LoanApplication;
import com.example.personal_loan.entity.Order;
import com.example.personal_loan.entity.PostponeRequest;
import com.example.personal_loan.entity.User;
import com.example.personal_loan.enums.ApplicationStatus;
import com.example.personal_loan.enums.OrderStatus;
import com.example.personal_loan.exception.BusinessException;
import com.example.personal_loan.mapper.ApplicationMapper;
import com.example.personal_loan.mapper.OrderMapper;
import com.example.personal_loan.mapper.PostponeRequestMapper;
import com.example.personal_loan.mq.NotificationOutboxPublisher;
import com.example.personal_loan.service.ManualApproveService;
import com.example.personal_loan.service.RepaymentScheduleService;
import com.example.personal_loan.service.UserService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ManualApproveServiceImpl implements ManualApproveService{

    @Autowired
    private ApplicationMapper applicationMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationOutboxPublisher notificationOutboxPublisher;
    
    @Autowired
    private RepaymentScheduleService repaymentScheduleService;

    @Autowired
    private PostponeRequestMapper postponeRequestMapper;

    // 获得本审核员所有代办审核申请
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

    // 返回审核结果
    @Override
    @Transactional
    @RedisLocked(key = "'lock:loan-application:manual-check:' + #p0")
    public ManualCheckResponse manualCheck(Long loanApplicationId, Boolean approved, String manualRejectReason){
        // 查询申请
        LoanApplication application = applicationMapper.selectById(loanApplicationId);
        if (application == null) {
            throw new BusinessException(404,"贷款申请不存在");
        }
        // 检查状态
        if (!ApplicationStatus.审核中.equals(application.getStatus())&&!ApplicationStatus.AI拒绝.equals(application.getStatus())) {
            throw new BusinessException(400, "该申请的状态有误");
        }
        ApplicationStatus newStatus;
        String currentRejectReason = application.getRejectReason();
        ManualCheckResponse response = new ManualCheckResponse();

        if (approved) {
            // 人工通过
            application.setRejectReason("无");
            newStatus = ApplicationStatus.人工通过;
            application.setReviewTime(LocalDateTime.now());
            response.setReviewTime(LocalDateTime.now());
            response.setRejectReason("无");

            // 创建订单
            Order order = new Order();
            order.setUserId(application.getUserId());
            order.setApplicationId(application.getId());  // 🆕
            order.setProductId(application.getProductId());
            order.setStatus(OrderStatus.正常);
            order.setRepaidAmount(BigDecimal.ZERO);
            order.setLoanAmount(application.getLoanAmount());
            order.setInterestRate(application.getInterestRate());
            order.setRepaidType(application.getRepaidType());
            order.setLoanPeriod(application.getLoanPeriod());
            order.setContract(null);
            order.setTerm(application.getTerm());
            order.setCurrentTerm(0);
            order.setOverdueDays(0);
            order.setStartTime(LocalDateTime.now());
            orderMapper.insert(order);
            
            // 生成还款计划
            repaymentScheduleService.generateRepaymentSchedule(order.getId());
        } else {
            // 人工拒绝
            StringBuilder reasonBuilder = new StringBuilder();
            if (currentRejectReason != null && !currentRejectReason.trim().isEmpty()) {
                reasonBuilder.append(currentRejectReason);
            }
            if (manualRejectReason != null && !manualRejectReason.trim().isEmpty()) {
                reasonBuilder.append("人工审核未通过: ").append(manualRejectReason);
            } else {
                reasonBuilder.append("人工审核未通过: 未填写原因");
            }
            application.setRejectReason(reasonBuilder.toString());
            response.setRejectReason(reasonBuilder.toString());
            newStatus = ApplicationStatus.人工拒绝;
        }

        application.setStatus(newStatus);
        applicationMapper.update(application);

        notificationOutboxPublisher.enqueueNotification(application.getUserId(), application.getId(), "LOAN_APPLICATION_STATUS");

        response.setLoanApplicationId(loanApplicationId);
        response.setStatus(newStatus);

        return response;
    }

    // 获取已办审核列表
    @Override
    public List<PendingApprovalResponse> completedApproves(Long userId){
        // 权限校验
        User admin = userService.getUserById(userId);
        if (!admin.getRole().equals(1)) {
            throw new BusinessException(403, "无权限查看代办审核列表");
        }
        return applicationMapper.listCompletedApprovals();
    }

    // 获取待审核延期申请列表
    @Override
    public List<PostponeRequest> getPendingPostponeRequests() {
        return postponeRequestMapper.selectPendingRequests();
    }

    // 获取延期申请详情
    @Override
    public PostponeRequest getPostponeRequest(Long requestId) {
        return postponeRequestMapper.selectById(requestId);
    }

    // 管理员审核延期-同意
    @Override
    @Transactional
    public void approvePostpone(Long requestId) {
        PostponeRequest request = postponeRequestMapper.selectById(requestId);
        if (request == null) {
            throw new BusinessException(404, "延期申请不存在");
        }
        if (!"待审核".equals(request.getStatus())) {
            throw new BusinessException(400, "该申请已审核");
        }
        Order order = orderMapper.selectById(request.getOrderId());
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        request.setStatus("已通过");
        request.setReviewedAt(LocalDateTime.now());
        postponeRequestMapper.updateStatus(request);
        repaymentScheduleService.updateDueDateAfterPostpone(order.getId(), order.getCurrentTerm() + 1);
        log.info("延期申请 {} 已通过，订单 {}", requestId, order.getId());
    }

    // 管理员审核延期-拒绝
    @Override
    @Transactional
    public void rejectPostpone(Long requestId, String reason) {
        PostponeRequest request = postponeRequestMapper.selectById(requestId);
        if (!"待审核".equals(request.getStatus())) {
            throw new BusinessException(400, "该申请已审核");
        }
        request.setStatus("已拒绝");
        request.setRejectReason(reason);
        request.setReviewedAt(LocalDateTime.now());
        postponeRequestMapper.updateStatus(request);
        log.info("延期申请 {} 已拒绝，原因：{}", requestId, reason);
    }

}
