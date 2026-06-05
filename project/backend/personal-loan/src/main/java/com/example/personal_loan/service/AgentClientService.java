package com.example.personal_loan.service;

import com.example.personal_loan.dto.LoanParams;
import com.example.personal_loan.dto.RouteResult;
import com.example.personal_loan.enums.AgentMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 与 Python 智能客服的后端通信
 */
@Service
@Slf4j
public class AgentClientService {

    private final WebClient webClient;

    public AgentClientService(@Value("${agent.service.url:http://localhost:8000}") String agentUrl) {
        this.webClient = WebClient.builder().baseUrl(agentUrl).build();
    }

    /**
     * 调 Python /api/route 做意图分类
     */
    public RouteResult classify(String message) {
        try {
            var request = Map.of("message", message);
            return webClient.post()
                    .uri("/api/route")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(RouteResult.class)
                    .block();
        } catch (Exception e) {
            log.warn("路由分类失败，降级到 AGENT: {}", e.getMessage());
            var fallback = new RouteResult();
            fallback.setIntent("UNKNOWN");
            fallback.setConfidence(0.0);
            fallback.setAction("AGENT");
            return fallback;
        }
    }

    /**
     * 转发对话到 Python /api/chat/stream，SSE 透传给前端
     */
    /**
     * 调 Python /api/extract-loan-params 用 LLM 提取结构化贷款参数
     */
    public LoanParams extractLoanParams(String message, List<String> availableProducts) {
        try {
            var request = Map.of(
                    "message", message,
                    "available_products", availableProducts != null ? availableProducts : List.of()
            );
            return webClient.post()
                    .uri("/api/extract-loan-params")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(LoanParams.class)
                    .block();
        } catch (Exception e) {
            log.warn("LLM 参数提取失败: {}", e.getMessage());
            return new LoanParams();
        }
    }

    public SseEmitter forwardToAgent(String message, Long userId,
                                      String sessionId, String token, String agentMode) {
        SseEmitter emitter = new SseEmitter(120_000L);

        var request = Map.of(
                "message", message,
                "session_id", sessionId != null ? sessionId : "",
                "agent_mode", agentMode
        );

        Flux<String> sseFlux = webClient.post()
                .uri("/api/chat/stream")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + token)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class);

        sseFlux.subscribe(
                chunk -> {
                    try {
                        emitter.send(SseEmitter.event().data(chunk));
                    } catch (IOException e) {
                        log.debug("SSE send error: {}", e.getMessage());
                        emitter.completeWithError(e);
                    }
                },
                error -> {
                    log.warn("Agent SSE error: {}", error.getMessage());
                    emitter.completeWithError(error);
                },
                emitter::complete
        );

        return emitter;
    }
}
