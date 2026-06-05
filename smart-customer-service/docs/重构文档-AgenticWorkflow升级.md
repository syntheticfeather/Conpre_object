# Agentic Workflow 升级 — 重构文档

> 时间：2026-05-31 ~ 2026-06-04
> 范围：Java Spring Boot + Python FastAPI 智能客服

---

## 改造总览

将纯 ReAct Agent 升级为 Agentic Workflow 架构。四层核心改动：

```
重构前:  所有用户消息 → 单一 ReAct Agent → while 循环
重构后:  用户消息 → 路由分类 → WORKFLOW(简单请求) / AGENT(复杂请求)
                        ↓                    ↓
                     Java 固定流程       Python Agent 链
                                      (React/Plan/Reflection 可组合)
```

---

## 改造清单

### 一、LLM 路由与意图分类

| 文件 | 类型 | 作用 |
|------|------|------|
| `api/route_routes.py` | 新增 | `/api/route` — 意图分类，`/api/extract-loan-params` — 贷款参数提取 |
| `dto/RouteResult.java` | 新增 | 路由响应模型 `{intent, confidence, action}` |
| `dto/LoanParams.java` | 新增 | 贷款参数提取结果模型 |
| `enums/ChatIntent.java` | 新增 | 意图枚举：QUERY_STATUS / CALCULATE / LIST_PRODUCTS / APPLY_LOAN / COMPLAINT / CONSULT |
| `enums/AgentMode.java` | 新增 | Agent 模式枚举：REACT / PLAN / REFLECTION / PLAN_REFLECTION |
| `api/models.py` | 修改 | 加 `RouteResult`、`ExtractLoanParamsRequest`、`LoanParams` 模型 |

**路由决策**：

```
intent + confidence → action:

QUERY_STATUS,   ≥0.8 → WORKFLOW
CALCULATE,      ≥0.8 → WORKFLOW
LIST_PRODUCTS,  ≥0.8 → WORKFLOW
APPLY_LOAN,     ≥0.8 → WORKFLOW
COMPLAINT,      任意 → AGENT
CONSULT,        任意 → AGENT
confidence < 0.8    → AGENT（兜底）
```

---

### 二、Workflow 系统（Java 端）

| 文件 | 类型 | 作用 |
|------|------|------|
| `controller/ChatController.java` | 新增 | `/api/chat` SSE 入口，路由分发 |
| `service/AgentClientService.java` | 新增 | HTTP 客户端：分类、转发、参数提取 |
| `workflow/WorkflowHandler.java` | 新增 | Workflow 处理器接口 |
| `workflow/ChatRouterService.java` | 新增 | 分发中心：自动收集所有 Handler |
| `workflow/SseUtil.java` | 新增 | SSE 事件工具 |
| `workflow/Impl/QueryStatusHandler.java` | 新增 | 查询申请进度 |
| `workflow/Impl/CalculateHandler.java` | 新增 | 计算还款计划（正则提取 + 参数引导） |
| `workflow/Impl/ListProductsHandler.java` | 新增 | 展示 Top-5 产品 |
| `workflow/Impl/ApplyLoanHandler.java` | 新增 | 申请贷款（LLM 提取 + 跨轮参数累积 + 确认卡） |

**Workflow 设计原则**：

- 加新 Handler 只需新建 `@Component` 类实现 `WorkflowHandler`——Controller 和 Router 零改动
- 查询类直接返回，动作类需确认卡
- 跨轮参数通过 Redis 累积（10 分钟 TTL）
- LLM 参数提取替代正则（`/api/extract-loan-params`）

---

### 三、Agent 可组合架构（Python 端）

