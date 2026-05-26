package com.example.personal_loan.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.personal_loan.entity.LoanApplication;
import com.example.personal_loan.entity.Order;
import com.example.personal_loan.enums.ApplicationStatus;
import com.example.personal_loan.enums.OrderStatus;
import com.example.personal_loan.mapper.ApplicationMapper;
import com.example.personal_loan.mapper.OrderMapper;
import com.example.personal_loan.mq.NotificationOutboxPublisher;
import com.example.personal_loan.service.AIApproveService;
import com.example.personal_loan.service.CreditScoreCalculator;
import com.example.personal_loan.service.RepaymentScheduleService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AIApproveServiceImpl implements AIApproveService {

    @Value("${approval.score.pass:80}")
    private int passThreshold;

    @Value("${approval.score.reject:40}")
    private int rejectThreshold;

    @Autowired
    private ApplicationMapper applicationMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private NotificationOutboxPublisher notificationOutboxPublisher;

    @Autowired
    private RepaymentScheduleService repaymentScheduleService;

    @Autowired
    private CreditScoreCalculator creditScoreCalculator;

    @Override
    @Transactional
    public Boolean AICheck(LoanApplication application) {
        // 1. 计算信用分
        int creditScore = creditScoreCalculator.calculate(application.getUserId());
        log.info("用户 {} 信用分: {}", application.getUserId(), creditScore);

        // 2. 根据信用分决策
        if (creditScore >= passThreshold) {
            // AI审核通过
            application.setStatus(ApplicationStatus.AI通过);
            application.setRejectReason("无");
            application.setReviewTime(LocalDateTime.now());
            applicationMapper.update(application);

            // 创建订单
            Order order = new Order(
                null,
                application.getUserId(),
                application.getProductId(),
                OrderStatus.正常,
                BigDecimal.ZERO,
                application.getLoanAmount(),
                application.getInterestRate(),
                application.getRepaidType(),
                application.getLoanPeriod(),
                application.getTerm(),
                0,
                null,
                0,
                LocalDateTime.now()
            );
            orderMapper.insert(order);

            // 生成还款计划
            repaymentScheduleService.generateRepaymentSchedule(order.getId());

            // 发送通知
            notificationOutboxPublisher.enqueueNotification(application.getUserId(), application.getId(), "LOAN_APPLICATION_STATUS");

            log.info("AI审核通过，信用分 {}，生成订单 {}", creditScore, order.getId());
            return true;

        } else if (creditScore >= rejectThreshold) {
            // 信用分在中间区间，需要人工审核
            application.setStatus(ApplicationStatus.审核中);
            application.setRejectReason("信用分不足，需人工审核");
            application.setReviewTime(LocalDateTime.now());
            applicationMapper.update(application);

            // 通知管理员进行人工审核
            notificationOutboxPublisher.enqueueAdminNotification(application.getId(), "LOAN_APPLICATION_APPROVE");

            log.info("AI审核转人工，信用分 {}", creditScore);
            return false;

        } else {
            // AI审核拒绝
            application.setStatus(ApplicationStatus.AI拒绝);
            application.setRejectReason("信用分不足");
            application.setReviewTime(LocalDateTime.now());
            applicationMapper.update(application);

            // 通知管理员
            notificationOutboxPublisher.enqueueAdminNotification(application.getId(), "LOAN_APPLICATION_APPROVE");

            log.info("AI审核拒绝，信用分 {}", creditScore);
            return false;
        }
    }
}
