# smart-customer-service/agent/chat_agent.py
"""
ChatAgent 工厂 — 按 AGENT_MODE 环境变量组装 Agent 链。

AGENT_MODE:
  react           → ReactAgent（默认，纯 ReAct）
  plan            → PlanExecuteAgent(ReactAgent)
  reflection      → ReflectionAgent(ReactAgent)
  plan+reflection → PlanExecuteAgent(ReflectionAgent(ReactAgent))
"""
import os
from dotenv import load_dotenv

from agent.react_agent import ReactAgent
from agent.plan_execute_agent import PlanExecuteAgent
from agent.reflection_agent import ReflectionAgent

load_dotenv()

_chat_agent_instance = None


class ChatAgent:
    """对外统一入口，内部按 mode 组装 Agent 链。"""

    def __init__(self, mode: str = "react"):
        react = ReactAgent()

        if mode == "react":
            self._agent = react
        elif mode == "plan":
            self._agent = PlanExecuteAgent(react)
        elif mode == "reflection":
            self._agent = ReflectionAgent(react)
        elif mode == "plan+reflection":
            self._agent = PlanExecuteAgent(ReflectionAgent(react))
        else:
            self._agent = react

    async def chat(self, message: str, session_id: str, user_id: str = None, token: str = None):
        async for chunk in self._agent.chat(message, session_id, user_id, token):
            yield chunk


# 缓存多种模式的 Agent 实例，避免重复创建
_agent_cache = {}


def get_chat_agent(mode: str = "react"):
    if mode not in _agent_cache:
        _agent_cache[mode] = ChatAgent(mode)
    return _agent_cache[mode]
