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
    ) -> AsyncGenerator[str, None]:
        """
        返回 JSON 字符串流，每块一条 {"type":"...","content":"..."}。
        和现有 chat_agent.py 的 chat() 签名一致。
        """
        ...
