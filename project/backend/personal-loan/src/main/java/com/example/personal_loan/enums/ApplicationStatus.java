package com.example.personal_loan.enums;

public enum ApplicationStatus {
    审核中,
    已通过,
    AI拒绝,
    人工拒绝,
    已取消;

    public boolean isRejected() {
        return this == AI拒绝 || this == 人工拒绝;
    }
}
