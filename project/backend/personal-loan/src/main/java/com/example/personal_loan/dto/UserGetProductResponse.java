package com.example.personal_loan.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserGetProductResponse {
    private Long productId;

    private String productName;

    private String description;
    private String loanUsage;
    private String productType;
    private String promotionDetails;

    private BigDecimal minAmount;
    private BigDecimal maxAmount;

    private List<Integer> terms;
    private List<LoanOptionResponse> options;

    /** 推荐理由（仅推荐接口返回时填充，普通列表接口为 null） */
    private String recommendReason;
}
