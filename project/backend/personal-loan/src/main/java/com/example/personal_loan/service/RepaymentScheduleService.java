package com.example.personal_loan.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.personal_loan.entity.RepaymentSchedule;
import com.example.personal_loan.exception.BusinessException;
import com.example.personal_loan.mapper.RepaymentScheduleMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RepaymentScheduleService {

    @Autowired
    private RepaymentScheduleMapper repaymentScheduleMapper;

    /**
     * 生成还款计划（调用存储过程，事务由 SP 内部管理）
     * @param orderId 订单ID
     */
    public void generateRepaymentSchedule(Long orderId) {
        Map<String, Object> params = new HashMap<>();
        params.put("orderId", orderId);
        repaymentScheduleMapper.callGenerateRepaymentSchedule(params);
        Integer success = (Integer) params.get("success");
        if (success == null || success != 1) {
            throw new BusinessException("生成还款计划失败");
        }
        log.info("存储过程生成还款计划完成, orderId={}", orderId);
    }

    /**
     * 延期后修改还款计划（调用存储过程，事务由 SP 内部管理）
     * @param orderId 订单ID
     * @param term    申请延期的当期期数
     */
    public void updateDueDateAfterPostpone(Long orderId, Integer term) {
        Map<String, Object> params = new HashMap<>();
        params.put("orderId", orderId);
        params.put("term", term);
        repaymentScheduleMapper.callUpdateDueDateAfterPostpone(params);
        Integer success = (Integer) params.get("success");
        if (success == null || success != 1) {
            throw new BusinessException("延期更新还款计划失败");
        }
        log.info("存储过程更新到期日完成, orderId={}, term={}", orderId, term);
    }

    public List<RepaymentSchedule> getRepaymentSchedule(Long orderId) {
        return repaymentScheduleMapper.selectByOrderId(orderId);
    }
}
