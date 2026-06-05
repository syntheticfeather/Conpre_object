package com.example.personal_loan.workflow;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SSE 事件构造工具
 */
public final class SseUtil {

    private SseUtil() {}

    /** 快速回复一段文本 */
    public static SseEmitter quickReply(String text) {
        SseEmitter emitter = new SseEmitter(60_000L);
        try {
            emitter.send(SseEmitter.event().data(event("message", text)));
            emitter.send(SseEmitter.event().data(event("done", "")));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
        return emitter;
    }

    /** 创建 emitter 并发送错误消息 */
    public static void errorReply(SseEmitter emitter, String text) {
        try {
            emitter.send(SseEmitter.event().data(event("message", text)));
            emitter.send(SseEmitter.event().data(event("done", "")));
            emitter.complete();
        } catch (Exception ex) {
            emitter.completeWithError(ex);
        }
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
