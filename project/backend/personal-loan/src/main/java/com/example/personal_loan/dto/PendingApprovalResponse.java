package com.example.personal_loan.dto;

import java.math.BigDecimal;
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
public class PendingApprovalResponse {
    private Long applicationId;

    private String userName;
    private String productName;
    private BigDecimal loanAmount;
    private Integer loanPeriod;
    private Integer term;
    private ApplicationStatus status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime applyTime;
    
    @JsonGetter("status")
    public String getStatusDisplay() {
        return status.getDisplayStatus();
    }
}