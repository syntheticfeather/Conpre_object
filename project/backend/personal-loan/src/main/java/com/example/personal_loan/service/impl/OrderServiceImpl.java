package com.example.personal_loan.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.personal_loan.dto.AdminGetOrderResponse;
import com.example.personal_loan.dto.UserGetOrderResponse;
import com.example.personal_loan.entity.LoanProduct;
import com.example.personal_loan.entity.Order;
import com.example.personal_loan.entity.User;
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
    
    // 用户获取单个订单
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

        // 构建 contractUrl（安全路径）
        // String contractUrl = null;
        // if (order.getContract() != null && !order.getContract().isEmpty()) {
        //     contractUrl = "/api/orders/" + orderId + "/contract";
        // }

        // 构建响应 DTO
        return new UserGetOrderResponse(
            order.getId(),
            productName, 
            order.getRepaidAmount(),
            order.getLoanAmount(), 
            order.getInterestRate(), 
            order.getLoanPeriod(),  
            order.getRepaidType(),
            order.getStartTime(),
            order.getStatus(),  
            (order.getStatus() == OrderStatus.已逾期)  ? order.getOverdueDays() : null, // overdueDays
            order.getTerm(),                // term（总期数）
            order.getCurrentTerm(),         // currentTerm
            null                    // contractUrl
        );
    }
    
    // 用户获取所有订单
    @Override
    public List<UserGetOrderResponse> userGetAllOrders(Long userId) {
        List<Order> orders = orderMapper.selectAllByUserId(userId);
    
        return orders.stream().map(order -> {
            LoanProduct product = loanProductMapper.findById(order.getProductId());
            String productName = product.getProductName();
            
            String contractUrl = null;  // 存储合同还未实现

            return new UserGetOrderResponse(
                order.getId(),
                productName,
                order.getRepaidAmount(),
                order.getLoanAmount(),
                order.getInterestRate(),
                order.getLoanPeriod(),
                order.getRepaidType(),
                order.getStartTime(),
                order.getStatus(),
                (order.getStatus() == OrderStatus.已逾期)  ? order.getOverdueDays() : null,
                order.getTerm(),
                order.getCurrentTerm(),
                contractUrl
            );
        }).collect(Collectors.toList());
    }
    
    // 管理员获取单个订单
    @Override
    public AdminGetOrderResponse adminGetOrder(Long orderId){
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }

        LoanProduct product = loanProductMapper.findById(order.getProductId());
        User user = userService.getUserById(order.getUserId());

        String productName = product.getProductName();
        String contractUrl = null;

        return new AdminGetOrderResponse(
            order.getId(),
            order.getUserId(),
            productName,
            order.getRepaidAmount(),
            order.getLoanAmount(),
            order.getInterestRate(),
            order.getLoanPeriod(),
            order.getTerm(),
            order.getRepaidType(),
            order.getStartTime(),
            order.getStatus(),
            (order.getStatus() == OrderStatus.已逾期)  ? order.getOverdueDays() : null,
            order.getCurrentTerm(),
            contractUrl
        );
    }

    // 管理员获取指定用户所有订单
    @Override
    public List<AdminGetOrderResponse> adminGetAllOrdersByUser(Long userId){
        List<Order> orders = orderMapper.selectAllByUserId(userId);
        if (orders.isEmpty()) {
            throw new BusinessException(404,"该用户无贷款记录");
        }

        User user = userService.getUserById(userId);

        List<AdminGetOrderResponse> result = new ArrayList<>();

        for (Order order : orders) {
            LoanProduct product = loanProductMapper.findById(order.getProductId());
            String productName = (product != null) ? product.getProductName() : "未知产品";

            String contractUrl = null; // 存储合同还未实现

            AdminGetOrderResponse dto = new AdminGetOrderResponse(
                order.getId(),
                order.getUserId(),
                productName,
                order.getRepaidAmount(),
                order.getLoanAmount(),
                order.getInterestRate(),
                order.getLoanPeriod(),
                order.getTerm(),
                order.getRepaidType(),
                order.getStartTime(),
                order.getStatus(),
                (order.getStatus() == OrderStatus.已逾期)  ? order.getOverdueDays() : null,
                order.getCurrentTerm(),
                contractUrl
            );
            result.add(dto);
        }

        return result;
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

        return new UserGetOrderResponse(
            order.getId(),
            productName,
            order.getRepaidAmount(),
            order.getLoanAmount(),
            order.getInterestRate(),
            order.getLoanPeriod(),
            order.getRepaidType(),
            order.getStartTime(),
            order.getStatus(),
            (order.getStatus() == OrderStatus.已逾期)  ? order.getOverdueDays() : null,
            order.getTerm(),
            order.getCurrentTerm(),
            contractUrl
        );
    }
    
    // 延期申请（加1期的时间，默认人工审核）【未实现】
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
        
        return true;
    }
    
    
    
}