package com.example.personal_loan.workflow;

/**
 * Workflow 处理器接口。
 * 每个 Workflow 类型（QUERY_STATUS / CALCULATE / ...）实现此接口，
 * 通过 intent() 返回自己的意图标识。
 */
public interface WorkflowHandler {

    /** 处理的意图类型 */
    String intent();

    /**
     * 执行 Workflow，返回 SSE 流及本轮最终回复文本。
     * replyText 供 ChatRouterService 回调 Python 补录聊天记录。
     */
    WorkflowResult handle(String message, Long userId, String sessionId);
}
