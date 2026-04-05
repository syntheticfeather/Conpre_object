package com.example.personal_loan.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
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

import com.example.personal_loan.dto.UserGetOrderResponse;
import com.example.personal_loan.dto.UserOrderListResponse;
import com.example.personal_loan.entity.LoanProduct;
import com.example.personal_loan.entity.Order;
import com.example.personal_loan.enums.OrderStatus;
import com.example.personal_loan.exception.BusinessException;
import com.example.personal_loan.mapper.LoanProductMapper;
import com.example.personal_loan.mapper.OrderMapper;
import com.example.personal_loan.service.impl.OrderServiceImpl;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private UserService userService;

    @Mock
    private LoanProductMapper loanProductMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order order;
    private LoanProduct loanProduct;
    private UserOrderListResponse orderListResponse;

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

        orderListResponse = new UserOrderListResponse();
        orderListResponse.setId(1L);
        orderListResponse.setLoanAmount(new BigDecimal("10000"));
        orderListResponse.setStatus(OrderStatus.正常);
        orderListResponse.setCurrentTerm(0);
        orderListResponse.setTerm(12);
        orderListResponse.setStartTime(LocalDateTime.now());
        orderListResponse.setOverdueDays(0);
    }

    @Test
    void testUserGetOrder_Success() {
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(loanProductMapper.findById(1L)).thenReturn(loanProduct);

        UserGetOrderResponse response = orderService.userGetOrder(1L, 1L);

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
        when(orderMapper.selectOrderListByUserId(1L)).thenReturn(Arrays.asList(orderListResponse));

        List<UserOrderListResponse> responses = orderService.userGetAllOrders(1L);

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals(1L, responses.get(0).getId());
    }

    @Test
    void testUserGetAllOrders_EmptyList() {
        when(orderMapper.selectOrderListByUserId(1L)).thenReturn(Collections.emptyList());

        List<UserOrderListResponse> responses = orderService.userGetAllOrders(1L);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    @Test
    void testRepay_Success_NormalOrder() {
        order.setCurrentTerm(5);
        order.setTerm(12);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(loanProductMapper.findById(1L)).thenReturn(loanProduct);

        orderService.repay(1L);

        assertEquals(6, order.getCurrentTerm());
        assertEquals(OrderStatus.正常, order.getStatus());
        verify(orderMapper).update(any(Order.class));
    }

    @Test
    void testRepay_Success_LastTerm() {
        order.setCurrentTerm(11);
        order.setTerm(12);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(loanProductMapper.findById(1L)).thenReturn(loanProduct);

        orderService.repay(1L);
        assertEquals(12, order.getCurrentTerm());
        assertEquals(OrderStatus.已完成, order.getStatus());
        verify(orderMapper).update(any(Order.class));
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
        when(loanProductMapper.findById(1L)).thenReturn(loanProduct);

        orderService.repay(1L);

        assertEquals(6, order.getCurrentTerm());
        assertEquals(OrderStatus.正常, order.getStatus());
        verify(orderMapper).update(any(Order.class));
    }

    @Test
    void testPostpone_Success() {
        order.setCurrentTerm(5);
        order.setTerm(12);
        when(orderMapper.selectById(1L)).thenReturn(order);

        Boolean result = orderService.postpone(1L);

        assertTrue(result);
        assertEquals(6, order.getCurrentTerm());
        assertEquals(13, order.getTerm());
        verify(orderMapper).update(any(Order.class));
    }

    @Test
    void testPostpone_OrderNotFound() {
        when(orderMapper.selectById(1L)).thenReturn(null);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            orderService.postpone(1L)
        );
        assertEquals(404, exception.getCode());
        assertTrue(exception.getMessage().contains("订单不存在"));
    }

    @Test
    void testPostpone_OrderCompleted() {
        order.setStatus(OrderStatus.已完成);
        when(orderMapper.selectById(1L)).thenReturn(order);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            orderService.postpone(1L)
        );
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("订单已结清，无法延期"));
    }

    @Test
    void testPostpone_OrderAlreadyPaidOff() {
        order.setCurrentTerm(12);
        order.setTerm(12);
        when(orderMapper.selectById(1L)).thenReturn(order);

        BusinessException exception = assertThrows(BusinessException.class, () -> 
            orderService.postpone(1L)
        );
        assertEquals(400, exception.getCode());
        assertTrue(exception.getMessage().contains("订单已结清，无法延期"));
    }

    @Test
    void testRepay_IncrementsRepaidAmount() {
        order.setCurrentTerm(5);
        order.setTerm(12);
        order.setRepaidAmount(new BigDecimal("5000"));
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(loanProductMapper.findById(1L)).thenReturn(loanProduct);

        BigDecimal initialRepaidAmount = order.getRepaidAmount();
        orderService.repay(1L);

        assertTrue(order.getRepaidAmount().compareTo(initialRepaidAmount) > 0);
    }

    @Test
    void testRepay_UpdatesStartTime() {
        order.setCurrentTerm(5);
        order.setTerm(12);
        LocalDateTime oldStartTime = order.getStartTime();
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(loanProductMapper.findById(1L)).thenReturn(loanProduct);

        orderService.repay(1L);

        assertEquals(oldStartTime, order.getStartTime());
    }

    @Test
    void testRepay_OverdueDaysRemainsZero() {
        order.setStatus(OrderStatus.已逾期);
        order.setOverdueDays(5);
        order.setCurrentTerm(5);
        order.setTerm(12);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(loanProductMapper.findById(1L)).thenReturn(loanProduct);

        orderService.repay(1L);

        assertEquals(0, order.getOverdueDays());
    }

    @Test
    void testUserGetAllOrders_MultipleOrders() {
        UserOrderListResponse order2 = new UserOrderListResponse();
        order2.setId(2L);
        order2.setLoanAmount(new BigDecimal("20000"));
        order2.setStatus(OrderStatus.已完成);
        order2.setCurrentTerm(12);
        order2.setTerm(12);
        order2.setStartTime(LocalDateTime.now());
        order2.setOverdueDays(0);

        when(orderMapper.selectOrderListByUserId(1L)).thenReturn(Arrays.asList(orderListResponse, order2));

        List<UserOrderListResponse> responses = orderService.userGetAllOrders(1L);

        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals(1L, responses.get(0).getId());
        assertEquals(2L, responses.get(1).getId());
    }

    @Test
    void testRepay_FirstTerm() {
        order.setCurrentTerm(0);
        order.setTerm(12);
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(loanProductMapper.findById(1L)).thenReturn(loanProduct);

        orderService.repay(1L);

        assertEquals(1, order.getCurrentTerm());
        verify(orderMapper).update(any(Order.class));
    }

    @Test
    void testPostpone_FirstTerm() {
        order.setCurrentTerm(0);
        order.setTerm(12);
        when(orderMapper.selectById(1L)).thenReturn(order);

        Boolean result = orderService.postpone(1L);

        assertTrue(result);
        assertEquals(1, order.getCurrentTerm());
        assertEquals(13, order.getTerm());
        verify(orderMapper).update(any(Order.class));
    }
}
