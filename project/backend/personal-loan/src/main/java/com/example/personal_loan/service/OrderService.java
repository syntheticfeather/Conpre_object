package com.example.personal_loan.service;

import java.util.List;

import com.example.personal_loan.dto.UserGetOrderResponse;
import com.example.personal_loan.dto.UserOrderListResponse;

public interface OrderService {
    // 用户获取单个订单详情
    UserGetOrderResponse userGetOrder(Long userId, Long orderId);

    // 用户获取所有订单列表
    List<UserOrderListResponse> userGetAllOrders(Long userId);

    // 还款
    UserGetOrderResponse repay(Long orderId); 

    // 延期(加1期的时间)(先默认给人工审核)
    Boolean postpone(Long orderId);
}
