package com.example.personal_loan.workflow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.personal_loan.enums.ChatIntent;
import com.example.personal_loan.service.AgentClientService;

@Service
public class ChatRouterService {

    private final Map<String, WorkflowHandler> handlers = new HashMap<>();
    private final AgentClientService agentClient;

    public ChatRouterService(List<WorkflowHandler> handlerList, AgentClientService agentClient) {
        for (WorkflowHandler h : handlerList) {
            handlers.put(h.intent(), h);
        }
        this.agentClient = agentClient;
    }

    public SseEmitter handleWorkflow(ChatIntent intent, String message, Long userId,
                                     String sessionId, String token) {
        // 前端首次发消息时 sessionId 可能为空：WORKFLOW 在 Java 本地处理，
        // 不像 AGENT 路径有 Python 生成 UUID，这里补生成，并通过 SSE session_init
        // 事件回传前端（SseUtil.newEmitter），保证后续轮次同一会话、Redis 参数隔离、可补录。
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        WorkflowResult result;
        WorkflowHandler handler = handlers.get(intent.name());
        if (handler == null) {
            result = SseUtil.quickReply(sessionId, "已转接 AI 助手处理。");
        } else {
            result = handler.handle(message, userId, sessionId);
        }

        // WORKFLOW 在 Java 本地处理，不经过 Python /chat/stream；
        // 处理完成后异步回调 Python 补录本轮对话（会话兜底 + user/assistant 两条消息 + 首条标题）。
        // fire-and-forget：SSE 已发送完毕，落库失败仅告警，不影响用户。
        if (result.replyText() != null && !result.replyText().isBlank()) {
            agentClient.persistWorkflowTurn(sessionId, message, result.replyText(), token);
        }

        return result.emitter();
    }

    public boolean hasHandler(ChatIntent intent) {
        return handlers.containsKey(intent.name());
    }
}
