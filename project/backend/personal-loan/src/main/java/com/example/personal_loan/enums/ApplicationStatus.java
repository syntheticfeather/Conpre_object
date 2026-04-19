package com.example.personal_loan.enums;

public enum ApplicationStatus {
    审核中,  // 非终态
    AI通过,  // AI自动通过
    人工通过,  // 人工审核通过
    AI拒绝,  // 非终态
    人工拒绝,  
    已取消;
    
    // 对外显示状态
    public String getDisplayStatus() {
        if (this == AI通过 || this == 人工通过) {
            return "已通过";
        }
        return this.name();
    }
}
