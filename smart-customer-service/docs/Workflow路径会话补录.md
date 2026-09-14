# Workflow 路径会话补录

> 时间：2026-09-09
> 背景：Agentic Workflow 架构下，WORKFLOW 请求由 Java 本地处理、不经过 Python，导致聊天记录丢失

---

## 一、问题

系统路由分两条链路：

```
前端 POST /api/chat
   │
   ▼
Java 调 Python /api/route 意图分类
   │
   ├── AGENT（咨询/投诉/兜底）
   │     Java 转发 Python /api/chat/stream
   │     → Python 流式结束后自行落库 ✅ 有记录
   │
   └── WORKFLOW（QUERY_STATUS / CALCULATE / LIST_PRODUCTS / APPLY_LOAN，confidence ≥ 0.8）
         Java 本地 Handler 直接 SSE 回复
         → 全程不调用 Python ❌ chat_sessions / chat_messages 无记录、无标题
```

WORKFLOW 对话在侧栏会话列表中不可见，也没有历史记录。

---

## 二、方案

Java workflow 处理完（SSE 已发送）后，**异步回调** Python 新增的内部接口
`POST /api/chat/persist` 补录本轮对话。存储逻辑统一收敛到
`memory/chat_history_service.py` 的 `persist_turn()`，两条链路共用，存储代码只有一处：

```
WORKFLOW 链路：
  Java Handler 生成回复
     │  SSE 先推 session_init（空 sessionId 时 Java 生成 UUID，回传前端）
     │  SSE 再推 message / done（用户无感知）
     ▼
  ChatRouterService 拿到 replyText
     │  WebClient POST /api/chat/persist（fire-and-forget，透传 Bearer token）
     ▼
  Python chat_routes.py → chat_history_service.persist_turn()
     │
     ▼
  MongoDB：兜底建会话 → append user/assistant → 首条消息 LLM 标题回填

AGENT 链路（不变）：
  ChatAgent.chat() 流式结束
     └── 直接 await chat_history_service.persist_turn()（同一个方法）
```

### 空 sessionId 的处理（关键）

前端首次发消息时 `sessionId` 可能为空串。两条链路行为必须对齐：

- **AGENT**：空 sessionId 传给 Python，由 Python 生成 UUID 并通过 SSE `session_init` 事件回传前端；
- **WORKFLOW**：Java 本地处理，**Java 必须自己生成 UUID**（`ChatRouterService` 开头判空 →
  `UUID.randomUUID()`），否则补录会因 session_id 为空被跳过/422，且 Redis 跨轮参数
  （`loan:params:{sessionId}`）会因空 key 互相污染。

生成的新 id 通过 SSE 事件回传前端，格式与 Python 完全一致：

```
event: session_init
data: {"session_id":"<uuid>"}
```

前端收到后更新当前会话 id，后续轮次带同一 id，会话连续、侧栏可见。

---

## 三、补录接口

```
POST http://localhost:8000/api/chat/persist
Header: Authorization: Bearer <token>
Content-Type: application/json

{
  "session_id": "f0d22921-7cf5-4e41-8f26-8830cbf66c41",
  "message": "我有一笔贷款，10000元，利率0.065，12期，先息后本，每个月应该还多少钱？",
  "response": "计算结果如下：\n贷款金额：10000元\n期限：12期\n年利率：6.5%\n还款方式：先息后本\n每月还款：54.17元（仅利息）\n到期还本：10000元"
}

→ 200 { "code": 200, "data": null, "message": "对话已持久化" }
```

- 鉴权与 `/api/chat/stream` 一致：透传用户 token，Python 端解析 user_id；无效 token 返回 401。
- 字段为 snake_case：`session_id` / `message`（用户原话）/ `response`（workflow 最终回复）。

---

## 四、persist_turn 内部流程

`memory/chat_history_service.py` —— 对话落库的唯一入口：

```
persist_turn(session_id, user_id, user_message, assistant_response)
  1. 空值守卫：session_id / message / response 任一为空 → 直接返回
  2. 会话兜底创建：get_session 未命中 → create_session(session_id=..., 跳过空会话复用)
  3. 首条判断：append 之前查 message_count == 0（append 后 count 已 +2，必须先查）
  4. 两次 append_message：先 user 后 assistant，同一时间戳
  5. 首条消息 → await asyncio.wait_for(TitleGenerator().generate(...), timeout=15)
     成功 → update_session_title 回填 10-15 字标题
     超时/失败 → 仅 warning，保留占位标题
  6. 任何异常仅 warning，不影响主链路
```

