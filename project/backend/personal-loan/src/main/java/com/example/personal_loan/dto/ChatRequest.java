package com.example.personal_loan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "AI 对话请求")
public class ChatRequest {

    @NotBlank
    @Size(min = 1, max = 1000)
    @Schema(description = "用户消息", example = "帮我查一下贷款申请进度")
    private String message;

    @Schema(description = "会话ID", example = "uuid-xxx")
    private String sessionId;

    @Schema(description = "Agent 模式: react/plan/reflection/plan+reflection", example = "reflection")
    private String agentMode;

    public ChatRequest() {}

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getAgentMode() { return agentMode; }
    public void setAgentMode(String agentMode) { this.agentMode = agentMode; }
}
