package com.example.personal_loan.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.personal_loan.enums.ProductStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanProduct {

    private Long id;

    @NotBlank(message = "产品名称不能为空")
    @Size(max = 100, message = "产品名称不能超过100个字符")
    private String productName;

    @Size(max = 500, message = "产品描述不能超过500个字符")
    private String description;

    @Size(max = 200, message = "贷款用途说明不能超过200个字符")
    private String loanUsage;

    private ProductStatus status;

    @NotNull(message = "最短期数不能为空")
    @Min(value = 1, message = "最短期数不能小于1")
    private Integer minTerm;

    @NotNull(message = "最长期数不能为空")
    @Min(value = 1, message = "最长期数不能小于1")
    private Integer maxTerm;

    @NotNull(message = "期数步长不能为空")
    @Min(value = 1, message = "期数步长不能小于1")
    private Integer termStep;

    private BigDecimal minAmount;
    
    private BigDecimal maxAmount;

    @Size(max = 1000, message = "促销详情不能超过1000个字符")
    private String promotionDetails;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;

    public LoanProduct(String productName, int minTerm, int maxTerm, int termStep, String promotionDetails) {
        this.productName = productName;
        this.minTerm = minTerm;
        this.maxTerm = maxTerm;
        this.termStep = termStep;
        this.promotionDetails = promotionDetails;
    }
}
