package com.example.personal_loan.enums;

public enum ApplicationStatus {
    PENDING,
    APPROVED,
    AI_REJECTED,
    MANUAL_REJECTED,
    CANCELLED;

    public boolean isRejected() {
        return this == AI_REJECTED || this == MANUAL_REJECTED;
    }
}
