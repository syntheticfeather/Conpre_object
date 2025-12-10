package com.example.personal_loan.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.personal_loan.entity.LoanOption;
import com.example.personal_loan.enums.ProductStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminGetProDetailResponse {
    private Long productId;

    private String productName;
    private String description;
    private String loanUsage;
    private List<Integer> terms;
    private String promotionDetails;
    private ProductStatus status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    private List<LoanOption> options;

}
