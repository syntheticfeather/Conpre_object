package com.example.personal_loan.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.personal_loan.aop.RedisLocked;
import com.example.personal_loan.config.RabbitMQConfig;
import com.example.personal_loan.dto.PaymentRequestedEvent;
import com.example.personal_loan.dto.UserGetOrderResponse;
import com.example.personal_loan.dto.UserOrderListResponse;
import com.example.personal_loan.entity.LoanProduct;
import com.example.personal_loan.entity.Order;
import com.example.personal_loan.entity.OutboxMessage;
import com.example.personal_loan.entity.PostponeRequest;
import com.example.personal_loan.entity.RepaymentSchedule;
import com.example.personal_loan.enums.BusinessType;
import com.example.personal_loan.enums.OrderStatus;
import com.example.personal_loan.exception.BusinessException;
import com.example.personal_loan.factory.OutboxMessageFactory;
import com.example.personal_loan.mapper.LoanProductMapper;
import com.example.personal_loan.mapper.OrderMapper;
import com.example.personal_loan.mapper.OutboxMapper;
import com.example.personal_loan.mapper.PostponeRequestMapper;
import com.example.personal_loan.mapper.RepaymentScheduleMapper;
import com.example.personal_loan.service.OrderService;
import com.example.personal_loan.service.RepaymentScheduleService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Transactional
public class OrderServiceImpl implements OrderService{
    
    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private LoanProductMapper loanProductMapper;

    @Autowired
    private OutboxMapper outboxMapper;

    @Autowired
    private OutboxMessageFactory outboxMessageFactory;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RepaymentScheduleService repaymentScheduleService;

    @Autowired
    private RepaymentScheduleMapper repaymentScheduleMapper;

    @Autowired
    private PostponeRequestMapper postponeRequestMapper;

    @Override
    public UserGetOrderResponse userGetOrder(Long userId, Long orderId){
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权查看他人订单");
        }
        LoanProduct product = loanProductMapper.findById(order.getProductId());
        String productName = product.getProductName();
        return new UserGetOrderResponse(productName, order);
    }
    
    @Override
    public List<UserOrderListResponse> userGetAllOrders(Long userId) {
        return orderMapper.selectOrderListByUserId(userId);
    }
    
    @Override
    @RedisLocked(key = "'lock:order:repay:' + #p0")
    public void repay(Long orderId) {
        // 检查订单状态
        Order order = orderMapper.selectById(orderId);
        if (order.getCurrentTerm() >= order.getTerm()) {
            throw new BusinessException("订单已结清，无需还款");
        }
        OrderStatus status = order.getStatus();
        if (status == OrderStatus.已完成) {
            throw new BusinessException("订单已完成，无需还款");
        }

        // 检查还款计划
        List<RepaymentSchedule> schedules = repaymentScheduleMapper.selectByOrderId(orderId);
        RepaymentSchedule currentSchedule = null;
        for (RepaymentSchedule schedule : schedules) {
            if (schedule.getTerm().equals(order.getCurrentTerm() + 1)) {
                currentSchedule = schedule;
                break;
            }
        }
        if (currentSchedule == null) {
            throw new BusinessException("当期还款计划不存在");
        }
        if ("已还".equals(currentSchedule.getStatus())) {
            throw new BusinessException("当期已还款");
        }
        // 投递还款消息
        try {
            PaymentRequestedEvent event = new PaymentRequestedEvent(
                    orderId,
                    currentSchedule.getTotalAmount(),
                    LocalDateTime.now()
            );

            OutboxMessage outboxMessage = outboxMessageFactory.create(BusinessType.PAYMENT_REQUESTED, event, orderId);
            outboxMapper.insert(outboxMessage);

        } catch (Exception e) {
            log.error("写入outbox表失败", e);
            throw new BusinessException("消息投递失败");
        }

        log.info("订单 {} 第 {} 期还款消息已投递", orderId, order.getCurrentTerm() + 1);
    }

    /*
     * 提交申请延期
     * @param orderId 订单ID
     * @return 是否成功
     */
    @Override
    public Boolean postpone(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order.getCurrentTerm() >= order.getTerm()) {
            throw new BusinessException(400,"订单已结清，无法延期");
        }
        OrderStatus status = order.getStatus();
        if (status == OrderStatus.已完成) {
            throw new BusinessException(400,"当前订单无法申请延期");
        }
        
        PostponeRequest existingRequest = postponeRequestMapper.selectByOrderIdAndCurrentTerm(orderId, order.getCurrentTerm());
        if (existingRequest != null) {
            throw new BusinessException(400,"当前期数已申请延期，请勿重复申请");
        }
        
        PostponeRequest request = new PostponeRequest(
                null,
                orderId,
                order.getUserId(),
                order.getCurrentTerm(),
                "待审核",
                null,
                LocalDateTime.now(),
                null
        );
        postponeRequestMapper.insert(request);
        return true;
    }

    /*
     * 提前还款
     * @param orderId 订单ID
     */
    @Override
    @Transactional
    public void earlyRepay(Long orderId) {
        // 检查订单状态
        Order order = orderMapper.selectById(orderId);
        if (order.getCurrentTerm() >= order.getTerm()) {
            throw new BusinessException("订单已结清，无需提前还款");
        }
        // 检查还款计划
        List<RepaymentSchedule> schedules = repaymentScheduleMapper.selectByOrderId(orderId);
        BigDecimal totalEarlyAmount = BigDecimal.ZERO;
        for (RepaymentSchedule schedule : schedules) {
            if (schedule.getTerm() > order.getCurrentTerm()) {
                totalEarlyAmount = totalEarlyAmount.add(schedule.getPrincipal()).add(schedule.getInterest());
            }
        }
        // 投递还款消息
        try {
            PaymentRequestedEvent event = new PaymentRequestedEvent(
                    orderId,
                    totalEarlyAmount,
                    LocalDateTime.now()
            );
            OutboxMessage outboxMessage = outboxMessageFactory.create(BusinessType.PAYMENT_REQUESTED, event, orderId);
            outboxMapper.insert(outboxMessage);
        } catch (Exception e) {
            log.error("写入outbox表失败", e);
            throw new BusinessException("消息投递失败");
        }
        log.info("订单 {} 提前还款消息已投递，总还款金额：{}", orderId, totalEarlyAmount);
    }

    /*
     * 逾期检查任务
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void checkOverdue() {
        log.info("begin check overdue");
        List<Order> orders = orderMapper.selectUncompletedOrders();
        LocalDate today = LocalDate.now();

        for (Order order : orders) {
            List<RepaymentSchedule> schedules = repaymentScheduleMapper.selectByOrderId(order.getId());
            int overdueDays = 0;
            boolean hasOverdue = false;

            for (RepaymentSchedule schedule : schedules) {
                if ("未还".equals(schedule.getStatus())) {
                    LocalDate dueDate = schedule.getDueDate();
                    if (today.isAfter(dueDate)) {
                        long days = ChronoUnit.DAYS.between(dueDate, today);
                        overdueDays = Math.max(overdueDays, (int) days);
                        hasOverdue = true;
                        schedule.setStatus("逾期");
                        repaymentScheduleMapper.updateById(schedule);
                    }
                }
            }

            if (hasOverdue) {
                order.setOverdueDays(overdueDays);
                order.setStatus(OrderStatus.已逾期);
                orderMapper.update(order);
                log.info("订单 {} 已逾期 {} 天", order.getId(), overdueDays);
            }
        }
        log.info("check overdue done");
    }
}
