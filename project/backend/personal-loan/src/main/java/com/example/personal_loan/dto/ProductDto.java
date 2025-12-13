package com.example.personal_loan.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.personal_loan.entity.LoanOption;
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
public class ProductDto {

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

    @Size(max = 1000, message = "促销详情不能超过1000个字符")
    private String promotionDetails;

    private List<LoanOption> options;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
