package com.example.personal_loan.dto;

import java.time.LocalDateTime;

import com.example.personal_loan.enums.ApplicationStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonGetter;

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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewTime;
    
    @JsonGetter("status")
    public String getStatusDisplay() {
        return status.getDisplayStatus();
    }
}
