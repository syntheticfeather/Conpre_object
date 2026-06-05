package com.example.personal_loan.enums;

/**
 * 智能客服意图分类 — 对应 Python /api/route 返回的 intent 字段
 */
public enum ChatIntent {

    QUERY_STATUS,     // 查询申请进度
    CALCULATE,        // 计算还款
    LIST_PRODUCTS,    // 查看产品列表
    APPLY_LOAN,       // 申请贷款
    COMPLAINT,        // 投诉
    CONSULT,          // 咨询/推荐/评估
    UNKNOWN;          // 无法识别/降级兜底

    public boolean isWorkflow() {
        return this == QUERY_STATUS || this == CALCULATE
            || this == LIST_PRODUCTS || this == APPLY_LOAN;
    }

    /** 从 JSON 字符串反序列化 */
    public static ChatIntent fromString(String s) {
        if (s == null) return UNKNOWN;
        try { return valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) { return UNKNOWN; }
    }
}
