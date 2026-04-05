package com.example.personal_loan.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    private String eventType;
    private Long userId;
    private Long businessId;
    private String businessType;
    private String visibleStatus;
    private LocalDateTime occurredAt;
}

