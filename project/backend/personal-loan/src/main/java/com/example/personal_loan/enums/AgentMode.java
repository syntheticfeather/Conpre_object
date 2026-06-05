package com.example.personal_loan.enums;

/**
 * Agent 运行模式 — 传给 Python /api/chat/stream 的 agent_mode 参数
 */
public enum AgentMode {

    REACT("react"),                       // 纯 ReAct while 循环
    PLAN("plan"),                         // Plan-and-Execute
    REFLECTION("reflection"),             // ReAct + Reflection 审查
    PLAN_REFLECTION("plan+reflection");   // 三者全开

    private final String value;

    AgentMode(String value) { this.value = value; }

    /** Python API 需要的字符串 */
    public String value() { return value; }

    /** 默认模式 */
    public static AgentMode defaultMode() { return REACT; }
}
