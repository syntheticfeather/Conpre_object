package com.example.personal_loan.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** Python /api/extract-loan-params 返回的结构化参数 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoanParams {

    private String productName;   // 或 null
    private Double amount;        // 元，或 null
    private Integer months;       // 月，或 null
    private String repaidType;    // 等额本息 / 等额本金 / ... 或 null
    private String intent;        // apply / calculate / modify / cancel / unknown

    public LoanParams() {}

    public String getProductName() { return productName; }
    public void setProductName(String v) { this.productName = v; }

    public Double getAmount() { return amount; }
    public void setAmount(Double v) { this.amount = v; }

    public Integer getMonths() { return months; }
    public void setMonths(Integer v) { this.months = v; }

    public String getRepaidType() { return repaidType; }
    public void setRepaidType(String v) { this.repaidType = v; }

    public String getIntent() { return intent; }
    public void setIntent(String v) { this.intent = v; }

    /** 参数是否齐全（可以开始申请/计算） */
    public boolean isComplete() {
        return productName != null && amount != null && months != null;
    }

    /** 是否至少有一些有效参数 */
    public boolean hasAny() {
        return productName != null || amount != null || months != null;
    }
}
