package com.example.personal_loan.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * 风控特征值转换工具。
 * <p>
 * 统一处理 JSON 反序列化后的数字类型不确定问题（Jackson 可能解析成 Integer / Long / Double），
 * 以及 L7.1/L7.2 多处需要的 Map 转换。
 */
public final class RiskFeatureConverter {

    private RiskFeatureConverter() {
        // 工具类禁止实例化
    }

    /**
     * 把 {@code Map<String, Object>} 中的数值安全地转成 {@code Map<String, Double>}。
     * 非数值条目会被忽略。
     */
    public static Map<String, Double> toDoubleMap(Map<String, Object> raw) {
        Map<String, Double> result = new HashMap<>();
        if (raw == null) {
            return result;
        }
        for (Map.Entry<String, Object> e : raw.entrySet()) {
            Object val = e.getValue();
            if (val instanceof Number) {
                result.put(e.getKey(), ((Number) val).doubleValue());
            }
        }
        return result;
    }

    /**
     * 把违约概率（0~1）转成信用分（0~750）。
     * 与 {@link com.example.personal_loan.service.RiskScoringService#toCreditScore(double)} 保持一致。
     */
    public static int toCreditScore(double defaultProbability) {
        return (int) Math.round((1.0 - defaultProbability) * 750);
    }
}
