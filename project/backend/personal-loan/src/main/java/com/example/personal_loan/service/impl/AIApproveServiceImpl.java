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

    @Override
    @Transactional
    @RedisLocked(key = "'lock:loan-application:ai-check:' + #p0.id")
    public Boolean AICheck(LoanApplication application) {
        if (new Random().nextInt(100) < 50) {
            // AI审核成功
            application.setStatus(ApplicationStatus.已通过);
            application.setRejectReason("无");
            application.setReviewTime(LocalDateTime.now());
            applicationMapper.update(application);

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
            order.setTerm(application.getTerm()); 
            order.setCurrentTerm(0); // 初始为0，尚未还款（？）
            order.setOverdueDays(0);
            order.setStartTime(LocalDateTime.now());
            orderMapper.insert(order); // 插入订单表

            notificationOutboxPublisher.enqueueLoanApplicationStatus(application.getUserId(), application.getId(), "已通过");

            log.info("AI approve success");
            return true;
        }
        else {
            // AI审核失败
            application.setStatus(ApplicationStatus.AI拒绝);
            application.setRejectReason("AI审核未通过\n");
            applicationMapper.update(application);
            log.info("AI reject success");
            return false;
        }
    }
}
