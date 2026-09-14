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
import com.example.personal_loan.dto.OrderStatisticsResponse;
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
import com.example.personal_loan.service.MlTrainingLogService;
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
    private MlTrainingLogService mlTrainingLogService;

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

        // 计算应还总额、待还总额、待还本金、待还利息
        List<RepaymentSchedule> schedules = repaymentScheduleMapper.selectByOrderId(orderId);
        BigDecimal totalAmountDue = BigDecimal.ZERO;
        BigDecimal outstandingAmount = BigDecimal.ZERO;
        BigDecimal outstandingPrincipal = BigDecimal.ZERO;
        BigDecimal outstandingInterest = BigDecimal.ZERO;
        for (RepaymentSchedule schedule : schedules) {
            totalAmountDue = totalAmountDue.add(schedule.getTotalAmount());
            if (!"已还".equals(schedule.getStatus())) {
                outstandingPrincipal = outstandingPrincipal.add(schedule.getPrincipal());
                outstandingInterest = outstandingInterest.add(schedule.getInterest());
                outstandingAmount = outstandingAmount.add(schedule.getPrincipal()).add(schedule.getInterest());
            }
        }

        UserGetOrderResponse response = new UserGetOrderResponse();
        response.setProductName(productName);
        response.setOrder(order);
        response.setTotalAmountDue(totalAmountDue);
        response.setOutstandingAmount(outstandingAmount);
        response.setOutstandingPrincipal(outstandingPrincipal);
        response.setOutstandingInterest(outstandingInterest);
        return response;
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
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (order.getCurrentTerm() >= order.getTerm()) {
            throw new BusinessException(400, "订单已结清");
        }
        OrderStatus status = order.getStatus();
        if (status == OrderStatus.已完成) {
            throw new BusinessException(400, "订单不可还款");
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
                    LocalDateTime.now(),
                    null
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
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (order.getCurrentTerm() >= order.getTerm()) {
            throw new BusinessException(400,"订单已结清，无法延期");
        }
        OrderStatus status = order.getStatus();
        if (status == OrderStatus.已完成) {
            throw new BusinessException(400,"订单已结清，无法延期");
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
     * @param periods 提前还款期数，null 表示结清剩余全部期数
     */
    @Override
    @Transactional
    public void earlyRepay(Long orderId, Integer periods) {
        // 检查订单状态
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (order.getCurrentTerm() >= order.getTerm()) {
            throw new BusinessException("订单已结清，无需提前还款");
        }

        int remaining = order.getTerm() - order.getCurrentTerm();
        // 校验期数：null 表示全部结清，否则必须在 1 ~ remaining 之间
        int repayPeriods;
        if (periods == null) {
            repayPeriods = remaining;
        } else {
            if (periods < 1) {
                throw new BusinessException(400, "提前还款期数必须大于0");
            }
            if (periods > remaining) {
                throw new BusinessException(400, "提前还款期数不能超过剩余期数(" + remaining + ")");
            }
            repayPeriods = periods;
        }

        // 检查还款计划，计算本次提前还款的总金额
        List<RepaymentSchedule> schedules = repaymentScheduleMapper.selectByOrderId(orderId);
        int endTerm = order.getCurrentTerm() + repayPeriods;
        BigDecimal totalEarlyAmount = BigDecimal.ZERO;
        for (RepaymentSchedule schedule : schedules) {
            if (schedule.getTerm() > order.getCurrentTerm() && schedule.getTerm() <= endTerm) {
                totalEarlyAmount = totalEarlyAmount.add(schedule.getPrincipal()).add(schedule.getInterest());
            }
        }
        if (totalEarlyAmount.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException("未找到待还的还款计划");
        }
        // 投递还款消息
        try {
            PaymentRequestedEvent event = new PaymentRequestedEvent(
                    orderId,
                    totalEarlyAmount,
                    LocalDateTime.now(),
                    repayPeriods
            );
            OutboxMessage outboxMessage = outboxMessageFactory.create(BusinessType.PAYMENT_REQUESTED, event, orderId);
            outboxMapper.insert(outboxMessage);
        } catch (Exception e) {
            log.error("写入outbox表失败", e);
            throw new BusinessException("消息投递失败");
        }
        log.info("订单 {} 提前还款消息已投递，提前还款期数：{}，总还款金额：{}", orderId, repayPeriods, totalEarlyAmount);
    }

    /*
     * 统计用户所有订单的待还总额、待还本金、待还利息
     */
    @Override
    public OrderStatisticsResponse getUserOrderStatistics(Long userId) {
        OrderStatisticsResponse statistics = repaymentScheduleMapper.sumOutstandingByUserId(userId);
        if (statistics == null) {
            statistics = new OrderStatisticsResponse(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        return statistics;
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

                // 🆕 逾期≥90天 → 标记为违约
                if (overdueDays >= 90 && order.getApplicationId() != null) {
                    mlTrainingLogService.markDefaulted(order.getApplicationId());
                }
            }
        }
        log.info("check overdue done");
    }
}
