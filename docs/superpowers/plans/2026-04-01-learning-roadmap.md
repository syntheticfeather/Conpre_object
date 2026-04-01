# 智能客服学习知识图谱

## 学习目标

通过智能客服项目，让各团队成员掌握以下核心技能：

| 团队 | 核心学习目标 |
|------|-------------|
| **后端** | LangChain Agent 开发、MCP 协议、ChromaDB 向量数据库 |
| **前端** | Vue 3 + AI 前端开发、流式响应 UI、LangChain.js 入门 |
| **安卓** | Kotlin 协程、流式 UI 开发、SSE 实时通信 |

---

## 一、后端团队（Python）

### 1.1 LangChain 核心概念

```
LangChain 学习路径
├── 基础：Prompt Template + LLM 调用
│   ├── ChatPromptTemplate
│   ├── SystemMessage / HumanMessage
│   └── LangChain Expression Language (LCEL)
│
├── 中级：Tool Calling Agent
│   ├── @tool 装饰器定义工具
│   ├── create_tool_calling_agent
│   ├── AgentExecutor 执行流程
│   └── tool_choice 控制工具调用
│
└── 高级：RAG (检索增强生成)
    ├── 向量数据库集成
    ├── similarity_search 语义检索
    └── 上下文注入策略
```

**推荐学习资料**:
- LangChain 官方文档: https://python.langchain.com/
- ChromaDB 文档: https://docs.trychroma.com/

**关键代码示例**:

```python
# 1. 定义 Tool
from langchain_core.tools import tool

@tool
def get_weather(location: str) -> str:
    """获取天气信息"""
    return f"{location} 今天晴天，25度"

# 2. 创建 Agent
from langchain.agents import create_tool_calling_agent

agent = create_tool_calling_agent(
    llm,
    tools=[get_weather],
    prompt=prompt
)

# 3. 执行
agent_executor = AgentExecutor(agent=agent, tools=[get_weather], verbose=True)
result = agent_executor.invoke({"input": "北京天气怎么样？"})
```

### 1.2 ChromaDB 向量数据库

**概念**: 将文本转为向量，存储在向量数据库中，检索时计算相似度

```
知识库检索流程
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│  用户问题    │ →  │  向量检索    │ →  │  知识库匹配  │
│  "贷款材料" │    │  (embedding) │    │  "身份证..." │
└─────────────┘    └─────────────┘    └─────────────┘
```

**关键操作**:

```python
# 添加知识
collection.add(
    documents=["问题文本"],
    metadatas=[{"answer": "答案", "category": "分类"}],
    ids=["unique_id"]
)

# 检索
results = collection.query(
    query_texts=["用户问题"],
    n_results=3  # 返回前3条
)
```

### 1.3 MCP (Model Context Protocol)

**概念**: AI 模型调用外部工具的标准协议

```
MCP 工作流程
┌────────┐     ┌─────────┐     ┌──────────┐     ┌─────────┐
│  LLM   │ ──→ │ Tool    │ ──→ │ External │ ──→ │ Result  │
│        │     │ Call    │     │ API      │     │ Response│
└────────┘     └─────────┘     └──────────┘     └─────────┘
```

**项目中的 MCP 工具**:

| 工具名 | 功能 | 数据来源 |
|--------|------|----------|
| `query_application_status` | 查询申请状态 | Java API |
| `calculate_repayment` | 计算还款 | Java API |

### 1.4 FastAPI 进阶

**流式响应 (SSE)**:

```python
from sse_starlette.sse import EventSourceResponse

async def event_generator():
    for chunk in stream_response():
        yield {"event": "message", "data": chunk}

return EventSourceResponse(event_generator())
```

**CORS 跨域配置**:

```python
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # 生产环境应限制
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)
```

---

## 二、前端团队（Vue 3）

### 2.1 LangChain.js 入门

**概念**: JavaScript 版本的 LangChain，用于前端 AI 开发

```
LangChain.js 模块
├── prompts: Prompt 模板
├── llms: LLM 调用
├── chains: 链式调用
├── agents: Agent 框架
└── tools: 工具定义
```

**最小示例**:

```javascript
import { ChatOpenAI } from "@langchain/openai";
import { PromptTemplate } from "@langchain/core/prompts";

const model = new ChatOpenAI({
  model: "MiniMax-M2.7",
  apiKey: process.env.API_KEY
});

const prompt = PromptTemplate.fromTemplate(
  "你是客服，回复用户问题：{input}"
);

const chain = prompt.pipe(model);
const result = await chain.invoke({ input: "贷款需要什么材料？" });
```

### 2.2 流式响应 UI

**核心概念**: AI 生成内容时逐字显示，类似打字机效果

```
实现思路
┌──────────┐    ┌──────────┐    ┌──────────┐
│  SSE     │ → │  流式    │ →  │  UI      │
│  接收    │    │  拼接    │    │  渲染    │
└──────────┘    └──────────┘    └──────────┘
```

**Vue 3 实现思路**:

```vue
<script setup>
import { ref } from 'vue'
import axios from 'axios'

const messages = ref([])
const fullResponse = ref('')

const sendMessage = async (text) => {
  messages.value.push({ role: 'user', content: text })

  const response = await fetch('http://localhost:8000/api/chat/stream', {
    method: 'POST',
    body: JSON.stringify({ message: text }),
    headers: { 'Content-Type': 'application/json' }
  })

  const reader = response.body.getReader()
  const decoder = new TextDecoder()

  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    const chunk = decoder.decode(value)
    fullResponse.value += chunk
  }

  messages.value.push({ role: 'assistant', content: fullResponse.value })
}
</script>
```

### 2.3 Vue 3 知识库管理页面

