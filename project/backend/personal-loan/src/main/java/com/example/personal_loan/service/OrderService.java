package com.example.personal_loan.service;

import java.util.List;

import com.example.personal_loan.dto.UserGetOrderResponse;
import com.example.personal_loan.dto.UserOrderListResponse;

public interface OrderService {
    UserGetOrderResponse userGetOrder(Long userId, Long orderId);
    // 用户获取单个订单详情

    List<UserOrderListResponse> userGetAllOrders(Long userId);
    // 用户获取所有订单列表

    void repay(Long orderId);

    Boolean postpone(Long orderId);

    void earlyRepay(Long orderId);
}
