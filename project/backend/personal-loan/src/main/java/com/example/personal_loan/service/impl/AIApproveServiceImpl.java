package com.example.personal_loan.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.personal_loan.aop.RedisLocked;
import com.example.personal_loan.entity.LoanApplication;
import com.example.personal_loan.entity.Order;
import com.example.personal_loan.enums.ApplicationStatus;
import com.example.personal_loan.enums.OrderStatus;
import com.example.personal_loan.mapper.ApplicationMapper;
import com.example.personal_loan.mapper.OrderMapper;
import com.example.personal_loan.mq.NotificationOutboxPublisher;
import com.example.personal_loan.service.AIApproveService;
import com.example.personal_loan.service.AuthService;
import com.example.personal_loan.service.RepaymentScheduleService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AIApproveServiceImpl implements AIApproveService {

    @Autowired
    private AuthService authService;

    @Autowired
    private ApplicationMapper applicationMapper;

    @Autowired 
    private OrderMapper orderMapper;

    @Autowired
    private NotificationOutboxPublisher notificationOutboxPublisher;
    
    @Autowired
    private RepaymentScheduleService repaymentScheduleService;

    @Override
    @Transactional
    @RedisLocked(key = "'lock:loan-application:ai-check:' + #p0.id")
    public Boolean AICheck(LoanApplication application) {
        if (new Random().nextInt(100) < 50) {
            // AI审核成功
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
            orderMapper.insert(order); // 插入订单表
            
            // 生成还款计划
            repaymentScheduleService.generateRepaymentSchedule(order.getId());

            // 写outbox消息发送通知
            notificationOutboxPublisher.enqueueNotification(application.getUserId(), application.getId(), "LOAN_APPLICATION_STATUS");

            log.info("AI approve success");
            return true;
        }
        else {
            // AI审核失败
            application.setStatus(ApplicationStatus.AI拒绝);
            application.setRejectReason("AI审核未通过\n");
            applicationMapper.update(application);
            
            // 通知管理员
            notificationOutboxPublisher.enqueueAdminNotification(application.getId(), "LOAN_APPLICATION_APPROVE");
            
            log.info("AI reject success");
            return false;
        }
    }
}
