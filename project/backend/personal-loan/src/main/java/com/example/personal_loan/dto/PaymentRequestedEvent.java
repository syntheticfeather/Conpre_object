package com.example.personal_loan.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestedEvent {
    private Long orderId;
    private BigDecimal amount;
    private LocalDateTime requestTime;
}
