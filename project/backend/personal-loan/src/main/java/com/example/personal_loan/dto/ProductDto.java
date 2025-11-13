package com.example.personal_loan.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.personal_loan.entity.LoanOption;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {

    private Long id;

    private String productName;

    @Min(value = 1, message = "最短期数不能小于1")
    private Integer minTerm;

    private Integer maxTerm;

    private Integer termStep;

    private String promotionDetails;

    private List<LoanOption> options;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
