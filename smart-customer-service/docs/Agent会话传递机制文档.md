# Agent 会话传递机制文档

> 时间：2026-06-04

---

## 总体架构

前端生成 sessionId → Java 透传 → Python 使用 → MongoDB 持久化 → 下次恢复

---

## 完整链路

```
第 1 轮对话：
┌─────────────────────────────────────────────────────────┐
│ 前端 (Vue 3)                                            │
│  用户打开聊天窗口 → sessionId = uuid.v4()                │
│  POST /api/chat                                         │
│  {"message": "查进度", "sessionId": "abc-123"}            │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│ Java: ChatController (第42行)                           │
│  request.getSessionId() → "abc-123"                     │
│  路由分类 → intent=QUERY_STATUS → WORKFLOW               │
│                                                        │
│  分发给 Workflow:                                       │
│    chatRouter.handleWorkflow(intent, msg, userId,        │
│                               "abc-123")                 │
│                                                        │
│  或转发给 Agent:                                         │
│    agentClient.forwardToAgent(msg, userId,               │
│        "abc-123", token, agentMode)                     │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│ AgentClientService (第 85 行)                            │
│  var request = Map.of(                                   │
│      "message", message,                                │
│      "session_id", "abc-123",                           │
│      "agent_mode", agentMode                            │
│  );                                                     │
│  POST http://localhost:8000/api/chat/stream              │
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│ Python: main.py                                         │
│  session_id = body.session_id → "abc-123"                │
│  agent.chat(message, session_id="abc-123", user_id, token)│
└──────────────────────┬──────────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────────┐
│ ReactAgent.chat() (react_agent.py 第 73-78 行)           │
│                                                        │
│  // 1. 恢复历史                                         │
│  history = mongodb_client.get_session_history("abc-123") │
│  // → [] (空，第一次对话)                                │
│                                                        │
│  // 2. 执行 Agent                                       │
│  input_data = {                                        │
│      "input": "查进度",                                  │
│      "system_prompt": "...",                            │
│      "chat_history": []                                 │
│  }                                                     │
│  async for event in agent_executor.astream_events(...)   │
│                                                        │
│  // 3. 保存历史                                         │
│  mongodb_client.save_session_history("abc-123", user_id,  │
│      [{role:"user","content":"查进度"},                  │
│       {role:"assistant","content":"申请#12345已通过"}])  │
└─────────────────────────────────────────────────────────┘

第 2 轮对话（同一个 sessionId）：
┌─────────────────────────────────────────────────────────┐
│ 前端 POST /api/chat                                     │
│  {"message": "算月供", "sessionId": "abc-123"}           │
│                                                        │
│  ... 同样的 Java → Python 链路 ...                        │
│                                                        │
│ ReactAgent.chat():                                      │
│  history = mongodb_client.get_session_history("abc-123") │
│  // → [{role:"user","content":"查进度"},                 │
│  //     {role:"assistant","content":"申请#12345已通过"}] │
│                                                        │
│  input_data = {                                        │
│      "input": "算月供",                                  │
│      "chat_history": [                                 │
│          HumanMessage("查进度"),                         │
│          AIMessage("申请#12345已通过")                   │
│      ]                                                 │
│  }                                                     │
│                                                        │
│  LLM 看到的完整上下文:                                    │
│    system_prompt +                                      │
│    "用户: 查进度"                                        │
│    "AI: 申请#12345已通过"                                │
│    "用户: 算月供"         ← LLM 知道用户刚才在查进度        │
└─────────────────────────────────────────────────────────┘
```

---

## 数据持久化

```
MongoDB: smart_customer_service.chat_history

{
  "session_id": "abc-123",
  "user_id": "4",
  "messages": [
    {
      "message_id": "uuid-1",
      "role": "user",
      "content": "查进度",
      "timestamp": "2026-06-04T10:00:00"
    },
    {
      "message_id": "uuid-2",
      "role": "assistant",
      "content": "申请#12345已通过",
      "timestamp": "2026-06-04T10:00:01"
    }
  ],
  "updated_at": "2026-06-04T10:00:01"
}
```

每次对话追加 2 条（用户 + AI），恢复时取最近 10 条（5 轮）。

---

## 职责划分

| 角色 | 职责 |
|------|------|
| 前端 | 生成 sessionId（uuid.v4），存在组件 state 中 |
| Java | 透传 sessionId，不做处理 |
| Python | 用 sessionId 读写 MongoDB 会话历史 |
| MongoDB | 持久化存储，按 session_id 索引 |

---

## Workflow 中的 session

Workflow 路径不走 Agent，但也需要 session：

```
ApplyLoanHandler:
  sessionId 用于 Redis key → "loan:params:abc-123"
  → 跨轮累积参数（产品名、金额）
  → 10 分钟 TTL 自动过期
```
