# 智能客服 MVP 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建智能客服 MVP，包含知识库管理、LangChain Agent、MCP 工具调用、安卓聊天界面

**Architecture:**
- Python Agent 层：LangChain Agent + ChromaDB 向量知识库 + MiniMax M2.7 LLM
- MCP 工具层：查申请状态、计算还款（调用现有 Java API）
- 前端：Vue 3 知识库管理 + 安卓 Kotlin 聊天界面

**Tech Stack:** Python 3.11+, LangChain, ChromaDB, FastAPI, MiniMax M2.7, Vue 3, Kotlin, Jetpack Compose

---

## 第一阶段：Python Agent 服务（后端团队）

### 任务 1: 项目初始化

**Files:**
- Create: `smart-customer-service/requirements.txt`
- Create: `smart-customer-service/.env.example`

- [ ] **Step 1: 创建项目目录和依赖文件**

```txt
# smart-customer-service/requirements.txt
langchain==0.3.0
langchain-community==0.3.0
chromadb==0.5.0
fastapi==0.115.0
uvicorn==0.30.0
anthropic==0.30.0
python-dotenv==1.0.0
sse-starlette==2.0.0
httpx==0.27.0
```

```bash
# .env.example
MINIMAX_API_KEY=your_api_key
MINIMAX_BASE_URL=https://api.minimaxi.com/anthropic
JAVA_API_BASE_URL=http://localhost:8080/api
PORT=8000
```

- [ ] **Step 2: 提交**
```bash
cd smart-customer-service
git init
git add requirements.txt .env.example
git commit -m "feat: init smart-customer-service project structure"
```

---

### 任务 2: ChromaDB 知识库封装

**Files:**
- Create: `smart-customer-service/knowledge/vector_store.py`
- Create: `smart-customer-service/knowledge/models.py`

- [ ] **Step 1: 定义知识库数据模型**

```python
# smart-customer-service/knowledge/models.py
from pydantic import BaseModel
from datetime import datetime
from typing import Optional
import uuid

class KnowledgeItem(BaseModel):
    id: str = str(uuid.uuid4())
    question: str
    answer: str
    category: str = "通用"
    created_at: datetime = datetime.now()
    updated_at: datetime = datetime.now()

    def to_dict(self) -> dict:
        return {
            "id": self.id,
            "question": self.question,
            "answer": self.answer,
            "category": self.category,
            "created_at": self.created_at.isoformat(),
            "updated_at": self.updated_at.isoformat()
        }
```

- [ ] **Step 2: 实现 ChromaDB 向量存储**

```python
# smart-customer-service/knowledge/vector_store.py
import chromadb
from chromadb.config import Settings
from typing import List, Optional
from .models import KnowledgeItem

class VectorStore:
    def __init__(self, persist_directory: str = "./chroma_db"):
        self.client = chromadb.PersistentClient(path=persist_directory)
        self.collection = self.client.get_or_create_collection(
            name="knowledge_base",
            metadata={"description": "智能客服知识库"}
        )

    def add_item(self, item: KnowledgeItem) -> None:
        self.collection.add(
            documents=[item.question],
            metadatas=[{"answer": item.answer, "category": item.category}],
            ids=[item.id]
        )

    def search(self, query: str, top_k: int = 3) -> List[dict]:
        results = self.collection.query(
            query_texts=[query],
            n_results=top_k
        )
        return [
            {
                "id": results["ids"][0][i],
                "question": results["documents"][0][i],
                "answer": results["metadatas"][0][i]["answer"],
                "category": results["metadatas"][0][i]["category"]
            }
            for i in range(len(results["ids"][0]))
        ]

    def delete(self, item_id: str) -> None:
        self.collection.delete(ids=[item_id])

    def get_all(self) -> List[dict]:
        results = self.collection.get()
        return [
            {
                "id": results["ids"][i],
                "question": results["documents"][i],
                "answer": results["metadatas"][i]["answer"],
                "category": results["metadatas"][i]["category"]
            }
            for i in range(len(results["ids"]))
        ]
```

- [ ] **Step 3: 编写测试**

```python
# smart-customer-service/tests/test_vector_store.py
import pytest
import shutil
from knowledge.vector_store import VectorStore
from knowledge.models import KnowledgeItem

@pytest.fixture
def vector_store():
    store = VectorStore(persist_directory="./test_chroma_db")
    yield store
    shutil.rmtree("./test_chroma_db", ignore_errors=True)

def test_add_and_search(vector_store):
    item = KnowledgeItem(
        question="贷款需要什么材料？",
        answer="需要身份证、收入证明",
        category="申请流程"
    )
    vector_store.add_item(item)
    results = vector_store.search("贷款材料")
    assert len(results) > 0
    assert "身份证" in results[0]["answer"]
```

