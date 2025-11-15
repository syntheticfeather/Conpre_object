package com.example.personal_loan.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.personal_loan.mapper.OrderMapper;
import com.example.personal_loan.entity.Order;
import com.example.personal_loan.enums.OrderStatus;
import com.example.personal_loan.exception.BusinessException;
import com.example.personal_loan.service.AuthService;
import com.example.personal_loan.service.OrderService;
import com.example.personal_loan.service.UserService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Service
@Transactional
public class OrderServiceImpl implements OrderService{
    
    @Autowired
    private OrderMapper orderMapper;
    
    @Autowired
    private UserService userService;
    
    // @Autowired
    // private LoanProductService loanProductService;
    
    // @Autowired
    // private PayService payService;
    
    @Autowired
    private AuthService authService;
    
    
    // 获取用户的单个贷款项目
    @Override
    public Order getOrder(Long userId, Long orderId) {
        // 验证用户存在
        if (userService.getUserById(userId)== null) {
            throw new BusinessException("用户不存在");
        }
        
        Order order = orderMapper.selectByIdAndUserId(orderId, userId);
        if (order == null) {
            throw new BusinessException("订单不存在或不属于该用户");
        }
        
        return order;
    }
    
    // 获取用户所有贷款项目
    @Override
    public List<Order> getOrders(Long userId) {
        // 验证用户存在
        if (userService.getUserById(userId)== null) {
            throw new BusinessException("用户不存在");
        }
        
        return orderMapper.selectAllByUserId(userId);
    }
    
    // 还款
    @Override
    public Order repay(Long orderId) {

        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        if ("SETTLED".equals(order.getStatus().name())) {
            throw new BusinessException("订单已结清，无需还款");
        }
        
        // 调用内部方法计算还款金额
        RepaymentResult repaymentResult = calculateRepaymentAmount(order);
        
        // 更新订单信息(已还，未还，当前期数)
        BigDecimal newRepaidAmount = order.getRepaidAmount().add(repaymentResult.getCurrentRepayment());
        BigDecimal newOutstandingAmount = order.getOutstandingAmount().subtract(repaymentResult.getCurrentRepayment());
        Integer newCurrentTerm = order.getCurrentTerm() + 1;
        
        order.setRepaidAmount(newRepaidAmount);
        order.setOutstandingAmount(newOutstandingAmount);
        order.setCurrentTerm(newCurrentTerm);
        
        // 检查是否已完全还清
        if (newCurrentTerm >= order.getLoanPeriod() || newOutstandingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            order.setStatus(OrderStatus.SETTLED);
            order.setEndDate(LocalDateTime.now());
        }
        
        orderMapper.updateForRepayment(order.getId(), newRepaidAmount, newOutstandingAmount, newCurrentTerm);
        orderMapper.updateStatus(order.getId(), order.getStatus().name());
        
        return order;
    }
    
    // 延期申请（加1期的时间，默认人工审核）
    @Override
    public Boolean postpone(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        if ("SETTLED".equals(order.getStatus().name())) {
            throw new BusinessException("订单已结清，无法延期");
        }
        
        if ("OVERDUE".equals(order.getStatus().name())) {
            throw new BusinessException("逾期订单无法申请延期");
        }
        
        // 调用审核服务,进行延期审批
        if (authService.approve(orderId)) {
            orderMapper.updateForPostpone(orderId);
            return true;
        }else{
            return false;
        }
    }
    
    
    
    //内部类：还款计算结果
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class RepaymentResult {
        private BigDecimal currentRepayment;    // 本期应还金额
        private BigDecimal principal;           // 本期本金
        private BigDecimal interest;            // 本期利息
    }

    // 根据还款方式计算还款金额 - 内部方法
    private RepaymentResult calculateRepaymentAmount(Order order) {
        String repaidType = order.getRepaidType();
        BigDecimal outstandingAmount = order.getOutstandingAmount();
        Integer currentTerm = order.getCurrentTerm();
        Integer totalTerms = order.getLoanPeriod();
        BigDecimal annualInterestRate = order.getInterestRate();
        
        switch (repaidType) {
            case "等额本息":
                return calculateEqualInstallment(outstandingAmount, currentTerm, totalTerms, annualInterestRate);
            case "等额本金":
                return calculateEqualPrincipal(outstandingAmount, currentTerm, totalTerms, annualInterestRate);
            case "先息后本":
                return calculateInterestFirst(outstandingAmount, currentTerm, totalTerms, annualInterestRate);
            case "一次性还本付息":
                return calculateBulletPayment(outstandingAmount, currentTerm, totalTerms, annualInterestRate);
            default:
                throw new BusinessException("不支持的还款方式: " + repaidType);
        }
    }
    
