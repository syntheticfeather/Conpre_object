package com.example.personal_loan.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyStatistics {
    // 月份(格式: yyyy-MM)
    private String month;
    // 数量
    private int count;
}
