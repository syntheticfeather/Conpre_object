package com.example.personal_loan.service;

import java.util.List;

import com.example.personal_loan.dto.AdminGetOrderResponse;
import com.example.personal_loan.dto.UserGetOrderResponse;
import com.example.personal_loan.entity.Order;

public interface OrderService {
    // 用户获取单个贷款项目
    UserGetOrderResponse userGetOrder(Long userId, Long orderId);

    // 用户获取所有贷款项目
    List<UserGetOrderResponse> userGetAllOrders(Long userId);

    // 管理员获取单个订单
    AdminGetOrderResponse adminGetOrder(Long orderId);

    // 管理员根据用户ID获取该用户的所有订单
    List<AdminGetOrderResponse> adminGetAllOrdersByUser(Long userId);

    // 还款
    UserGetOrderResponse repay(Long orderId); 

    // 延期(加1期的时间)(先默认给人工审核)
    Boolean postpone(Long orderId);
}