- [ ] **Step 4: 运行测试**
```bash
cd smart-customer-service
pytest tests/test_vector_store.py -v
```

- [ ] **Step 5: 提交**
```bash
git add knowledge/vector_store.py knowledge/models.py tests/test_vector_store.py
git commit -m "feat: add ChromaDB vector store for knowledge base"
```

---

### 任务 3: MCP 工具实现

**Files:**
- Create: `smart-customer-service/tools/mcp_tools.py`
- Create: `smart-customer-service/tools/java_api_client.py`

- [ ] **Step 1: Java API 客户端**

```python
# smart-customer-service/tools/java_api_client.py
import httpx
from typing import Optional, Dict, Any
import os

class JavaApiClient:
    def __init__(self, base_url: Optional[str] = None):
        self.base_url = base_url or os.getenv("JAVA_API_BASE_URL", "http://localhost:8080/api")

    async def get_application_status(self, user_name: str, phone: str) -> Dict[str, Any]:
        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{self.base_url}/loan-applications/status",
                params={"userName": user_name, "phone": phone}
            )
            response.raise_for_status()
            return response.json()

    async def calculate_repayment(self, amount: float, rate: float, months: int) -> Dict[str, Any]:
        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{self.base_url}/orders/calculate-repayment",
                json={"amount": amount, "annualRate": rate, "months": months}
            )
            response.raise_for_status()
            return response.json()
```

- [ ] **Step 2: LangChain Tool 定义**

```python
# smart-customer-service/tools/mcp_tools.py
from langchain_core.tools import tool
from .java_api_client import JavaApiClient

client = JavaApiClient()

@tool
def query_application_status(user_name: str, phone: str) -> str:
    """查询用户的贷款申请状态。当用户询问申请进度、申请状态时使用。"""
    import asyncio
    try:
        result = asyncio.run(client.get_application_status(user_name, phone))
        status = result.get("status", "未知")
        stage = result.get("stage", "未知")
        return f"您的贷款申请状态：{status}，当前阶段：{stage}"
    except Exception as e:
        return f"抱歉，无法查询申请状态：{str(e)}"

@tool
def calculate_repayment(amount: float, rate: float, months: int) -> str:
    """计算还款计划。当用户询问月还款额、还款金额时使用。"""
    import asyncio
    try:
        result = asyncio.run(client.calculate_repayment(amount, rate, months))
        monthly_payment = result.get("monthlyPayment", 0)
        total_payment = result.get("totalPayment", 0)
        return f"贷款金额：{amount}元，年利率：{rate}%，期限：{months}月\n月还款额：{monthly_payment}元\n总还款额：{total_payment}元"
    except Exception as e:
        return f"抱歉，无法计算还款计划：{str(e)}"

TOOLS = [query_application_status, calculate_repayment]
```

- [ ] **Step 3: 编写测试**

```python
# smart-customer-service/tests/test_mcp_tools.py
import pytest
from unittest.mock import AsyncMock, patch
from tools.mcp_tools import query_application_status, calculate_repayment

@pytest.mark.asyncio
async def test_query_application_status():
    with patch("tools.java_api_client.JavaApiClient.get_application_status") as mock:
        mock.return_value = {"status": "审批中", "stage": "风控审核"}
        result = query_application_status.invoke({"user_name": "张三", "phone": "13800138000"})
        assert "审批中" in result

@pytest.mark.asyncio
async def test_calculate_repayment():
    with patch("tools.java_api_client.JavaApiClient.calculate_repayment") as mock:
        mock.return_value = {"monthlyPayment": 5000, "totalPayment": 60000}
        result = calculate_repayment.invoke({"amount": 50000, "rate": 5.0, "months": 12})
        assert "5000" in result
```

- [ ] **Step 4: 运行测试**
```bash
pytest tests/test_mcp_tools.py -v
```

- [ ] **Step 5: 提交**
```bash
git add tools/mcp_tools.py tools/java_api_client.py tests/test_mcp_tools.py
git commit -m "feat: add MCP tools for application status and repayment calculation"
```

---

### 任务 4: LangChain Agent

**Files:**
- Create: `smart-customer-service/agent/chat_agent.py`
- Create: `smart-customer-service/agent/prompts.py`

- [ ] **Step 1: 定义 System Prompt**

