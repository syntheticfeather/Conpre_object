package com.example.personal_loan.dto;

import java.math.BigDecimal;

import com.example.personal_loan.enums.RepaidType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanOptionResponse {
    private Long optionId;
    private BigDecimal loanAmount;
    private BigDecimal interestRate;
    private Integer loanPeriod;
    private RepaidType repaidType;
}
