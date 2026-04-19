package com.example.personal_loan.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalTypeStatistics {
    // 月份(格式: yyyy-MM)
    private String month;
    // 状态(AI通过/人工通过)
    private String status;
    // 数量
    private int count;
}
