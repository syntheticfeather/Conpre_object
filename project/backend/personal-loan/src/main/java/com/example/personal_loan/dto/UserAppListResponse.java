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
public class UserAppListResponse {
    private Long applicationId;      // 用于撤回操作
    
    private String productName;
    private BigDecimal loanAmount;
    private ApplicationStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime applyTime;

    // 可选：拒绝原因（仅当状态为拒绝时返回）
    private String rejectReason;
    
    @JsonGetter("status")
    public String getStatusDisplay() {
        return status.getDisplayStatus();
    }
}
