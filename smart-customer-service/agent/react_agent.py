# smart-customer-service/agent/react_agent.py
"""ReAct Agent — 纯 while 循环，不做 Plan/Reflection"""

import os, json, uuid, logging
from datetime import datetime
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
            model="gpt-4.1-mini-free",
            api_key=api_key,
            base_url=base_url,
            temperature=0.7,
            http_client=http_client,
        )

        tools = tool_manager.get_all_tools()
        system_prompt_template = get_system_prompt()

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
        self.system_prompt_template = system_prompt_template

    # ========== 核心 ==========

    async def chat(
        self,
        message: str,
        session_id: str,
        user_id: str = None,
        token: str = None,
    ) -> AsyncGenerator[str, None]:
        try:
            if token:
                set_token(token)

            # 恢复历史
            processed_history = []
            if session_id:
                for msg in mongodb_client.get_session_history(session_id, limit=10):
                    if msg["role"] == "user":
                        processed_history.append(HumanMessage(content=msg["content"]))
                    elif msg["role"] == "assistant":
                        processed_history.append(AIMessage(content=msg["content"]))

            # 填系统提示
            current_date = datetime.now().strftime("%Y年%m月%d日 %H:%M:%S")
            tools = tool_manager.get_all_tools()
            tools_desc = "\n".join(f"- `{t.name}`: {t.description}" for t in tools)
            filled_system_prompt = self.system_prompt_template.format(
                current_date=current_date, tools_description=tools_desc
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

            # 存历史
            if full_response and session_id:
                mongodb_client.save_session_history(session_id, user_id, [
                    {"message_id": str(uuid.uuid4()), "role": "user", "content": message, "timestamp": datetime.now()},
                    {"message_id": str(uuid.uuid4()), "role": "assistant", "content": full_response, "timestamp": datetime.now()},
                ])

        except Exception as e:
            error_msg = f"发生错误：{str(e)}"
            if "Connection error" in str(e) or "APIConnectionError" in str(type(e)):
                error_msg = "网络连接错误，请检查网络设置和API配置"
            yield json.dumps({"type": "error", "message": error_msg}, ensure_ascii=False)
            logger.error(f"ReactAgent error: {str(e)}", exc_info=True)
