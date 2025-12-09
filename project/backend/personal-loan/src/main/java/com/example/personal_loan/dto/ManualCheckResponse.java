package com.example.personal_loan.dto;

import java.time.LocalDateTime;

import com.example.personal_loan.enums.ApplicationStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ManualCheckResponse {
    private Long loanApplicationId;
    private ApplicationStatus status;
    private String rejectReason;  
    private LocalDateTime reviewTime;
}
