package com.example.personal_loan.service;

public interface ApplicationService {
    void addApplication(Long userId, Long proId);  // 添加订单

    void deleteApplication(Long userId, Long proId); // 删除订单

    void updateApplication(Long userId, Long proId); // 更新订单

    // Order getOrder(Long userId, Long proId); // 获取用户的单个订单

    // List<Order> getOrders(Long userId); // 获取用户所有订单

}
