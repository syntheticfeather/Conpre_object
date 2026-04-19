package com.example.personal_loan.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.personal_loan.enums.ApplicationStatus;
import com.example.personal_loan.enums.RepaidType;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonGetter;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplication {

    private Long id;
    private Long userId;
    private Long productId;

    private ApplicationStatus status;
    private BigDecimal loanAmount;
    private BigDecimal interestRate;
    private Integer loanPeriod;
    private Integer term;
    private RepaidType repaidType;
    private String rejectReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime applyTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime reviewTime;
    
    @JsonGetter("status")
    public String getStatusDisplay() {
        return status.getDisplayStatus();
    }

}