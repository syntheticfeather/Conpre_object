package com.example.personal_loan.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.personal_loan.enums.OrderStatus;
import com.example.personal_loan.enums.RepaidType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserGetOrderResponse {
    private Long orderId;

    // 产品信息
    private String productName;

    // 贷款基本信息
    private BigDecimal repaidAmount;    // 已还款金额
    private BigDecimal loanAmount;      // 贷款本金
    private BigDecimal interestRate;    // 年化利率（%）
    private Integer loanPeriod;         // 年限
    private RepaidType repaidType;      // 还款方式如 "等额本息"
    private LocalDateTime startTime;    // 放款时间

    // 状态相关
    private OrderStatus status;          // "正常还款" / "已逾期" / "已结清"
    private Integer overdueDays;        // 仅当逾期时 > 0，否则可为 null 或 0

    // 还款进度
    private Integer term;               // 总期数
    private Integer currentTerm;        // 当前期数

    // 合同（前端可点击下载）
    private String contractUrl;         // 如 "/api/contracts/123.pdf"，而非服务器绝对路径
}
