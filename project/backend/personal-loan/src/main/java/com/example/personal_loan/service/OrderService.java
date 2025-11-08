package com.example.personal_loan.service;

import java.util.List;

import com.example.personal_loan.entity.Order;

public interface OrderService {
    // 获取用户的单个贷款项目
    Order getOrder(Long userId, Long orderId); 

    // 获取用户所有贷款项目
    List<Order> getOrders(Long userId);

    // 还款
    Order repay(Long orderId); 

    // 延期(加1期的时间)(先默认给人工审核)
    Boolean postpone(Long orderId);
}
