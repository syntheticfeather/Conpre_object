package com.example.personal_loan.dto;

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
    private String promotionDetails;

    private List<Integer> terms;
    private List<LoanOptionResponse> options;   
}
