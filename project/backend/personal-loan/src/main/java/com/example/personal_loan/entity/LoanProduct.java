package com.example.personal_loan.entity;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanProduct {

    private Long id;

    private String productName;

    @Min(value = 1, message = "最短期数不能小于1")
    private Integer minTerm;

    private Integer maxTerm;

    private Integer termStep;

    private String promotionDetails;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public LoanProduct(String productName, int minTerm, int maxTerm, int termStep, String promotionDetails) {
        this.productName = productName;
        this.minTerm = minTerm;
        this.maxTerm = maxTerm;
        this.termStep = termStep;
        this.promotionDetails = promotionDetails;
    }
}
