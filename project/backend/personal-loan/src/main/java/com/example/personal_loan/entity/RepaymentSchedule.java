package com.example.personal_loan.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RepaymentSchedule {
    private Long id;
    private Long orderId;
    private Integer term;
    private BigDecimal principal;
    private BigDecimal interest;
    private BigDecimal totalAmount;
    private String status;
    private BigDecimal remainingPrincipal;
    private BigDecimal remainingInterest;
    private LocalDate dueDate;
    private LocalDate actualPayDate;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