```python
# smart-customer-service/agent/prompts.py

SYSTEM_PROMPT = """你是一个友好的贷款智能客服。请根据以下规则回答用户问题：

1. 如果用户询问贷款申请状态，使用 query_application_status 工具查询
2. 如果用户询问还款计划/月还款额，使用 calculate_repayment 工具计算
3. 如果用户询问其他问题，优先从知识库中查找答案
4. 如果知识库没有相关信息，给出通用建议并引导用户提供更多信息

回答要求：
- 语气友好、专业
- 简洁明了
- 对于需要调用工具的问题，先调用工具再基于结果回答
"""
```

- [ ] **Step 2: 实现 Chat Agent**

```python
# smart-customer-service/agent/chat_agent.py
from langchain.agents import AgentExecutor, create_tool_calling_agent
from langchain_core.messages import HumanMessage, SystemMessage
from langchain_openai import ChatMiniMax
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from typing import List, Dict, Any, AsyncGenerator
import os

from knowledge.vector_store import VectorStore
from tools.mcp_tools import TOOLS

class ChatAgent:
    def __init__(self):
        self.vector_store = VectorStore()
        api_key = os.getenv("MINIMAX_API_KEY")
        base_url = os.getenv("MINIMAX_BASE_URL", "https://api.minimaxi.com/anthropic")

        self.llm = ChatMiniMax(
            model="MiniMax-M2.7",
            api_key=api_key,
            base_url=base_url,
            temperature=0.7
        )

        prompt = ChatPromptTemplate.from_messages([
            SystemMessage(content=SYSTEM_PROMPT),
            MessagesPlaceholder(variable_name="chat_history", optional=True),
            HumanMessage(content="{input}"),
            MessagesPlaceholder(variable_name="agent_scratchpad")
        ])

        agent = create_tool_calling_agent(self.llm, TOOLS, prompt)
        self.agent_executor = AgentExecutor(agent=agent, tools=TOOLS, verbose=True)

    async def chat(self, message: str, chat_history: List[Dict] = None) -> AsyncGenerator[str, None]:
        # 先检索知识库
        knowledge_results = self.vector_store.search(message, top_k=2)

        # 构建上下文
        context = ""
        if knowledge_results:
            context = "知识库参考：\n" + "\n".join([
                f"Q: {r['question']}\nA: {r['answer']}"
                for r in knowledge_results
            ])

        input_with_context = f"{context}\n\n用户问题：{message}" if context else message

        # 执行 Agent
        async for event in self.agent_executor.astream_events(
            {"input": input_with_context, "chat_history": chat_history or []},
            version="v1"
        ):
            if event["event"] == "tool":
                yield f"[tool_call]{event['data']['output']['name']}[/tool_call]"
            elif event["event"] == "chain_stream":
                yield event["data"]["chunk"].content

    def add_knowledge(self, question: str, answer: str, category: str = "通用") -> None:
        from knowledge.models import KnowledgeItem
        item = KnowledgeItem(question=question, answer=answer, category=category)
        self.vector_store.add_item(item)
```

- [ ] **Step 3: 提交**
```bash
git add agent/chat_agent.py agent/prompts.py
git commit -m "feat: implement LangChain chat agent with knowledge retrieval"
```

---

### 任务 5: FastAPI 接口

**Files:**
- Create: `smart-customer-service/api/main.py`
- Create: `smart-customer-service/api/models.py`
- Create: `smart-customer-service/api/knowledge_routes.py`

- [ ] **Step 1: API 数据模型**

```python
# smart-customer-service/api/models.py
from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime

class ChatRequest(BaseModel):
    message: str
    session_id: Optional[str] = None

class ChatResponse(BaseModel):
    message: str
    session_id: str

class KnowledgeItemCreate(BaseModel):
    question: str
    answer: str
    category: str = "通用"

class KnowledgeItemResponse(BaseModel):
    id: str
    question: str
    answer: str
    category: str
    created_at: datetime
    updated_at: datetime
```

- [ ] **Step 2: 知识库路由**

```python
# smart-customer-service/api/knowledge_routes.py
from fastapi import APIRouter, HTTPException
from typing import List
from .models import KnowledgeItemCreate, KnowledgeItemResponse
from knowledge.vector_store import VectorStore
from knowledge.models import KnowledgeItem

router = APIRouter(prefix="/knowledge", tags=["knowledge"])
vector_store = VectorStore()

@router.get("", response_model=List[dict])
def list_knowledge():
    return vector_store.get_all()

@router.post("")
def create_knowledge(item: KnowledgeItemCreate):
    kb_item = KnowledgeItem(
        question=item.question,
        answer=item.answer,
        category=item.category
    )
    vector_store.add_item(kb_item)
    return kb_item.to_dict()

@router.delete("/{item_id}")
def delete_knowledge(item_id: str):
    vector_store.delete(item_id)
    return {"status": "ok"}
```

