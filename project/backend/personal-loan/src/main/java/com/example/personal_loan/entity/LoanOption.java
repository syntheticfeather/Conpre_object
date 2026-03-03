package com.example.personal_loan.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.personal_loan.enums.RepaidType;
import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanOption {

    private Long optionId;

    @NotNull(message = "产品ID不能为空")
    private Long productId;

    @NotNull(message = "贷款期限不能为空")
    @Min(value = 1, message = "贷款期限至少为1个月")
    private Integer loanPeriod;

    // @DecimalMin(value = "1000.00", message = "贷款金额不能低于1000元")
    // @DecimalMax(value = "10000000.00", message = "贷款金额不能超过1000万元")
    // private BigDecimal loanAmount;

    @NotNull(message = "利率不能为空")
    @DecimalMin(value = "0.0001", message = "利率必须大于0")
    @DecimalMax(value = "0.5", message = "利率不能超过50%") // 根据监管调整
    private BigDecimal interestRate;
    
    @NotNull(message = "还款方式不能为空")
    private RepaidType repaidType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
