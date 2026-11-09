package com.example.personal_loan.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import com.example.personal_loan.client.RiskFeatureClient.RiskFeatureClientException;
import com.example.personal_loan.dto.risk.RiskFeatureRequest;
import com.example.personal_loan.dto.risk.RiskFeatureResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.util.List;

/**
 * RiskFeatureClient —— MockRestServiceServer 单元测试
 * <p>
 * 覆盖：正常 / 超时 / 401 / 5xx / INVALID_PATH
 */
class RiskFeatureClientTest {

    private MockRestServiceServer mockServer;
    private RiskFeatureClient client;

    @BeforeEach
    void setUp() {
        // 禁用默认错误处理器，让 4xx/5xx 响应能被我们手动解析
        ResponseErrorHandler noOpHandler = new DefaultResponseErrorHandler() {
            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                // no-op：让 RestTemplate 把错误响应当正常响应返回
            }
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                HttpStatusCode status = response.getStatusCode();
                return false;  // 所有状态码都视为无错误
            }
        };
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(noOpHandler);
        mockServer = MockRestServiceServer.bindTo(restTemplate).build();
        client = new RiskFeatureClient(restTemplate, "http://mock-host:8000", "test-api-key");
    }

    private RiskFeatureRequest defaultRequest() {
        RiskFeatureRequest req = new RiskFeatureRequest();
        req.setApplicationId(1001);
        RiskFeatureRequest.ApplicationContext ctx = new RiskFeatureRequest.ApplicationContext();
        ctx.setAmount(java.math.BigDecimal.valueOf(50000));
        ctx.setTerm(12);
        ctx.setIncome(java.math.BigDecimal.valueOf(8000));
        req.setApplication(ctx);
        RiskFeatureRequest.DocumentItem doc = new RiskFeatureRequest.DocumentItem();
        doc.setType("salary_cert");
        doc.setSourcePath("1001/01_salary_cert.png");
        req.setDocuments(List.of(doc));
        req.setApplicationNote("客户申请装修贷");
        return req;
    }

    // ═══════════════════════════════════════════════════════
    //  正常成功
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("正常调用 → 返回 RiskFeatureResponse")
    void extractFeatures_success() {
        mockServer.expect(requestTo("http://mock-host:8000/v1/risk/features"))
                .andExpect(header("X-API-Key", "test-api-key"))
                .andExpect(header("Content-Type", "application/json"))
                .andRespond(withSuccess(
                        "{\"application_id\":1001," +
                        "\"status\":\"ok\"," +
                        "\"error_code\":null," +
                        "\"error\":null," +
                        "\"features\":{\"llm_feature_available\":1.0,\"llm_verdict\":1.0,\"llm_confidence\":0.86}," +
                        "\"deterministic_features\":{\"doc_count\":1.0}," +
                        "\"document_conversion\":[]," +
                        "\"reasoning\":\"ok\"," +
                        "\"model\":\"deepseek-flash\",\"cached\":false}",
                        MediaType.APPLICATION_JSON));

        RiskFeatureResponse resp = client.extractFeatures(defaultRequest());

        assertThat(resp.getApplicationId()).isEqualTo(1001);
        assertThat(resp.getStatus()).isEqualTo("ok");
        assertThat(resp.isLlmFeatureAvailable()).isTrue();
        assertThat(resp.getFeatures()).containsEntry("llm_feature_available", 1.0);
        assertThat(resp.getModel()).isEqualTo("deepseek-flash");
        mockServer.verify();
    }

    @Test
    @DisplayName("Python 端降级 → features 里 llm_feature_available=0 仍正常返回对象")
    void extractFeatures_pythonDegraded() {
        mockServer.expect(requestTo("http://mock-host:8000/v1/risk/features"))
                .andRespond(withSuccess(
                        "{\"application_id\":1001," +
                        "\"status\":\"error\"," +
                        "\"error_code\":\"TIMEOUT\"," +
                        "\"error\":\"特征抽取超时\"," +
                        "\"features\":{\"llm_feature_available\":0.0,\"llm_verdict\":0.0}," +
                        "\"deterministic_features\":{}," +
                        "\"document_conversion\":[]," +
                        "\"reasoning\":\"\"," +
                        "\"model\":\"deepseek-flash\",\"cached\":false}",
                        MediaType.APPLICATION_JSON));

        RiskFeatureResponse resp = client.extractFeatures(defaultRequest());
        assertThat(resp.isDegraded()).isTrue();
        assertThat(resp.getErrorCode()).isEqualTo("TIMEOUT");
    }

    // ═══════════════════════════════════════════════════════
    //  异常场景
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("401 Unauthorized → 抛 RiskFeatureClientException(UNAUTHORIZED)")
    void extractFeatures_401() {
        mockServer.expect(requestTo("http://mock-host:8000/v1/risk/features"))
                .andRespond(withUnauthorizedRequest());

        assertThatThrownBy(() -> client.extractFeatures(defaultRequest()))
                .isInstanceOf(RiskFeatureClientException.class)
                .satisfies(ex -> assertThat(((RiskFeatureClientException) ex).getClientErrorCode())
                        .isEqualTo("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("500 Internal Server Error → 抛 RiskFeatureClientException(SERVER_ERROR)")
    void extractFeatures_500() {
        mockServer.expect(requestTo("http://mock-host:8000/v1/risk/features"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.extractFeatures(defaultRequest()))
                .isInstanceOf(RiskFeatureClientException.class)
                .satisfies(ex -> assertThat(((RiskFeatureClientException) ex).getClientErrorCode())
                        .isEqualTo("SERVER_ERROR"));
    }

    @Test
    @DisplayName("400 INVALID_PATH → 抛 RiskFeatureClientException(INVALID_PATH)")
    void extractFeatures_400_invalidPath() {
        mockServer.expect(requestTo("http://mock-host:8000/v1/risk/features"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error_code\":\"INVALID_PATH\",\"error\":\"材料路径不合法\"}"));

        assertThatThrownBy(() -> client.extractFeatures(defaultRequest()))
                .isInstanceOf(RiskFeatureClientException.class)
                .satisfies(ex -> assertThat(((RiskFeatureClientException) ex).getClientErrorCode())
                        .isEqualTo("INVALID_PATH"));
    }

    @Test
    @DisplayName("503 Service Unavailable → 抛 RiskFeatureClientException(SERVICE_UNAVAILABLE)")
    void extractFeatures_503() {
        mockServer.expect(requestTo("http://mock-host:8000/v1/risk/features"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client.extractFeatures(defaultRequest()))
                .isInstanceOf(RiskFeatureClientException.class)
                .satisfies(ex -> assertThat(((RiskFeatureClientException) ex).getClientErrorCode())
                        .isEqualTo("SERVICE_UNAVAILABLE"));
    }

    @Test
    @DisplayName("空响应体 → 抛 RiskFeatureClientException(EMPTY_RESPONSE)")
    void extractFeatures_emptyBody() {
        mockServer.expect(requestTo("http://mock-host:8000/v1/risk/features"))
                .andRespond(withSuccess().contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.extractFeatures(defaultRequest()))
                .isInstanceOf(RiskFeatureClientException.class)
                .satisfies(ex -> assertThat(((RiskFeatureClientException) ex).getClientErrorCode())
                        .isEqualTo("EMPTY_RESPONSE"));
    }
}
