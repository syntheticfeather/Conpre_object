package com.example.personal_loan.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManualCheckRequest {
    private Long loanApplicationId;
    private Boolean approved; 
}
