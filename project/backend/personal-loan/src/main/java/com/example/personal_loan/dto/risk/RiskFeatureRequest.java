package com.example.personal_loan.dto.risk;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * 风控特征服务请求 DTO（对应 Python FastAPI 的 RiskFeatureRequest）
 */
@Data
public class RiskFeatureRequest {

    /** 申请ID */
    private Integer applicationId;

    /** 申请上下文 */
    private ApplicationContext application;

    /** 材料列表 */
    private List<DocumentItem> documents;

    /** 申请备注 */
    private String applicationNote;

    @Data
    public static class ApplicationContext {
        private BigDecimal amount;
        private Integer term;
        private BigDecimal income;
    }

    @Data
    public static class DocumentItem {
        /** 材料类型: salary_cert / credit_report / work_cert 等 */
        private String type;
        /** 直接传入材料文本（优先于 sourcePath） */
        private String text;
        /** 材料源文件路径（相对于 RISK_DOC_BASE_DIR） */
        private String sourcePath;
    }
}
