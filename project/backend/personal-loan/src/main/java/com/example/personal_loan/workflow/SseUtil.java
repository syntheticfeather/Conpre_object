package com.example.personal_loan.workflow;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 事件构造工具
 */
public final class SseUtil {

    private SseUtil() {}

    /**
     * 创建 emitter 并发送 session_init 事件（对齐 Python /chat/stream 的事件格式：
     * event: session_init / data: {"session_id":"..."}）。
     *
     * WORKFLOW 路径在 Java 本地处理，前端首次发消息时 sessionId 可能为空，
     * 由 Java 生成 UUID 后通过本事件回传前端，保证后续轮次带同一 sessionId、
     * Redis 跨轮参数（loan:params:*）按会话隔离、聊天记录可补录。
     */
    public static SseEmitter newEmitter(String sessionId) {
        SseEmitter emitter = new SseEmitter(60_000L);
        try {
            emitter.send(SseEmitter.event()
                    .name("session_init")
                    .data("{\"session_id\":\"" + escape(sessionId) + "\"}"));
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }

    /** 快速回复一段文本（含 WorkflowResult，便于上层补录聊天记录） */
    public static WorkflowResult quickReply(String sessionId, String text) {
        SseEmitter emitter = newEmitter(sessionId);
        try {
            emitter.send(SseEmitter.event().data(event("message", text)));
            emitter.send(SseEmitter.event().data(event("done", "")));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
        return WorkflowResult.of(emitter, text);
    }

    /** 创建 emitter 并发送错误消息，返回包含错误文本的 WorkflowResult */
    public static WorkflowResult errorReply(SseEmitter emitter, String text) {
        try {
            emitter.send(SseEmitter.event().data(event("message", text)));
            emitter.send(SseEmitter.event().data(event("done", "")));
            emitter.complete();
        } catch (Exception ex) {
            emitter.completeWithError(ex);
        }
        return WorkflowResult.of(emitter, text);
    }

    /** 发送一条消息事件 */
    public static void sendMessage(SseEmitter emitter, String text) throws Exception {
        emitter.send(SseEmitter.event().data(event("message", text)));
    }

    /** 关闭 SSE 流 */
    public static void complete(SseEmitter emitter) throws Exception {
        emitter.send(SseEmitter.event().data(event("done", "")));
        emitter.complete();
    }

    /** 构造单条 SSE 事件 JSON */
    public static String event(String type, String content) {
        return "{\"type\":\"" + type + "\",\"content\":\"" + escape(content) + "\"}";
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
