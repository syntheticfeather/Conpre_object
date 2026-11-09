package com.example.personal_loan.dto.risk;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

/**
 * 风控特征服务响应 DTO（对应 Python FastAPI 的 RiskFeatureResponse）
 * <p>
 * 注意：Python 端即使失败也返回 features（全 0 + llm_feature_available=0），
 * 所以不要处理 null，直接用 status 判断是否成功。
 * <p>
 * features / deterministicFeatures 用 Map<String, Object> 是因为 Python JSON 里的数字
 * 可能被 Jackson 解析成 Integer / Long / Double 中的任意一种。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RiskFeatureResponse {

    /** 申请ID（某些错误响应可能缺失） */
    private Integer applicationId;

    /** "ok" 或 "error" */
    private String status;

    /** 错误码（仅 error 时有值） */
    private String errorCode;

    /** 错误信息（仅 error 时有值） */
    private String error;

    /**
     * LLM 抽取的特征字典（G 组）。
     * 即使 status=error 也存在 —— Python 会用默认值填充。
     * 但应检查 llm_feature_available 是否为 0 来判断是否降级。
     * <p>
     * 用 Object 类型接收是为了兼容 JSON 数字类型不确定的情况。
     */
    private Map<String, Object> features;

    /** 确定性特征字典（H 组） */
    private Map<String, Object> deterministicFeatures;

    /** 材料转换结果列表 */
    private List<DocumentConversionInfo> documentConversion;

    /** LLM 推理说明（可能为空字符串） */
    private String reasoning;

    /** LLM 模型名称 */
    private String model;

    /** 是否命中缓存 */
    private boolean cached;

    /**
     * 从 features 里安全地取出一个 Double 值（兼容 Integer/Long）。
     */
    private static Double toDouble(Object val) {
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        return null;
    }

    /**
     * 便捷方法：LLM 特征是否可用
     * <p>
     * Python 端失败时会返回 features 里带 "llm_feature_available": 0
     */
    public boolean isLlmFeatureAvailable() {
        if (features == null || features.isEmpty()) {
            return false;
        }
        Double available = toDouble(features.get("llm_feature_available"));
        return available != null && available == 1.0;
    }

    /** 是否为降级响应 */
    public boolean isDegraded() {
        return !isLlmFeatureAvailable() || "error".equals(status);
    }

    /** 取出 features 中某个 key 的 Double 值（空安全） */
    public Double getFeatureDouble(String key) {
        if (features == null) return null;
        return toDouble(features.get(key));
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DocumentConversionInfo {
        private String type;
        private String status;
        private boolean cached;
        private String error;
    }
}
