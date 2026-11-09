package com.example.personal_loan.client;

import com.example.personal_loan.dto.risk.RiskFeatureRequest;
import com.example.personal_loan.dto.risk.RiskFeatureResponse;
import com.example.personal_loan.exception.BusinessException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

/**
 * 风控 LLM 特征 HTTP 客户端（L7.1）
 * <p>
 * 调用 Python FastAPI 的 {@code POST /v1/risk/features} 接口获取 LLM 抽取的风险特征。
 * <p>
 * 实现要点：
 * <ul>
 *   <li>先用 String 接收响应体，再用 ObjectMapper 手动反序列化 —— 绕过 RestTemplate 默认 MessageConverter 的坑</li>
 *   <li>ObjectMapper 配置 {@code FAIL_ON_UNKNOWN_PROPERTIES=false}，避免版本演进时新字段导致失败</li>
 *   <li>ObjectMapper 配置 {@code ACCEPT_SINGLE_VALUE_AS_ARRAY=true} 等宽容选项</li>
 * </ul>
 */
@Slf4j
@Component
public class RiskFeatureClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${risk.service.url:http://127.0.0.1:8000}")
    private String serviceUrl;

    @Value("${risk.service.api-key:classroom-demo-key}")
    private String apiKey;

    @Autowired
    public RiskFeatureClient(@Qualifier("riskServiceRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = createConfiguredMapper();
    }

    /** 测试用构造器：允许注入 mock RestTemplate */
    RiskFeatureClient(RestTemplate restTemplate, String serviceUrl, String apiKey) {
        this.restTemplate = restTemplate;
        this.serviceUrl = serviceUrl;
        this.apiKey = apiKey;
        this.objectMapper = createConfiguredMapper();
    }

    /** 创建一个宽容的 ObjectMapper，适合对接外部 API（Python 风格 snake_case） */
    private static ObjectMapper createConfiguredMapper() {
        ObjectMapper m = new ObjectMapper();
        m.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        m.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        m.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, false);
        m.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        return m;
    }

    /**
     * 调用风控特征服务
     *
     * @return 服务响应（即使 Python 侧降级也返回 features，由上层判断是否可用）
     * @throws RiskFeatureClientException 连接失败 / 超时 / 401 / 5xx / 业务错误
     */
    public RiskFeatureResponse extractFeatures(RiskFeatureRequest request) {
        String url = serviceUrl + "/v1/risk/features";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", apiKey);

        HttpEntity<RiskFeatureRequest> entity = new HttpEntity<>(request, headers);

        log.info("[RiskFeatureClient] POST {} applicationId={} docCount={}",
                url, request.getApplicationId(),
                request.getDocuments() == null ? 0 : request.getDocuments().size());

        // 先用 String 接收，再手动解析 —— 避免 RestTemplate MessageConverter 吞掉错误详情
        final ResponseEntity<String> response;
        try {
            response = restTemplate.postForEntity(url, entity, String.class);
        } catch (ResourceAccessException e) {
            log.warn("[RiskFeatureClient] 请求超时/连接失败 applicationId={}: {}",
                    request.getApplicationId(), e.getMessage());
            throw new RiskFeatureClientException("TIMEOUT", "风控特征服务超时: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("[RiskFeatureClient] 请求异常 applicationId={}: {}",
                    request.getApplicationId(), e.getMessage(), e);
            throw new RiskFeatureClientException("CONNECTION_ERROR",
                    "风控特征服务连接异常: " + e.getMessage(), e);
        }

        HttpStatusCode statusCode = response.getStatusCode();
        int code = statusCode.value();
        String rawBody = response.getBody();

        if (code == 401) {
            log.error("[RiskFeatureClient] API Key 错误 applicationId={}", request.getApplicationId());
            throw new RiskFeatureClientException("UNAUTHORIZED", "风控特征服务 API Key 无效");
        }
        if (code == 503) {
            log.error("[RiskFeatureClient] 服务未配置 applicationId={}", request.getApplicationId());
            throw new RiskFeatureClientException("SERVICE_UNAVAILABLE", "风控特征服务未配置好");
        }
        if (statusCode.is5xxServerError()) {
            log.error("[RiskFeatureClient] 服务端错误 status={} applicationId={}",
                    code, request.getApplicationId());
            throw new RiskFeatureClientException("SERVER_ERROR",
                    "风控特征服务返回 5xx: " + code);
        }

        // 尝试解析响应体
        RiskFeatureResponse body = parseResponse(rawBody, request.getApplicationId());

        if (code == 400) {
            // Python 端 INVALID_PATH / INVALID_INPUT 会返回 400
            String errorCode = body != null && body.getErrorCode() != null
                    ? body.getErrorCode() : "BAD_REQUEST";
            log.warn("[RiskFeatureClient] 业务错误 code={} applicationId={}",
                    errorCode, request.getApplicationId());
            throw new RiskFeatureClientException(errorCode,
                    body != null ? body.getError() : "请求不合法");
        }

        if (body == null) {
            throw new RiskFeatureClientException("EMPTY_RESPONSE", "风控特征服务返回空响应体");
        }

        log.info("[RiskFeatureClient] 成功 applicationId={} status={} llmAvailable={} cached={}",
                body.getApplicationId(), body.getStatus(),
                body.isLlmFeatureAvailable(), body.isCached());

        return body;
    }

    /**
     * 安全地把 JSON 字符串解析为 RiskFeatureResponse。
     * 解析失败时记录日志并返回 null（调用方会抛出 EMPTY_RESPONSE 异常）。
     */
    private RiskFeatureResponse parseResponse(String rawBody, long applicationId) {
        if (rawBody == null || rawBody.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(rawBody, RiskFeatureResponse.class);
        } catch (Exception e) {
            log.error("[RiskFeatureClient] JSON 解析失败 applicationId={}: {}",
                    applicationId, e.getMessage());
            log.debug("[RiskFeatureClient] 原始响应体: {}", rawBody);
            return null;
        }
    }

    /** 自定义异常：风控特征客户端调用失败（需降级） */
    public static class RiskFeatureClientException extends BusinessException {
        private final String clientErrorCode;

        public RiskFeatureClientException(String code, String message) {
            super(500, message);
            this.clientErrorCode = code;
        }

        public RiskFeatureClientException(String code, String message, Throwable cause) {
            super(500, message);
            this.clientErrorCode = code;
            initCause(cause);
        }

        public String getClientErrorCode() {
            return clientErrorCode;
        }
    }
}
