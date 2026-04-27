package com.example.personal_loan.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.personal_loan.entity.LoanProduct;
import com.example.personal_loan.entity.Order;
import com.example.personal_loan.entity.OutboxMessage;
import com.example.personal_loan.enums.OrderStatus;
import com.example.personal_loan.exception.BusinessException;
import com.example.personal_loan.mapper.LoanProductMapper;
import com.example.personal_loan.mapper.OrderMapper;
import com.example.personal_loan.mapper.OutboxMapper;
import com.example.personal_loan.service.impl.OrderServiceImpl;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private LoanProductMapper loanProductMapper;

    @Mock
    private OutboxMapper outboxMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order order;
    private LoanProduct loanProduct;

    @BeforeEach
    void setUp() {
        loanProduct = new LoanProduct();
        loanProduct.setId(1L);
        loanProduct.setProductName("个人消费贷");

        order = new Order();
        order.setId(1L);
        order.setUserId(1L);
        order.setProductId(1L);
        order.setStatus(OrderStatus.正常);
        order.setLoanAmount(new BigDecimal("10000"));
        order.setInterestRate(new BigDecimal("0.05"));
        order.setRepaidType(com.example.personal_loan.enums.RepaidType.等额本息);
        order.setLoanPeriod(12);
        order.setTerm(12);
        order.setCurrentTerm(0);
        order.setRepaidAmount(BigDecimal.ZERO);
        order.setOverdueDays(0);
        order.setStartTime(LocalDateTime.now());
    }

    @Test
    void testUserGetOrder_Success() {
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(loanProductMapper.findById(1L)).thenReturn(loanProduct);

        var response = orderService.userGetOrder(1L, 1L);

        assertNotNull(response);
        assertNotNull(response.getOrder());
        assertEquals("个人消费贷", response.getProductName());
        assertEquals(1L, response.getOrder().getId());
        assertEquals(new BigDecimal("10000"), response.getOrder().getLoanAmount());
        assertEquals(OrderStatus.正常, response.getOrder().getStatus());
    }

    @Test
    void testUserGetOrder_OrderNotFound() {
        when(orderMapper.selectById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            orderService.userGetOrder(1L, 1L)
        );
        assertEquals(404, exception.getCode());
        assertTrue(exception.getMessage().contains("订单不存在"));
    }

    @Test
    void testUserGetOrder_NotOwner() {
        order.setUserId(2L);
        when(orderMapper.selectById(1L)).thenReturn(order);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            orderService.userGetOrder(1L, 1L)
        );
        assertEquals(403, exception.getCode());
        assertTrue(exception.getMessage().contains("无权查看他人订单"));
    }

    @Test
    void testUserGetAllOrders_Success() {
        when(orderMapper.selectOrderListByUserId(1L)).thenReturn(Arrays.asList());

        List<?> responses = orderService.userGetAllOrders(1L);

        assertNotNull(responses);
    }

    @Test
    void testRepay_Success_NormalOrder() {
        order.setCurrentTerm(5);
        order.setTerm(12);
        when(orderMapper.selectById(1L)).thenReturn(order);

        orderService.repay(1L);

        verify(outboxMapper).insert(any(OutboxMessage.class));
    }

    @Test
    void testRepay_OrderNotFound() {
        when(orderMapper.selectById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            orderService.repay(1L)
        );
        assertEquals(404, exception.getCode());
        assertTrue(exception.getMessage().contains("订单不存在"));
    }

    @Test
    void testRepay_OrderCompleted() {
        order.setStatus(OrderStatus.已完成);
        when(orderMapper.selectById(1L)).thenReturn(order);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            orderService.repay(1L)
        );
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("订单不可还款"));
    }

    @Test
    void testRepay_OrderAlreadyPaidOff() {
        order.setCurrentTerm(12);
        order.setTerm(12);
        when(orderMapper.selectById(1L)).thenReturn(order);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            orderService.repay(1L)
        );
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("订单已结清"));
    }

    @Test
    void testRepay_OverdueOrder() {
        order.setStatus(OrderStatus.已逾期);
        order.setCurrentTerm(5);
        order.setTerm(12);
        when(orderMapper.selectById(1L)).thenReturn(order);

        orderService.repay(1L);

        verify(outboxMapper).insert(any(OutboxMessage.class));
    }

    @Test
    void testPostpone_OrderNotFound() {
        when(orderMapper.selectById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            orderService.postpone(1L)
        );
        assertTrue(exception.getMessage().contains("订单不存在"));
    }

    @Test
    void testPostpone_OrderCompleted() {
        order.setStatus(OrderStatus.已完成);
        when(orderMapper.selectById(1L)).thenReturn(order);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            orderService.postpone(1L)
        );
        assertTrue(exception.getMessage().contains("订单已结清，无法延期"));
    }

    @Test
    void testRepay_FirstTerm() {
        order.setCurrentTerm(0);
        order.setTerm(12);
        when(orderMapper.selectById(1L)).thenReturn(order);

        orderService.repay(1L);

        verify(outboxMapper).insert(any(OutboxMessage.class));
    }
}