| 文件 | 类型 | 作用 |
|------|------|------|
| `agent/base_agent.py` | 新增 | `BaseAgent` 抽象基类，统一 `chat()` 接口 |
| `agent/react_agent.py` | 新增 | 纯 ReAct（原有逻辑提取） |
| `agent/plan_execute_agent.py` | 新增 | 包裹任意 Agent：Plan → Execute → Synthesize |
| `agent/reflection_agent.py` | 新增 | 包裹任意 Agent：ReAct → Review → Refine |
| `agent/chat_agent.py` | 重写 | 工厂：按 `agent_mode` 组装 Agent 链 |

**组装方式（组合，非继承）**：

```
ReactAgent()
PlanExecuteAgent(ReactAgent())
ReflectionAgent(ReactAgent())
PlanExecuteAgent(ReflectionAgent(ReactAgent()))   ← 三者全开
```

**Java 根据意图自动选择模式**：

```
CALCULATE   → REFLECTION       (金融计算，必须准确)
APPLY_LOAN  → REFLECTION       (申请确认，不能出错)
CONSULT     → PLAN_REFLECTION  (咨询推荐，复杂+准确)
COMPLAINT   → PLAN             (投诉处理，多步推理)
默认         → REACT            (兜底)
```

---

### 四、RAG 管线优化

| 文件 | 改动 | 说明 |
|------|------|------|
| `utils/markdown_processor.py` | 修改 | Chunk 重叠 200 字符 + 二级句子切分(800字符) |
| `utils/chromadb_client.py` | 修改 | 本地 sentence-transformers 兜底 + 维度对齐 + 缓存 |
| `utils/query_rewriter.py` | 新增 | Query 改写(口语→检索友好) |
| `utils/bm25_retriever.py` | 新增 | BM25 关键词检索 + RRF 多路融合 |
| `utils/reranker.py` | 新增 | Cross-Encoder API 精排 |
| `tools/search_tools.py` | 修改 | 集成多路召回+精排+改写 |
| `requirements.txt` | 修改 | 加 rank-bm25, jieba, sentence-transformers |

---

### 五、模型使用分层

```
单次 LLM 调用(非 Agent):
  /api/route              → gpt-4.1-mini-free   (意图分类)
  /api/extract-loan-params → gpt-4.1-mini-free   (参数提取)

Agent 内部分层:
  PlanExecuteAgent._create_plan()  → gpt-4.1-mini-free
  ReflectionAgent._review()        → gpt-4.1-mini-free
  ReflectionAgent._refine()        → gpt-4.1-mini-free
  ReactAgent (while 循环)          → deepseek-chat      (主力模型)
```

---

## 测试

| 测试文件 | 测试数 | 说明 |
|---------|--------|------|
| `test/.../workflow/WorkflowHandlersTest.java` | 13 | Mock 单元测试，覆盖 4 个 Handler |
| `test/test_rag_pipeline.py` | 23 | RAG 全链路测试 |

---

## 架构图

```
┌─────────────────────────────────────────────────────┐
│ 前端 (Vue 3)                                        │
│  POST /api/chat → SSE 流式接收                       │
└─────────────────┬───────────────────────────────────┘
                  │
┌─────────────────▼───────────────────────────────────┐
│ Java: ChatController                                │
│  1. AgentClientService.classify() → intent+action   │
│  2. 分发: WORKFLOW? → ChatRouterService             │
│           AGENT?    → AgentClientService.forward()   │
└───┬───────────────────────────────┬─────────────────┘
    │                               │
    ▼                               ▼
┌───────────────┐          ┌─────────────────────────┐
│ Workflow      │          │ Python FastAPI           │
│ (Java 本地)    │          │  /api/chat/stream        │
│               │          │                          │
│ QueryStatus   │          │  Agent 链 (可组合):       │
│ Calculate     │          │   ReactAgent             │
│ ListProducts  │          │   PlanExecuteAgent        │
│ ApplyLoan     │          │   ReflectionAgent         │
│               │          │                          │
│ 工具: LLM提取  │          │  工具: RAG / 搜索 / 后端API │
└───────────────┘          └─────────────────────────┘
```
