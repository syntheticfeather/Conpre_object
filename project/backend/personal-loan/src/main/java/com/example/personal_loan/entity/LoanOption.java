package com.example.personal_loan.entity;

import java.math.BigDecimal;

import com.example.personal_loan.enums.RepaidType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanOption {
    private Long id;
    private Long productId;

    private Integer loanPeriod;

    private BigDecimal loanAmount;

    private BigDecimal interestRate;
    
    private RepaidType repaidType;
}
