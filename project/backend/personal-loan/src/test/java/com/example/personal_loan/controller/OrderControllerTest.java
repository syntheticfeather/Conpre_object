package com.example.personal_loan.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.personal_loan.handler.JwtInterceptor;
import com.example.personal_loan.service.OrderService;
import com.example.personal_loan.service.UserService;
import com.example.personal_loan.utils.JwtUtil;

@WebMvcTest(OrderController.class)
@AutoConfigureMockMvc
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtInterceptor jwtInterceptor;

    @MockBean
    private UserService userService;

    // getOrder成功的测试
    // @Test
    // void getOrder_shouldReturnOrder_whenValidOrderIdAndUser() throws Exception {
    //     Long userId = 1L;
    //     Long orderId = 1L;
    //     Order mockOrder = new Order();
    //     mockOrder.setId(orderId);
    //     mockOrder.setUserId(userId);

    //     when(orderService.getOrder(userId, orderId)).thenReturn(mockOrder);

    //     mockMvc.perform(get("/orders/{orderId}", orderId)
    //                     .requestAttr("userId", userId))  // 默认通过拦截器
    //             .andDo(print()) // 关键：打印请求和响应详情
    //             .andExpect(status().isOk())
    //             .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    //             .andExpect(jsonPath("$.id").value(orderId))
    //             .andExpect(jsonPath("$.userId").value(userId));

    //     verify(orderService).getOrder(userId, orderId);
    // }

    // @Test
    // void getOrders_shouldReturnListOfOrders_whenUserHasOrders() throws Exception {
    //     Long userId = 1L;
    //     Order order1 = new Order();
    //     order1.setId(1L);
    //     order1.setUserId(userId);

    //     Order order2 = new Order();
    //     order2.setId(2L);
    //     order2.setUserId(userId);

    //     List<Order> orders = Arrays.asList(order1, order2);
    //     when(orderService.getOrders(userId)).thenReturn(orders);

    //     mockMvc.perform(get("/api/orders")
    //                     .requestAttr("userId", userId))
    //             .andExpect(status().isOk())
    //             .andExpect(jsonPath("$.size()").value(2))
    //             .andExpect(jsonPath("$[0].id").value(1))
    //             .andExpect(jsonPath("$[1].id").value(2));

    //     verify(orderService).getOrders(userId);
    // }

    // @Test
    // void repay_shouldReturnUpdatedOrder_whenRepayCalled() throws Exception {
    //     Long orderId = 1L;
    //     Order updatedOrder = new Order();
    //     updatedOrder.setId(orderId);
    //     updatedOrder.setStatus(OrderStatus.SETTLED); // 状态设置为已还清

    //     when(orderService.repay(orderId)).thenReturn(updatedOrder);

    //     mockMvc.perform(post("/api/orders/{orderId}/repay", orderId))
    //             .andExpect(status().isOk())
    //             .andExpect(jsonPath("$.id").value(orderId))
    //             .andExpect(jsonPath("$.status").value("REPAID"));

    //     verify(orderService).repay(orderId);
    // }

    @Test
    void postpone_shouldReturnTrue_whenPostponeSucceeds() throws Exception {
        Long orderId = 1L;
        when(orderService.postpone(orderId)).thenReturn(true);

        mockMvc.perform(post("/api/orders/{orderId}/postpone", orderId))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(orderService).postpone(orderId);
    }

    @Test
    void postpone_shouldReturnFalse_whenPostponeFails() throws Exception {
        Long orderId = 1L;
        when(orderService.postpone(orderId)).thenReturn(false);

        mockMvc.perform(post("/api/orders/{orderId}/postpone", orderId))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(orderService).postpone(orderId);
    }

    @Test
    void contextLoads() {
        // 这个测试什么都不做，只是验证上下文能否加载
    }
}
