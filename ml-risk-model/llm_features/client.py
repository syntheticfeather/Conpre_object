"""LLM 客户端抽象与 OpenAI 兼容实现。"""
from __future__ import annotations

import os
from typing import Optional, Protocol

from llm_features.config import load_env

# 导入时加载一次 .env；已存在的 shell 环境变量优先
load_env()

DEFAULT_MODEL = "deepseek-flash"
DEFAULT_BASE_URL = "https://api.deepseek.com"


class LLMClient(Protocol):
    """抽取器只依赖这个最小接口，便于测试注入。"""

    def complete(self, system: str, user: str) -> str:
        ...


class OpenAICompatibleClient:
    """通过 OpenAI 兼容接口调用项目现有 LLM。"""

    def __init__(
        self,
        api_key: Optional[str] = None,
        base_url: Optional[str] = None,
        model: Optional[str] = None,
        timeout: float = 60.0,
    ):
        self.api_key = (
            api_key
            or os.getenv("LLM_API_KEY")
            or os.getenv("OPENAI_API_KEY")
        )
        if not self.api_key:
            raise ValueError(
                "缺少 LLM_API_KEY 或 OPENAI_API_KEY，"
                "无 Key 时请使用 --mock 运行批处理。"
            )
        self.base_url = base_url or os.getenv("LLM_BASE_URL", DEFAULT_BASE_URL)
        self.model = model or os.getenv("LLM_MODEL", DEFAULT_MODEL)
        self.thinking = os.getenv("LLM_THINKING", "").strip()
        self.reasoning_effort = os.getenv("LLM_REASONING_EFFORT", "").strip()

        from openai import OpenAI

        self._client = OpenAI(
            api_key=self.api_key,
            base_url=self.base_url,
            timeout=timeout,
        )

    def _extra_body(self) -> dict:
        extra = {}
        if self.thinking:
            extra["thinking"] = {"type": self.thinking}
        if self.reasoning_effort:
            extra["reasoning_effort"] = self.reasoning_effort
        return extra

    def complete(self, system: str, user: str) -> str:
        messages = [
            {"role": "system", "content": system},
            {"role": "user", "content": user},
        ]
        base = {
            "model": self.model,
            "messages": messages,
            "temperature": 0,
        }
        extra_body = self._extra_body()

        # 依次降级：JSON+思考参数 → 去掉 JSON → 去掉思考参数 → 纯调用
        variants = []
        for use_json in (True, False):
            for use_extra in (True, False):
                if use_extra and not extra_body:
                    continue
                kwargs = dict(base)
                if use_json:
                    kwargs["response_format"] = {"type": "json_object"}
                if use_extra:
                    kwargs["extra_body"] = extra_body
                variants.append(kwargs)

        last_error: Optional[Exception] = None
        for kwargs in variants:
            try:
                response = self._client.chat.completions.create(**kwargs)
                content = response.choices[0].message.content
                if not content:
                    raise RuntimeError("LLM 返回了空内容")
                return content
            except Exception as exc:
                last_error = exc
                message = str(exc).lower()
                if not any(
                    hint in message
                    for hint in (
                        "response_format",
                        "json",
                        "thinking",
                        "reasoning",
                        "extra_body",
                        "extra fields",
                        "unknown parameter",
                        "unsupported parameter",
                    )
                ):
                    raise
        raise last_error if last_error else RuntimeError("LLM 调用失败")
