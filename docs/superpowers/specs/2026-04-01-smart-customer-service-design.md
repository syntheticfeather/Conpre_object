# 智能客服 MVP 设计文档

## 1. 项目概述

**项目名称**: 智能客服（贷款申请人服务）
**项目类型**: AI Agent + 本地知识库 + MCP 工具调用
**服务对象**: 贷款申请人（安卓端）
**团队**: 后端（Python）、前端（Vue）、安卓（Kotlin）

---

## 2. 系统架构

```
┌──────────────────────────────────────────────────────────────────┐
│                        智能客服 MVP 架构                           │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│   ┌─────────────┐         ┌─────────────────────────────────┐   │
│   │  安卓端      │  ←────→ │  Python Agent (FastAPI)        │   │
│   │  (Kotlin)   │  HTTP   │                                 │   │
│   │  贷款申请人   │         │  LangChain Agent               │   │
│   └─────────────┘         │   ├─ 知识库检索 (ChromaDB)      │   │
│                           │   ├─ MCP 工具调用                │   │
│                           │   │   ├─ 查申请状态 (调Java API) │   │
│   ┌─────────────┐         │   │   └─ 计算还款 (调Java API)   │   │
│   │  Web管理端   │  ←────→ │   └─ 回复生成 (MiniMax M2.7)   │   │
│   │  (Vue 3)    │         └─────────────────────────────────┘   │
│   │  知识库CRUD  │                                                   │
│   └─────────────┘                                                   │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 3. 技术栈

| 组件 | 技术 | 说明 |
|------|------|------|
| Agent 框架 | LangChain (Python) | 核心推理 |
| LLM | MiniMax M2.7 | 支持 Function Calling |
| 向量数据库 | ChromaDB | 轻量级，开箱即用 |
| API 框架 | FastAPI | Python 高性能 API |
| 安卓端 | Kotlin + Jetpack Compose | 聊天 UI |
| Web 管理端 | Vue 3 | 知识库 CRUD |

---

## 4. MVP 功能范围

### 4.1 知识库管理（Web 管理端）

| 功能 | 描述 |
|------|------|
| 查看知识库列表 | 分页展示所有问答对 |
| 添加问答对 | 录入问题、答案、分类标签 |
| 编辑问答对 | 修改已有内容 |
| 删除问答对 | 移除不需要的知识 |

**数据结构**:

```json
{
  "id": "uuid",
  "question": "贷款需要什么材料？",
  "answer": "需要身份证、收入证明、银行流水。",
  "category": "申请流程",
  "created_at": "2026-04-01T10:00:00Z",
  "updated_at": "2026-04-01T10:00:00Z"
}
```

### 4.2 智能对话（安卓端）

| 功能 | 描述 |
|------|------|
| 发送消息 | 用户输入文本 |
| 接收回复 | Agent 流式返回内容 |
| 聊天历史 | 同一会话内保持上下文 |

**对话流程**:
1. 用户发送消息
2. FastAPI 接收请求
3. LangChain Agent 处理：
   - 意图判断（知识库问答 / 调用工具）
   - 知识库检索（如需）
   - MCP 工具调用（如需）
   - 回复生成
4. 流式返回给安卓端

### 4.3 MCP 工具

| 工具 | 功能 | 调用方式 |
|------|------|----------|
| `query_application_status` | 查询贷款申请状态 | 调用 Java 后台 API |
| `calculate_repayment` | 计算还款计划 | 调用 Java 后台 API |

**工具定义（LangChain Tool）**:
```python
# 示例结构
Tool(
    name="query_application_status",
    description="查询用户的贷款申请状态",
    args_schema={
        "user_name": "用户名",
        "phone": "手机号"
    }
)

Tool(
    name="calculate_repayment",
    description="计算还款计划",
    args_schema={
        "amount": "贷款金额",
        "rate": "年利率",
        "months": "贷款月数"
    }
)
```

---

## 5. 团队分工

| 团队 | MVP 任务 | 学习目标 |
|------|----------|----------|
| **后端 (Python)** | LangChain Agent + MCP + FastAPI + ChromaDB | LangChain、ChromaDB、MCP 协议 |
| **前端 (Vue)** | 知识库管理页面 | Vue 3 + AI 数据结构设计 |
| **安卓 (Kotlin)** | 聊天界面 + API 对接 | 流式 UI、对话状态管理 |

---

## 6. API 接口设计

### 6.1 对话接口

```
POST /api/chat/stream
Body: { "message": "我的贷款申请到哪一步了？", "session_id": "xxx" }
Response: SSE 流式响应
```

### 6.2 知识库接口

```
GET    /api/knowledge              # 获取列表
POST   /api/knowledge              # 添加问答
PUT    /api/knowledge/{id}         # 更新问答
DELETE /api/knowledge/{id}        # 删除问答
```

---

## 7. 后续扩展（不在 MVP 范围内）

- 对话日志管理
- 问答对分类统计
- 用户反馈收集
- 多轮对话优化
- 前端 LangChain.js 学习

---

## 8. 项目目录结构（建议）

```
d:/Study/Conpre_object/
├── docs/superpowers/specs/           # 设计文档
├── smart-customer-service/            # AI 服务（Python）
│   ├── agent/                         # LangChain Agent
│   ├── tools/                         # MCP 工具
│   ├── knowledge/                     # ChromaDB 知识库
│   └── api/                          # FastAPI 接口
├── project/
│   ├── frontend/                     # Vue 3 管理端（已有）
│   └── Android/                      # 安卓端（已有）
```

---

## 9. 关键设计决策

| 决策 | 选择 | 理由 |
|------|------|------|
| LLM 模型 | MiniMax M2.7 | 支持 Function Calling，国内可用 |
| 知识库存储 | ChromaDB | 轻量级，易部署 |
| MCP 工具数据源 | Java API | 保持与现有系统解耦 |
| 知识结构 | 问答对 + 分类 | 便于管理和统计 |
| 上线策略 | MVP 先跑 | 快速验证，后续迭代 |
