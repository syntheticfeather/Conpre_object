package com.example.personal_loan.dto;

import com.example.personal_loan.entity.Order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserGetOrderResponse {
    private String productName;
    private Order order;
}