    // 等额本息计算 - 内部方法
    private RepaymentResult calculateEqualInstallment(BigDecimal totalAmount, Integer currentTerm, 
                                                     Integer totalTerms, BigDecimal annualRate) {
        if (currentTerm >= totalTerms) {
            return new RepaymentResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(12), 8, RoundingMode.HALF_UP);
        
        // 每月还款额 = [P × r × (1+r)^n] ÷ [(1+r)^n - 1]
        BigDecimal base = BigDecimal.ONE.add(monthlyRate);
        BigDecimal power = pow(base, totalTerms);
        
        BigDecimal monthlyPayment = totalAmount
            .multiply(monthlyRate)
            .multiply(power)
            .divide(power.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
        
        // 计算本期利息和本金
        BigDecimal remainingPrincipal = calculateRemainingPrincipal(totalAmount, monthlyPayment, monthlyRate, currentTerm);
        BigDecimal interest = remainingPrincipal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal principal = monthlyPayment.subtract(interest);
        
        return new RepaymentResult(monthlyPayment, principal, interest);
    }
    
    // 等额本金计算 - 内部方法
    private RepaymentResult calculateEqualPrincipal(BigDecimal totalAmount, Integer currentTerm, 
                                                   Integer totalTerms, BigDecimal annualRate) {
        if (currentTerm >= totalTerms) {
            return new RepaymentResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(12), 8, RoundingMode.HALF_UP);
        BigDecimal monthlyPrincipal = totalAmount.divide(BigDecimal.valueOf(totalTerms), 2, RoundingMode.HALF_UP);
        BigDecimal paidPrincipal = monthlyPrincipal.multiply(BigDecimal.valueOf(currentTerm));
        BigDecimal remainingPrincipal = totalAmount.subtract(paidPrincipal);
        BigDecimal interest = remainingPrincipal.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal currentRepayment = monthlyPrincipal.add(interest);
        
        return new RepaymentResult(currentRepayment, monthlyPrincipal, interest);
    }
    
    // 先息后本计算 - 内部方法
    private RepaymentResult calculateInterestFirst(BigDecimal totalAmount, Integer currentTerm, 
                                                  Integer totalTerms, BigDecimal annualRate) {
        BigDecimal monthlyRate = annualRate.divide(BigDecimal.valueOf(12), 8, RoundingMode.HALF_UP);
        BigDecimal interest = totalAmount.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
        
        if (currentTerm == totalTerms - 1) {
            return new RepaymentResult(totalAmount.add(interest), totalAmount, interest);
        } else {
            return new RepaymentResult(interest, BigDecimal.ZERO, interest);
        }
    }
    
    // 一次性还本付息 - 内部方法
    private RepaymentResult calculateBulletPayment(BigDecimal totalAmount, Integer currentTerm, 
                                                 Integer totalTerms, BigDecimal annualRate) {
        if (currentTerm < totalTerms - 1) {
            return new RepaymentResult(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        
        BigDecimal totalInterest = totalAmount
            .multiply(annualRate)
            .multiply(BigDecimal.valueOf(totalTerms))
            .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
        
        BigDecimal totalRepayment = totalAmount.add(totalInterest);
        
        return new RepaymentResult(totalRepayment, totalAmount, totalInterest);
    }
    
    // 计算剩余本金 - 内部工具方法
    private BigDecimal calculateRemainingPrincipal(BigDecimal totalAmount, BigDecimal monthlyPayment, 
                                                 BigDecimal monthlyRate, Integer currentTerm) {
        BigDecimal remaining = totalAmount;
        for (int i = 0; i < currentTerm; i++) {
            BigDecimal interest = remaining.multiply(monthlyRate);
            BigDecimal principal = monthlyPayment.subtract(interest);
            remaining = remaining.subtract(principal);
        }
        return remaining;
    }
    
    // 幂运算 - 内部工具方法
    private BigDecimal pow(BigDecimal base, int exponent) {
        BigDecimal result = BigDecimal.ONE;
        for (int i = 0; i < exponent; i++) {
            result = result.multiply(base);
        }
        return result;
    }
}