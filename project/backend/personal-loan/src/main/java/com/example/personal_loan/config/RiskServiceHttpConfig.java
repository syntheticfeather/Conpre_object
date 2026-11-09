package com.example.personal_loan.config;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 风控特征服务 HTTP 客户端配置。
 * <p>
 * 单独建一个 RestTemplate Bean，避免使用全局 RestTemplate 导致
 * {@code risk.service.timeout-ms} 影响其他 HTTP 调用。
 */
@Configuration
public class RiskServiceHttpConfig {

    @Value("${risk.service.timeout-ms:30000}")
    private int timeoutMs;

    @Bean
    public RestTemplate riskServiceRestTemplate() {
        Duration timeout = Duration.ofMillis(timeoutMs);
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(timeout)
                .withReadTimeout(timeout);
        return new RestTemplate(ClientHttpRequestFactoryBuilder.detect().build(settings));
    }
}