> 注意：标题生成用 `await asyncio.wait_for` 而非 `asyncio.create_task` —— SSE 结束后
> 游离 task 可能被事件循环回收导致标题丢失。推理模型只取 `content`，
> `reasoning_content` 是思考过程，绝不能当标题。

---

## 五、改动清单

**Python（smart-customer-service）**

| 文件 | 改动 |
|---|---|
| `memory/chat_history_service.py` | 新增。`persist_turn()` 对话落库唯一入口（AGENT/WORKFLOW 共用） |
| `api/chat_routes.py` | 新增。`POST /api/chat/persist`，HTTPBearer 鉴权 |
| `api/models.py` | 新增 `ChatTurnPersistRequest`（session_id / message / response） |
| `api/main.py` | 注册 chat_router |
| `agent/chat_agent.py` | 删除散落的建会话/append/标题逻辑，末尾改为调 `persist_turn()`；记忆注入与提取不变 |

**Java（personal-loan）**

| 文件 | 改动 |
|---|---|
| `workflow/SseUtil.java` | 新增 `newEmitter(sessionId)`：创建 emitter 并先发 `session_init` 事件（格式对齐 Python）；`quickReply` 增加 sessionId 参数 |
| `workflow/WorkflowResult.java` | 新增。`record WorkflowResult(SseEmitter emitter, String replyText)`，让 handler 返回 emitter 的同时带出回复文本 |
| `workflow/Impl/*Handler.java`（4 个） | `new SseEmitter(60_000L)` 统一改为 `SseUtil.newEmitter(sessionId)`；返回值由 `SseEmitter` 改为 `WorkflowResult` |
| `workflow/ChatRouterService.java` | `handleWorkflow` 增加 token 参数；**开头判空：sessionId 为空则 `UUID.randomUUID()` 生成**；SSE 发送后，replyText 非空则调补录 |
| `service/AgentClientService.java` | 新增 `persistWorkflowTurn()`：WebClient POST `/api/chat/persist`，`.subscribe()` fire-and-forget |

---

## 六、设计要点

| 要点 | 决策 |
|---|---|
| 存储逻辑位置 | 只有一处：`persist_turn()`。AGENT 进程内调用、WORKFLOW HTTP 回调，共用同一份代码 |
| 回调时机 | SSE 发送完毕后异步触发；失败只打 warn，**不阻塞、不重试、不影响用户对话** |
| 补录内容 | 仅 2 条消息：用户原话 + workflow 最终回复；不存卡片/引导等中间态 |
| 空 sessionId | **Java 侧判空生成 UUID**（不再跳过补录），并通过 SSE `session_init` 回传前端；仅 replyText 为空时才跳过补录 |
| 会话兜底 | 显式用请求自带的 session_id 建会话，**跳过空会话复用**（不能落到别的空会话上） |
| 标题生成 | 两条链路首条消息都触发 LLM 标题回填，前端无需区分 |
| 长期记忆 | MemoryExtractor 仅 AGENT 路径触发；WORKFLOW 为确定性业务应答，暂不提取 |

---

## 七、职责划分

| 角色 | 职责 |
|---|---|
| Java Handler | 业务处理 + SSE 回复（emitter 由 `SseUtil.newEmitter(sessionId)` 创建，首帧为 `session_init`），通过 `WorkflowResult.replyText()` 带出最终回复文本 |
| Java ChatRouterService | sessionId 为空时生成 UUID；SSE 完成后触发异步补录（不关心结果） |
| Java AgentClientService | 组装 snake_case 请求体、透传 token、fire-and-forget 调用 |
| Python `/api/chat/persist` | 鉴权、参数校验、转调 `persist_turn()` |
| Python `chat_history_service` | 落库唯一入口：兜底建会话、写消息、标题回填 |
| MongoDB | `chat_sessions`（会话元信息 + title）+ `chat_messages`（消息明细） |

---

## 八、验证结论

- Java 主代码 + 测试代码编译通过；两个 workflow 测试类已适配 `WorkflowResult` 返回类型与 `handleWorkflow` 新签名。
- Python 端实测 CALCULATE 场景回调：自动建会话、`chat_messages` 恰好 2 条（用户原话 + workflow 回复）、标题回填成功（如「贷款还款计算咨询」）；第二轮补录后 `message_count=4`、标题不重复生成。
- 空 sessionId 场景（前端首条消息）：模拟 Java 生成 UUID 后回调 → 会话正常创建、2 条消息、标题回填「贷款月还款额咨询」；空 session_id 请求被 Python 参数校验拒绝（Java 侧已保证不再发空）。
- **注意**：Java 侧改动需**重启 Spring Boot 服务**后生效；Python 服务需保持运行，否则补录回调连接失败（Java 仅打 warn，不影响对话本身）。
