package com.example.personal_loan.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.personal_loan.dto.ChatRequest;
import com.example.personal_loan.dto.RouteResult;
import com.example.personal_loan.enums.AgentMode;
import com.example.personal_loan.enums.ChatIntent;
import com.example.personal_loan.service.AgentClientService;
import com.example.personal_loan.workflow.ChatRouterService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api")
@Slf4j
@Tag(name = "AI 对话", description = "智能客服聊天接口")
public class ChatController {

    @Autowired private AgentClientService agentClient;
    @Autowired private ChatRouterService chatRouter;

    private static final String WORKFLOW = "WORKFLOW";

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "AI 对话（SSE流式）")
    public SseEmitter chat(
            @Valid @RequestBody ChatRequest request, HttpServletRequest httpRequest) {

        Long userId = (Long) httpRequest.getAttribute("userId");
        String token = extractToken(httpRequest);
        String sessionId = request.getSessionId();

        RouteResult route = agentClient.classify(request.getMessage());
        ChatIntent intent = route.getIntent();
        log.info("路由: intent={} c={} action={}", intent, route.getConfidence(), route.getAction());

        AgentMode agentMode = selectAgentMode(intent);

        if (WORKFLOW.equals(route.getAction()) && userId != null) {
            return chatRouter.handleWorkflow(intent, request.getMessage(), userId, sessionId);
        } else {
            return agentClient.forwardToAgent(request.getMessage(), userId, sessionId, token, agentMode.value());
        }
    }

    private AgentMode selectAgentMode(ChatIntent intent) {
        return switch (intent) {
            case CALCULATE   -> AgentMode.REFLECTION;
            case APPLY_LOAN  -> AgentMode.REFLECTION;
            case CONSULT     -> AgentMode.PLAN_REFLECTION;
            case COMPLAINT   -> AgentMode.PLAN;
            default          -> AgentMode.REACT;
        };
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return "";
    }
}
