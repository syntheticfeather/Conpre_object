package com.example.personal_loan.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.example.personal_loan.entity.LoanOption;
import com.example.personal_loan.enums.ProductStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

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

    private BigDecimal minAmount;
    private BigDecimal maxAmount;

    private String promotionDetails;
    private ProductStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    private List<LoanOption> options;

}
