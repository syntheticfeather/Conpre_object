# smart-customer-service/agent/react_agent.py
"""ReAct Agent — 纯 while 循环，不做 Plan/Reflection"""

import os, json, logging
from typing import AsyncGenerator

from dotenv import load_dotenv
from langchain.agents import AgentExecutor, create_tool_calling_agent
from langchain_openai import ChatOpenAI
from langchain_core.messages import HumanMessage, AIMessage
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder

from agent.base_agent import BaseAgent
from agent.prompts import get_system_prompt
from tools.tool_manager import tool_manager
from utils.context import set_token
from utils.context_budget import ContextBudgetManager
from utils.mongodb_client import mongodb_client

load_dotenv()
logger = logging.getLogger(__name__)


class ReactAgent(BaseAgent):
    """标准 ReAct Agent。可被 PlanExecuteAgent / ReflectionAgent 包装。"""

    def __init__(self):
        api_key = os.getenv("LLM_API_KEY")
        base_url = os.getenv("LLM_BASE_URL", "https://api.deepseek.com/v1")

        from httpx import Client
        http_client = Client(timeout=30.0, verify=True)

        self.llm = ChatOpenAI(
            model="deepseek-v4-flash",
            api_key=api_key,
            base_url=base_url,
            temperature=0.7,
            http_client=http_client,
        )

        tools = tool_manager.get_all_tools()

        prompt = ChatPromptTemplate.from_messages([
            ("system", "{system_prompt}"),
            MessagesPlaceholder(variable_name="chat_history", optional=True),
            ("human", "{input}"),
            MessagesPlaceholder(variable_name="agent_scratchpad"),
        ])

        agent = create_tool_calling_agent(self.llm, tools, prompt)
        self.agent_executor = AgentExecutor(
            agent=agent, tools=tools, verbose=True, return_intermediate_steps=True
        )
        # 注意：系统提示词不在构造期固化——每次 chat 按意图实时组装，
        # 管理后台改提示词后最多 TTL(60s) 生效，无需重启进程。

    # ========== 核心 ==========

    async def chat(
        self,
        message: str,
        session_id: str,
        user_id: str = None,
        token: str = None,
        intent: str = None,
        confidence: float = 1.0,
    ) -> AsyncGenerator[str, None]:
        try:
            if token:
                set_token(token)

            # 恢复历史（chat_messages 集合分页查询，返回最新 N 条且已升序）
            # 注入前经 ContextBudgetManager 按 token 预算分层处理：
            #   < 60% 原样；60%-80% 最近 5 条+摘要；> 80% 最近 3 条+摘要
            # 摘要每次实时生成（on-the-fly），不落库；失败退化为最近 N 条原文。
            processed_history = []
            if session_id:
                history = mongodb_client.get_messages_paginated(
                    session_id, limit=10
                ).get("items", [])
                budget_mgr = ContextBudgetManager()
                processed_history = await budget_mgr.build_context(history)

            # 每次请求实时组装系统提示词（五层架构；热更新；含意图专属片段）
            tools = tool_manager.get_all_tools()
            filled_system_prompt = get_system_prompt(
                intent=intent, confidence=confidence, tools=tools
            )

            input_data = {
                "input": message,
                "system_prompt": filled_system_prompt,
                "chat_history": processed_history,
            }

            # Agent 循环
            full_response = ""
            current_tool_name = ""
            async for event in self.agent_executor.astream_events(input_data, version="v2"):
                event_type = event["event"]

                if event_type == "on_tool_start":
                    current_tool_name = event.get("name", "")
                    tool_input = event["data"].get("input", {})
                    yield json.dumps({
                        "type": "tool_call",
                        "tool_name": current_tool_name,
                        "arguments": tool_input if isinstance(tool_input, dict) else {},
                    }, ensure_ascii=False)

                elif event_type == "on_tool_end":
                    output = event["data"]["output"]
                    result = output.get("output", output) if isinstance(output, dict) else output
                    yield json.dumps({
                        "type": "tool_result",
                        "tool_name": current_tool_name,
                        "result": result,
                    }, ensure_ascii=False)
                    current_tool_name = ""

                elif event_type == "on_chat_model_stream":
                    chunk = event["data"]["chunk"]
                    content = getattr(chunk, "content", None) or ""
                    if content:
                        full_response += content
                        yield json.dumps({"type": "message", "content": content}, ensure_ascii=False)

                elif event_type == "on_chain_end":
                    yield json.dumps({"event": "done", "data": "final"}, ensure_ascii=False)

            # 存储职责已统一上移到 ChatAgent：内层 Agent 不再写 chat_messages，
            # 避免 plan/reflection 的中间步骤消息污染聊天记录。
            # ChatAgent 在流式结束后统一 append「用户原话 + 最终 full_response」。

        except Exception as e:
            error_msg = f"发生错误：{str(e)}"
            if "Connection error" in str(e) or "APIConnectionError" in str(type(e)):
                error_msg = "网络连接错误，请检查网络设置和API配置"
            yield json.dumps({"type": "error", "message": error_msg}, ensure_ascii=False)
            logger.error(f"ReactAgent error: {str(e)}", exc_info=True)
