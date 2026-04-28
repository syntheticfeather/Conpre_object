package com.example.personal_loan.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.personal_loan.entity.Order;
import com.example.personal_loan.entity.RepaymentSchedule;
import com.example.personal_loan.mapper.OrderMapper;
import com.example.personal_loan.mapper.RepaymentScheduleMapper;
import com.example.personal_loan.utils.CalculateUtil;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RepaymentScheduleService {

    @Autowired
    private RepaymentScheduleMapper repaymentScheduleMapper;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private CalculateUtil calculateUtil;

    /**
     * 生成还款计划
     * @param orderId 订单ID
     */
    @Transactional
    public void generateRepaymentSchedule(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        LocalDate startDate = order.getStartTime().toLocalDate();

        List<RepaymentSchedule> plan = calculateUtil.calculateRepaymentPlan(
            order.getLoanAmount(),
            order.getInterestRate(),
            order.getTerm(),
            order.getRepaidType(),
            startDate
        );

        for (RepaymentSchedule item : plan) {
            RepaymentSchedule schedule = new RepaymentSchedule(
                null,
                orderId,
                item.getTerm(),
                item.getPrincipal(),
                item.getInterest(),
                item.getTotalAmount(),
                "未还",
                item.getRemainingPrincipal(),
                item.getRemainingInterest(),
                item.getDueDate(),
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
            );
            repaymentScheduleMapper.insert(schedule);
        }

        log.info("for order {} to generate {} schedules", orderId, plan.size());
    }

    /**
     * 延期后修改还款计划
     * @param orderId 订单ID
     * @param term 指定期数
     */
    @Transactional
    public void updateDueDateAfterPostpone(Long orderId, Integer term) {
        List<RepaymentSchedule> schedules = repaymentScheduleMapper.selectByOrderId(orderId);
        RepaymentSchedule targetSchedule = null;

        // 1. 找到目标期数（建议直接用 Stream 或 Mapper 层查询，减少循环）
        for (RepaymentSchedule schedule : schedules) {
            if (schedule.getTerm().equals(term)) {
                targetSchedule = schedule;
                break;
            }
        }

        if (targetSchedule == null) {
            throw new IllegalArgumentException("未找到指定期数的还款计划");
        }

        // 2. 计算目标期数的新还款日期（直接加1个月，Java会自动处理月末情况）
        LocalDate targetOldDueDate = targetSchedule.getDueDate();
        // 核心修改：直接用 plusMonths，抛弃手动计算天数和月末判断
        LocalDate targetNewDueDate = targetOldDueDate.plusMonths(1); 

        // 3. 更新目标期数
        targetSchedule.setDueDate(targetNewDueDate);
        targetSchedule.setUpdatedAt(LocalDateTime.now());
        repaymentScheduleMapper.updateById(targetSchedule);
        // 4. 顺延之后的每一期（同样直接加1个月）
        for (RepaymentSchedule schedule : schedules) {
            if (schedule.getTerm() > term) {
                LocalDate oldDueDate = schedule.getDueDate();
                // 核心修改：后续期数统一往后推1个月
                LocalDate newDueDate = oldDueDate.plusMonths(1); 
                
                schedule.setDueDate(newDueDate);
                schedule.setUpdatedAt(LocalDateTime.now());
                repaymentScheduleMapper.updateById(schedule);
            }
        }
    }

    public List<RepaymentSchedule> getRepaymentSchedule(Long orderId) {
        return repaymentScheduleMapper.selectByOrderId(orderId);
    }
}