**技能点**:
- Element Plus 组件使用
- 表单验证
- CRUD API 调用
- 响应式状态管理

---

## 三、安卓团队（Kotlin）

### 3.1 Kotlin 协程与 Flow

**协程**: 轻量级线程，用于处理异步操作

```kotlin
// 挂起函数
suspend fun fetchData(): String {
    return withContext(Dispatchers.IO) {
        // 耗时操作
        api.call()
    }
}

// Flow 用于流式数据
fun streamData(): Flow<String> = flow {
    while (true) {
        emit(dataSource.next())
        delay(100)
    }
}
```

### 3.2 Jetpack Compose 聊天 UI

**核心组件**:

```kotlin
@Composable
fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser)
            Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (message.isUser)
                MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.secondary
        ) {
            Text(text = message.content)
        }
    }
}
```

### 3.3 SSE (Server-Sent Events) 客户端

**OkHttp SSE 接收**:

```kotlin
fun receiveStream(): Flow<String> = flow {
    val request = Request.Builder()
        .url("http://10.0.2.2:8000/api/chat/stream") // 模拟器访问
        .post(body)
        .build()

    client.newCall(request).execute().use { response ->
        response.body?.byteStream()?.bufferedReader()?.use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line!!.startsWith("data:")) {
                    emit(line!!.substring(5).trim())
                }
            }
        }
    }
}
```

### 3.4 Android 网络权限

**AndroidManifest.xml**:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

---

## 四、全员 AI 基础知识

### 4.1 LLM 工作原理

```
LLM 推理流程
┌──────────┐    ┌──────────┐    ┌──────────┐
│  Token   │ →  │  Neural  │ →  │  Output  │
│  Input   │    │  Network │    │  Text    │
└──────────┘    └──────────┘    └──────────┘
  "你好"       170亿参数      "很高兴认识你"
```

**关键概念**:
- **Token**: 文本最小单位（中文约 1-2 字 = 1 token）
- **Temperature**: 创造性参数（0=确定，1=随机）
- **Context Window**: 最大输入+输出 token 数

### 4.2 Prompt Engineering

**基础原则**:
1. 清晰具体的指令
2. 给出例子（Few-shot）
3. 限制输出格式
4. 分步骤思考（Chain of Thought）

```python
# 基础 Prompt
"回答用户问题"

# 优化后
"""你是一个贷款客服助手。
规则：
1. 只回答与贷款相关问题
2. 回答控制在50字以内
3. 如不确定，建议用户联系人工客服

用户问题：{input}
"""
```

### 4.3 RAG (Retrieval-Augmented Generation)

```
RAG 流程
┌─────────┐    ┌─────────┐    ┌─────────┐    ┌─────────┐
│ Query   │ →  │ Retrieve│ →  │ Augment │ →  │  Generate│
│         │    │ (向量检索)│    │ (注入上下文)│   │ (LLM生成)│
└─────────┘    └─────────┘    └─────────┘    └─────────┘
  用户问题      知识库匹配     上下文拼接      最终回答
```

---

## 五、学习路径建议

### 阶段一：入门（1-2 周）

| 角色 | 学习内容 | 目标 |
|------|----------|------|
| 后端 | Python 基础 + FastAPI 入门 | 能写简单 API |
| 前端 | Vue 3 基础 + Element Plus | 能做管理页面 |
| 安卓 | Kotlin 基础 + Compose | 能做简单界面 |

### 阶段二：进阶（2-3 周）

| 角色 | 学习内容 | 目标 |
|------|----------|------|
| 后端 | LangChain 核心概念 + ChromaDB | 能实现 RAG Agent |
| 前端 | LangChain.js 入门 + 流式 UI | 能接入 AI API |
| 安卓 | 协程 + Flow + SSE | 能做流式聊天 |

### 阶段三：实战（3-4 周）

| 任务 | 后端 | 前端 | 安卓 |
|------|------|------|------|
| Week 1 | 搭建 Agent 服务 | 知识库管理页面 | 聊天界面框架 |
| Week 2 | 实现 MCP 工具 | API 对接 | 消息收发 |
| Week 3 | 流式响应 | 列表/表单完善 | 流式渲染 |
| Week 4 | 端到端联调 | 联调 + UI 优化 | 联调 + 测试 |

---

## 六、推荐资源

### 文档

| 资源 | 链接 |
|------|------|
| LangChain 文档 | https://python.langchain.com/ |
| LangChain.js | https://js.langchain.com/ |
| MiniMax API | https://platform.minimax.io/docs |
| FastAPI | https://fastapi.tiangolo.com/ |
| ChromaDB | https://docs.trychroma.com/ |

### 视频课程

| 课程 | 平台 |
|------|------|
| LangChain 101 | YouTube (Greg Lim) |
| Vue 3 + FastAPI 全栈 | YouTube / B站 |
| Jetpack Compose | 官方 Codelab |

---

## 七、学习检查清单

### 后端团队
- [ ] 能用 FastAPI 写一个简单 API
- [ ] 理解 LangChain Agent 执行流程
- [ ] 能用 ChromaDB 存储和检索知识
- [ ] 能定义 LangChain Tool
- [ ] 能实现 SSE 流式响应

### 前端团队
- [ ] 能用 Vue 3 + Element Plus 做 CRUD 页面
- [ ] 理解 SSE/流式响应的原理
- [ ] 了解 LangChain.js 的基本概念
- [ ] 能实现聊天消息的流式渲染

### 安卓团队
- [ ] 掌握 Kotlin 协程和 Flow
- [ ] 能用 Jetpack Compose 写聊天 UI
- [ ] 能实现 OkHttp SSE 接收
- [ ] 理解流式数据的处理方式
