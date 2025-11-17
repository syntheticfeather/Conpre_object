package com.example.personal_loan.controller.dto;

import java.util.List;

import com.example.personal_loan.entity.LoanOption;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchCreateOptionRequest {
    private Long productId;           // 指定产品 ID
    private List<LoanOption> options;
}
