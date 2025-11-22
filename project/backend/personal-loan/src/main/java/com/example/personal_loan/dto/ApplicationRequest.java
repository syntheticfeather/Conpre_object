package com.example.personal_loan.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationRequest {
    private Long productId;
    private Long optionId;
    private Integer term;
}
