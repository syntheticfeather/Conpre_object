package com.example.personal_loan.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchByDateRequest {
    private LocalDate updateStartDate;   // 更新开始日期
    private LocalDate updateEndDate;     // 更新结束日期
    private LocalDate createStartDate;   // 创建开始日期
    private LocalDate createEndDate;     // 创建结束日期
}
