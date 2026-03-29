package com.example.personal_loan.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRecord {
    private Long id;
    private String txId;
    private Long orderId;
    private BigDecimal amount;
    private String status;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
}
