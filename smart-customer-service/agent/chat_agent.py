# smart-customer-service/agent/chat_agent.py
from langchain.agents import AgentExecutor, create_tool_calling_agent
from langchain_core.messages import HumanMessage, SystemMessage, AIMessage
from langchain_openai import ChatOpenAI
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder
from typing import List, Dict, Any, AsyncGenerator
from dotenv import load_dotenv
import os
import json
from datetime import datetime

from tools.tool_manager import tool_manager
from utils.context import set_token
from utils.mongodb_client import mongodb_client
from agent.prompts import get_system_prompt

# 加载环境变量
load_dotenv()

# 单例模式：全局 ChatAgent 实例
chat_agent_instance = None

class ChatAgent:
    def __init__(self):
        api_key = os.getenv("LLM_API_KEY")
        base_url = os.getenv("LLM_BASE_URL", "https://api.deepseek.com/v1")

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

        # 从工具管理器获取所有启用的工具
        tools = tool_manager.get_all_tools()

        # 获取当前时间
        current_date = datetime.now().strftime("%Y年%m月%d日 %H:%M:%S")
        # 从数据库获取系统提示词
        system_prompt_template = get_system_prompt()
        filled_system_prompt = system_prompt_template.format(current_date=current_date)

        # 定义系统提示
        prompt = ChatPromptTemplate.from_messages([
            # 系统提示
            SystemMessage(content=filled_system_prompt),
            # 对话历史
            MessagesPlaceholder(variable_name="chat_history", optional=True),
            # 用户问题
            HumanMessage(content="{input}"),
            # 思考过程
            MessagesPlaceholder(variable_name="agent_scratchpad")
        ])
        # 创建 Agent
        agent = create_tool_calling_agent(self.llm, tools, prompt)
        self.agent_executor = AgentExecutor(agent=agent, tools=tools, verbose=True, return_intermediate_steps=True)

    async def chat(self, message: str, session_id: str, user_id: str = None, token: str = None) -> AsyncGenerator[str, None]:
        try:
            # 如果提供了 token，设置到 contextvars
            if token:
                set_token(token)

            processed_history = []
            
            if session_id:
                history_messages = mongodb_client.get_session_history(session_id, limit=10)
                for msg in history_messages:
                    if msg["role"] == "user":
                        processed_history.append(HumanMessage(content=msg["content"]))
                    elif msg["role"] == "assistant":
                        processed_history.append(AIMessage(content=msg["content"]))

            input_data = {
                "input": message,
                "chat_history": processed_history
            }

            current_tool_name = ""
            full_response = ""
            async for event in self.agent_executor.astream_events(
                input_data, 
                version="v1"
            ):
                event_type = event["event"]
                
                if event_type == "on_tool_start":
                    # 处理工具调用开始事件
                    # 工具名在 event['name'] 中
                    tool_name = event.get('name', '')
                    
                    # 工具参数在 event['data']['input'] 中
                    tool_input = event['data'].get('input', {})
                    
                    # 调试：打印事件结构
                    print(f"DEBUG on_tool_start event: {event}")
                    print(f"DEBUG tool_name: {tool_name}")
                    print(f"DEBUG tool_input: {tool_input}")
                    print(f"DEBUG tool_input type: {type(tool_input)}")

                    current_tool_name = tool_name
                    arguments = tool_input if isinstance(tool_input, dict) else {}

                    # 返回工具调用事件
                    yield json.dumps({
                        "type": "tool_call",
                        "tool_name": tool_name,
                        "arguments": arguments
                    }, ensure_ascii=False)
                
                elif event_type == "on_tool_end":
                    # 处理工具调用结束事件
                    tool_output = event['data']['output']
                    # 安全处理：如果 tool_output 是字符串，尝试解析
                    result = None
                    
                    if isinstance(tool_output, dict):
                        result = tool_output.get('output', None)
                    elif isinstance(tool_output, str):
                        # 如果是字符串，尝试作为结果处理
                        result = tool_output

                    # 返回结构化的工具执行结果事件
                    yield json.dumps({
                        "type": "tool_result",
                        "tool_name": current_tool_name,
                        "result": result
                    }, ensure_ascii=False)
                    current_tool_name = "" 
                
                elif event_type == "on_chat_model_stream":
                    # 处理模型直接输出的流式内容
                    chunk = event["data"]["chunk"]
                    content = ""
                    if hasattr(chunk, 'content') and chunk.content:
                        # AIMessageChunk对象
                        content = chunk.content
                    elif isinstance(chunk, str) and chunk.strip():
                        content = chunk
                    if content:
                        full_response += content
                        yield json.dumps({
                            "type": "message",
                            "content": content
                        }, ensure_ascii=False)

                elif event_type == "on_chain_end":
                    yield json.dumps({"event": "done", "data": "final"}, ensure_ascii=False)

            if full_response and session_id:
                new_messages = [
                    {"role": "user", "content": message},
                    {"role": "assistant", "content": full_response}
                ]
                mongodb_client.save_session_history(session_id, user_id, new_messages)

        except Exception as e:
            # 处理网络连接错误
            error_message = f"发生错误：{str(e)}"
            if "Connection error" in str(e) or "APIConnectionError" in str(type(e)):
                error_message = "网络连接错误，请检查网络设置和API配置"

            yield json.dumps({
                "type": "error",
                "message": error_message
            }, ensure_ascii=False)
            # 记录错误日志
            import logging
            logging.error(f"ChatAgent error: {str(e)}", exc_info=True)
            # 不重新抛出异常，避免客户端连接断开
            return

def get_chat_agent():
    """获取 ChatAgent 单例实例"""
    global chat_agent_instance
    if chat_agent_instance is None:
        chat_agent_instance = ChatAgent()
    return chat_agent_instance
