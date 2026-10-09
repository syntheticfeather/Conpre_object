# smart-customer-service/agent/base_agent.py
"""Agent 基类 — 统一 chat() 接口，支持任意嵌套组合"""
from abc import ABC, abstractmethod
from typing import AsyncGenerator


class BaseAgent(ABC):

    @abstractmethod
    async def chat(
        self,
        message: str,
        session_id: str,
        user_id: str = None,
        token: str = None,
        intent: str = None,
        confidence: float = 1.0,
    ) -> AsyncGenerator[str, None]:
        """
        返回 JSON 字符串流，每块一条 {"type":"...","content":"..."}。
        intent/confidence 为 Router 分类结果（Java 透传），用于意图化系统提示词；
        缺省时等价于改造前行为（general 片段）。
        """
        ...
