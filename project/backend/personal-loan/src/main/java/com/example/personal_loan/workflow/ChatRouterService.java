package com.example.personal_loan.workflow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.example.personal_loan.enums.ChatIntent;

@Service
public class ChatRouterService {

    private final Map<String, WorkflowHandler> handlers = new HashMap<>();

    public ChatRouterService(List<WorkflowHandler> handlerList) {
        for (WorkflowHandler h : handlerList) {
            handlers.put(h.intent(), h);
        }
    }

    public SseEmitter handleWorkflow(ChatIntent intent, String message, Long userId, String sessionId) {
        WorkflowHandler handler = handlers.get(intent.name());
        if (handler == null) {
            return SseUtil.quickReply("已转接 AI 助手处理。");
        }
        return handler.handle(message, userId, sessionId);
    }

    public boolean hasHandler(ChatIntent intent) {
        return handlers.containsKey(intent.name());
    }
}