- [ ] **Step 3: 主应用**

```python
# smart-customer-service/api/main.py
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from sse_starlette.sse import EventSourceResponse
from typing import Dict
import uuid

from .knowledge_routes import router as knowledge_router
from agent.chat_agent import ChatAgent

app = FastAPI(title="智能客服 API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(knowledge_router)

# 会话存储（生产环境用 Redis）
sessions: Dict[str, list] = {}

@app.post("/api/chat/stream")
async def chat_stream(request: dict):
    message = request["message"]
    session_id = request.get("session_id", str(uuid.uuid4()))

    if session_id not in sessions:
        sessions[session_id] = []

    agent = ChatAgent()

    async def event_generator():
        chat_history = sessions[session_id]
        full_response = ""

        async for chunk in agent.chat(message, chat_history):
            if chunk.startswith("[tool_call]"):
                yield {"event": "tool", "data": chunk}
            else:
                full_response += chunk
                yield {"event": "message", "data": chunk}

        # 保存对话历史
        sessions[session_id].append({"role": "user", "content": message})
        sessions[session_id].append({"role": "assistant", "content": full_response})

    return EventSourceResponse(event_generator())

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
```

- [ ] **Step 4: 启动测试**
```bash
cd smart-customer-service
uvicorn api.main:app --reload --port 8000
```

- [ ] **Step 5: 提交**
```bash
git add api/main.py api/models.py api/knowledge_routes.py
git commit -m "feat: add FastAPI endpoints for chat and knowledge management"
```

---

## 第二阶段：Vue 知识库管理（前端团队）

### 任务 6: Vue 知识库管理页面

**Files:**
- Create: `project/frontend/src/views/KnowledgeManagement.vue`
- Modify: `project/frontend/src/router/index.js`

- [ ] **Step 1: 知识库管理页面组件**

```vue
<!-- project/frontend/src/views/KnowledgeManagement.vue -->
<template>
  <div class="knowledge-management">
    <h2>知识库管理</h2>

    <!-- 添加表单 -->
    <el-card class="add-form">
      <h3>添加知识条目</h3>
      <el-form :model="form" label-width="80px">
        <el-form-item label="问题">
          <el-input v-model="form.question" placeholder="请输入问题" />
        </el-form-item>
        <el-form-item label="答案">
          <el-input v-model="form.answer" type="textarea" rows="3" placeholder="请输入答案" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" placeholder="选择分类">
            <el-option label="申请流程" value="申请流程" />
            <el-option label="产品咨询" value="产品咨询" />
            <el-option label="还款问题" value="还款问题" />
            <el-option label="通用" value="通用" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="addKnowledge">添加</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 知识列表 -->
    <el-card class="knowledge-list">
      <h3>知识库列表</h3>
      <el-table :data="knowledgeList" stripe>
        <el-table-column prop="question" label="问题" />
        <el-table-column prop="answer" label="答案" show-overflow-tooltip />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="danger" size="small" @click="deleteKnowledge(row.id)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const API_BASE = 'http://localhost:8000/knowledge'

const form = ref({
  question: '',
  answer: '',
  category: '通用'
})

const knowledgeList = ref([])

const fetchKnowledge = async () => {
  const res = await axios.get(API_BASE)
  knowledgeList.value = res.data
}

const addKnowledge = async () => {
  try {
    await axios.post(API_BASE, form.value)
    ElMessage.success('添加成功')
    form.value = { question: '', answer: '', category: '通用' }
    fetchKnowledge()
  } catch (e) {
    ElMessage.error('添加失败')
  }
}

const deleteKnowledge = async (id) => {
  try {
    await axios.delete(`${API_BASE}/${id}`)
    ElMessage.success('删除成功')
    fetchKnowledge()
  } catch (e) {
    ElMessage.error('删除失败')
  }
}

onMounted(fetchKnowledge)
</script>

<style scoped>
.knowledge-management {
  padding: 20px;
}
.add-form {
  margin-bottom: 20px;
}
</style>
```

- [ ] **Step 2: 添加路由**

```javascript
// project/frontend/src/router/index.js (在现有路由数组中添加)
{
  path: '/knowledge',
  name: 'KnowledgeManagement',
  component: () => import('../views/KnowledgeManagement.vue'),
  meta: { requiresAuth: true }
}
```

- [ ] **Step 3: 提交**
```bash
cd project/frontend
git add src/views/KnowledgeManagement.vue src/router/index.js
git commit -m "feat: add knowledge management page"
```

---

## 第三阶段：安卓聊天界面（安卓团队）

### 任务 7: 安卓聊天界面

