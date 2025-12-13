package com.example.personal_loan.service.impl;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.personal_loan.dto.UserGetOrderResponse;
import com.example.personal_loan.dto.UserOrderListResponse;
import com.example.personal_loan.entity.LoanProduct;
import com.example.personal_loan.entity.Order;
import com.example.personal_loan.enums.OrderStatus;
import com.example.personal_loan.exception.BusinessException;
import com.example.personal_loan.mapper.LoanProductMapper;
import com.example.personal_loan.mapper.OrderMapper;
import com.example.personal_loan.service.OrderService;
import com.example.personal_loan.service.UserService;
import com.example.personal_loan.utils.CalculateUtil;

@Service
@Transactional
public class OrderServiceImpl implements OrderService{
    
    @Autowired
    private OrderMapper orderMapper;
    
    @Autowired
    private UserService userService;

    @Autowired
    private LoanProductMapper loanProductMapper;
    
    // 用户获取单个订单详情
    @Override
    public UserGetOrderResponse userGetOrder(Long userId, Long orderId){
        // 查询订单
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        // 权限校验
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权查看他人订单");
        }
        // 查询产品名称
        LoanProduct product = loanProductMapper.findById(order.getProductId());
        String productName = product.getProductName();
        // 构建响应 DTO
        return new UserGetOrderResponse(productName, order);
    }
    
    // 用户获取订单列表
    @Override
    public List<UserOrderListResponse> userGetAllOrders(Long userId) {
        return orderMapper.selectOrderListByUserId(userId);
    }
    

    // 还款
    @Override
    public UserGetOrderResponse repay(Long orderId) {
        // 1. 查询订单
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }

        // 2. 校验状态
        OrderStatus status = order.getStatus();
        if (status != OrderStatus.正常 && status != OrderStatus.已逾期) {
            throw new BusinessException(400, "订单不可还款，当前状态：" + status);
        }

        // 3. 校验是否已还完
        if (order.getCurrentTerm() >= order.getTerm()) {
            throw new BusinessException(400, "订单已结清，无需还款");
        }

        // 4. 计算本期应还金额
        BigDecimal dueAmount = CalculateUtil.calculateCurrentTermPayment(order);

        // 5. 【模拟】用户支付成功（实际应调用支付网关）
        // 此处假设支付成功，直接更新数据

        // 6. 更新订单
        BigDecimal newRepaidAmount = order.getRepaidAmount().add(dueAmount);
        Integer newCurrentTerm = order.getCurrentTerm() + 1;
        OrderStatus newStatus = (newCurrentTerm.equals(order.getTerm())) ? OrderStatus.已完成 : OrderStatus.正常;

        order.setRepaidAmount(newRepaidAmount);
        order.setCurrentTerm(newCurrentTerm);
        order.setStatus(newStatus);
        // order.setUpdateTime(LocalDateTime.now());   ????

        // 7. 保存到数据库
        orderMapper.update(order);

        // 8. 构建返回 DTO（复用 userGetOrder 的逻辑）
        LoanProduct product = loanProductMapper.findById(order.getProductId());
        String productName = product.getProductName();

        String contractUrl =null; // 存储合同还未实现

        return new UserGetOrderResponse(productName,order);
    }
    
    // 延期申请（加1期的时间，默认人工审核）【未实现】
    @Override
    public Boolean postpone(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        
        if ("正常".equals(order.getStatus().name())) {
            throw new BusinessException("订单已结清，无法延期");
        }
        
        if ("已逾期".equals(order.getStatus().name())) {
            throw new BusinessException("逾期订单无法申请延期");
        }
        
        return true;
    }
}