package com.example.personal_loan.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSuccessEvent {
    private Long orderId;
    private BigDecimal amount;
    private String txId; // 支付网关返回的 交易流水号/支付订单号 （第三方唯一标识）
    private LocalDateTime paidAt;
}