**Files:**
- Create: `project/Android/app/src/main/java/com/example/smartcustomer/ChatScreen.kt`
- Create: `project/Android/app/src/main/java/com/example/smartcustomer/ChatViewModel.kt`
- Create: `project/Android/app/src/main/java/com/example/smartcustomer/ChatApiService.kt`

- [ ] **Step 1: ChatApiService (SSE 流式调用)**

```kotlin
// project/Android/app/src/main/java/com/example/smartcustomer/ChatApiService.kt
package com.example.smartcustomer

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ChatApiService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.SECONDS) // 无超时，用于 SSE
        .build()

    fun sendMessage(message: String, sessionId: String?): Flow<String> = flow {
        val json = JSONObject().apply {
            put("message", message)
            sessionId?.let { put("session_id", it) }
        }

        val request = Request.Builder()
            .url("http://10.0.2.2:8000/api/chat/stream") // 安卓模拟器访问 localhost
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body ?: return@flow
            body.byteStream().bufferedReader().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.startsWith("data:")) {
                        val data = line!!.substring(5).trim()
                        if (data.isNotEmpty() && data != "[DONE]") {
                            emit(data)
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: ChatViewModel**

```kotlin
// project/Android/app/src/main/java/com/example/smartcustomer/ChatViewModel.kt
package com.example.smartcustomer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val isToolCall: Boolean = false
)

class ChatViewModel : ViewModel() {
    private val apiService = ChatApiService()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var sessionId: String? = null

    fun sendMessage(text: String) {
        viewModelScope.launch {
            // 添加用户消息
            _messages.value = _messages.value + ChatMessage(text, isUser = true)

            _isLoading.value = true
            val fullResponse = StringBuilder()

            try {
                apiService.sendMessage(text, sessionId).collect { chunk ->
                    val isToolCall = chunk.startsWith("[tool_call]")
                    fullResponse.append(chunk.replace("[tool_call]", "").replace("[/tool_call]", ""))

                    // 实时更新 UI
                    val currentMessages = _messages.value.toMutableList()
                    if (currentMessages.lastOrNull()?.isUser == true) {
                        currentMessages.add(ChatMessage(fullResponse.toString(), isUser = false, isToolCall = isToolCall))
                    } else {
                        currentMessages[currentMessages.lastIndex] =
                            ChatMessage(fullResponse.toString(), isUser = false, isToolCall = isToolCall)
                    }
                    _messages.value = currentMessages
                }
            } finally {
                _isLoading.value = false
            }
        }
    }
}
```

- [ ] **Step 3: ChatScreen (Jetpack Compose)**

```kotlin
// project/Android/app/src/main/java/com/example/smartcustomer/ChatScreen.kt
package com.example.smartcustomer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ChatScreen(
    viewModel: ChatViewModel = viewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 标题
        Text(
            text = "智能客服",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 消息列表
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message)
            }
        }

        // 加载指示器
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(8.dp))
        }

        // 输入框
        Row(
            modifier = Modifier.padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入您的问题...") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                },
                enabled = !isLoading
            ) {
                Text("发送")
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (message.isUser) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondary,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.content,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(12.dp),
                style = if (message.isToolCall)
                    MaterialTheme.typography.bodySmall
                else
                    MaterialTheme.typography.bodyMedium
            )
        }
    }
}
```

- [ ] **Step 4: 提交**
```bash
cd project/Android
git add app/src/main/java/com/example/smartcustomer/
git commit -m "feat(android): add chat screen with streaming support"
```

---

## 第四阶段：联调与测试

### 任务 8: 联调测试

- [ ] **Step 1: 启动 Python 服务**
```bash
cd smart-customer-service
uvicorn api.main:app --reload --port 8000
```

- [ ] **Step 2: 测试对话接口**
```bash
curl -X POST http://localhost:8000/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"message": "贷款需要什么材料？"}'
```

- [ ] **Step 3: 测试知识库 CRUD**
```bash
# 添加
curl -X POST http://localhost:8000/knowledge \
  -H "Content-Type: application/json" \
  -d '{"question": "测试问题", "answer": "测试答案", "category": "测试"}'

# 列表
curl http://localhost:8000/knowledge
```

- [ ] **Step 4: 提交**
```bash
git commit -m "test: integration testing completed"
```

---

## 任务完成检查清单

- [ ] ChromaDB 知识库可正常添加/搜索
- [ ] MCP 工具可调用 Java API
- [ ] LangChain Agent 可正常回复
- [ ] FastAPI 流式接口正常
- [ ] Vue 知识库管理页面正常
- [ ] 安卓聊天界面可收发消息
- [ ] 端到端联调通过
