# smart-customer-service/agent/chat_agent.py
from langchain_classic.agents import AgentExecutor, create_tool_calling_agent
from langchain_core.messages import HumanMessage, SystemMessage
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from typing import List, Dict, Any, AsyncGenerator
from dotenv import load_dotenv
import os
import asyncio
from tenacity import retry, stop_after_attempt, wait_exponential

from knowledge.vector_store import VectorStore
from tools.mcp_tools import TOOLS 
from agent.prompts import SYSTEM_PROMPT

# 加载环境变量
load_dotenv()

class ChatAgent:
    def __init__(self):
        self.vector_store = VectorStore()
        api_key = os.getenv("OPENAI_API_KEY")
        base_url = os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.cn/v1")

        # 配置HTTP客户端，增加超时设置
        from httpx import Client
        http_client = Client(
            timeout=30.0,
            verify=True
        )

        self.llm = ChatOpenAI(
            model="deepseek-chat",
            api_key=api_key,
            base_url=base_url,
            temperature=0.7,
            http_client=http_client
        )

        # 定义系统提示
        prompt = ChatPromptTemplate.from_messages([
            # 系统提示
            SystemMessage(content=SYSTEM_PROMPT),
            # 对话历史
            MessagesPlaceholder(variable_name="chat_history", optional=True),
            # 用户问题
            HumanMessage(content="{input}"),
            # 思考过程
            MessagesPlaceholder(variable_name="agent_scratchpad")
        ])
        # 创建 Agent
        agent = create_tool_calling_agent(self.llm, TOOLS, prompt)
        self.agent_executor = AgentExecutor(agent=agent, tools=TOOLS, verbose=True)

    @retry(
        stop=stop_after_attempt(3),
        wait=wait_exponential(multiplier=1, min=2, max=10)
    )
    async def chat(self, message: str, token: str, chat_history: List[Dict] = None) -> AsyncGenerator[str, None]:
        try:
            # 先检索知识库
            knowledge_results = self.vector_store.search(message, top_k=2)

            # 构建上下文
            context = ""
            if knowledge_results:
                context = "知识库参考：\n" + "\n".join([
                    f"Q: {r['question']}\nA: {r['answer']}"
                    for r in knowledge_results
                ])

            # 合并用户问题和上下文
            input_with_context = f"{context}\n用户问题：{message}" if context else message

            # 构建配置对象，包含token
            config = {
                "configurable": {
                    "token": token
                }
            }
            print(f"传递给 Agent 的 token: {config.get('configurable', {}).get('token')}")


            # 执行 Agent
            async for event in self.agent_executor.astream_events(
                {"input": input_with_context,
                 "chat_history": chat_history or []
                 },
                config=config,
                version="v1"
            ):
                if event["event"] == "tool":
                    # 提取“工具的名字”
                    tool_name = event['data']['output']['name']
                    yield f"[tool_call]{tool_name}[/tool_call]"
                elif event["event"] == "chain_stream":
                    # 提取“生成的文字片段”
                    content = event["data"]["chunk"].content
                    yield content
        except Exception as e:
            # 处理网络连接错误
            if "Connection error" in str(e) or "APIConnectionError" in str(type(e)):
                yield "[error]网络连接错误，请检查网络设置和API配置[/error]"
            else:
                yield f"[error]发生错误：{str(e)}[/error]"
            raise

    def add_knowledge(self, question: str, answer: str, category: str = "通用") -> None:
        from knowledge.models import KnowledgeItem
        item = KnowledgeItem(question=question, answer=answer, category=category)
        self.vector_store.add_item(item)