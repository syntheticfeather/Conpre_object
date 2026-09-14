package com.example.personal_loan.workflow;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Workflow 处理结果：SSE 流 + 本轮最终回复文本。
 *
 * replyText 是发给用户的完整回复，用于 workflow 结束后回调 Python
 * POST /api/chat/persist 补录聊天记录（WORKFLOW 路径不经过 Python /chat/stream）。
 */
public record WorkflowResult(SseEmitter emitter, String replyText) {

    public static WorkflowResult of(SseEmitter emitter, String replyText) {
        return new WorkflowResult(emitter, replyText);
    }
}
