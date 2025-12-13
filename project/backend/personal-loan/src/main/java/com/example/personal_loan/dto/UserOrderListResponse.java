package com.example.personal_loan.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.personal_loan.enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserOrderListResponse {
    private Long id;
    private BigDecimal loanAmount;          // 借款金额
    private OrderStatus status;             // 状态

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;        // 起始时间
    
    private Integer term;                   // 总期数
    private Integer currentTerm;            // 当前期数
    private Integer overdueDays;            // 逾期天数（0 表示未逾期）
}
