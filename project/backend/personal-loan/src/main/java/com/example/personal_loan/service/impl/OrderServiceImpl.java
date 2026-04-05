package com.example.personal_loan.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.personal_loan.aop.RedisLocked;
import com.example.personal_loan.config.RabbitMQConfig;
import com.example.personal_loan.dto.PaymentRequestedEvent;
import com.example.personal_loan.dto.UserGetOrderResponse;
import com.example.personal_loan.dto.UserOrderListResponse;
import com.example.personal_loan.entity.LoanProduct;
import com.example.personal_loan.entity.Order;
import com.example.personal_loan.entity.OutboxMessage;
import com.example.personal_loan.enums.OrderStatus;
import com.example.personal_loan.exception.BusinessException;
import com.example.personal_loan.mapper.LoanProductMapper;
import com.example.personal_loan.mapper.OrderMapper;
import com.example.personal_loan.mapper.OutboxMapper;
import com.example.personal_loan.service.OrderService;
import com.example.personal_loan.utils.CalculateUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional
public class OrderServiceImpl implements OrderService{
    
    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private LoanProductMapper loanProductMapper;

    @Autowired
    private OutboxMapper outboxMapper;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    public UserGetOrderResponse userGetOrder(Long userId, Long orderId){
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权查看他人订单");
        }
        LoanProduct product = loanProductMapper.findById(order.getProductId());
        String productName = product.getProductName();
        return new UserGetOrderResponse(productName, order);
    }
    
    @Override
    public List<UserOrderListResponse> userGetAllOrders(Long userId) {
        return orderMapper.selectOrderListByUserId(userId);
    }
    
    @Override
    @RedisLocked(key = "'lock:order:repay:' + #p0")
    public void repay(Long orderId) {
        // 查找订单
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(404, "订单不存在");
        }
        // 获取订单状态
        OrderStatus status = order.getStatus();
        // 检查订单状态是否逾期
        if (status != OrderStatus.正常 && status != OrderStatus.已逾期) {
            throw new BusinessException(400, "订单不可还款，当前状态：" + status);
        }
        // 检查订单是否已结清
        if (order.getCurrentTerm() >= order.getTerm()) {
            throw new BusinessException(400, "订单已结清，无需还款");
        }
        // 计算当前期应还款金额
        BigDecimal dueAmount = CalculateUtil.calculateCurrentTermPayment(order);
        // 发送还款请求事件，写Outbox
        PaymentRequestedEvent event = new PaymentRequestedEvent(order.getId(), dueAmount, LocalDateTime.now());
        OutboxMessage outbox = new OutboxMessage();
        outbox.setMessageId("payment_requested_" + order.getId() + "_" + System.currentTimeMillis());
        outbox.setBusinessType("PAYMENT_REQUESTED");
        outbox.setBusinessId(order.getId());
        outbox.setTopic(RabbitMQConfig.PAYMENT_REQUESTED_ROUTING_KEY);
        try {
            outbox.setPayload(objectMapper.writeValueAsString(event));
        } catch (Exception ex) {
            throw new BusinessException(500, "消息序列化失败");
        }
        outbox.setStatus("PENDING");
        outbox.setCreatedAt(LocalDateTime.now());
        outboxMapper.insert(outbox);
    }
    
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
