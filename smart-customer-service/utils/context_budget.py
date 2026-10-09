# smart-customer-service/utils/context_budget.py
"""
对话上下文 Token 预算控制器。

按 token 占比分层注入：
  < 60%   PASS：全量原文注入
  60%-80% SOFT：最近 5 条原文 + 旧消息 LLM 摘要
  > 80%   HARD：最近 3 条原文 + 旧消息 LLM 摘要

摘要每次实时生成（on-the-fly），不落库、不缓存。
失败兜底：摘要失败时退化为「仅最近 N 条原文」，不阻塞主链路。

预算 = CONTEXT_HISTORY_BUDGET（默认 30000，含安全垫）
窗口 = CONTEXT_WINDOW_TOKENS（默认 64000，DeepSeek deepseek-v4-flash）

token 估算用 len(text) // 3 粗估（中英混合经验值），不引入 tiktoken 依赖。
"""
import os
import asyncio
import logging
from typing import List, Dict, Any

from dotenv import load_dotenv
from openai import AsyncOpenAI
from langchain_core.messages import HumanMessage, AIMessage

load_dotenv()
logger = logging.getLogger(__name__)


class ContextBudgetManager:
    """对话上下文 Token 预算控制器。"""

    # 分层阈值（占比）
    SOFT_THRESHOLD = 0.6   # < 60% PASS
    HARD_THRESHOLD = 0.8   # 60%-80% SOFT；> 80% HARD

    # 各档保留最近原文条数
    SOFT_KEEP_RECENT = 5
    HARD_KEEP_RECENT = 3

    # 摘要单条消息截断长度（防止拼成超长 prompt）
    PER_MSG_TRUNCATE = 500

    def __init__(self, budget: int = None):
        self._client = None
        self.budget = budget or int(os.getenv("CONTEXT_HISTORY_BUDGET", "30000"))

    @property
    def client(self):
        """延迟初始化 AsyncOpenAI 客户端，与 TitleGenerator 风格一致。"""
        if self._client is None:
            self._client = AsyncOpenAI(
                api_key=os.getenv("LLM_API_KEY"),
                base_url=(os.getenv("LLM_BASE_URL", "https://api.deepseek.com")
                          .rstrip("/") + "/v1"),
            )
        return self._client

    @staticmethod
    def estimate_tokens(text: str) -> int:
        """粗估 token 数：中英混合经验值 len/3。偏保守，留正向误差。"""
        if not text:
            return 0
        return max(1, len(text) // 3)

    async def build_context(self, items: List[Dict[str, Any]]) -> List:
        """
        入参：get_messages_paginated 返回的 items（按 created_at 升序，
              每条含 role/content）
        出参：LangChain Message 列表（HumanMessage/AIMessage，混合原文与摘要）

        分档逻辑：
          - items 不足 SOFT_KEEP_RECENT 条 → 直接原文（无需摘要）
          - 占比 < 60%                                   → PASS 全量原文
          - 60%-80%                                       → SOFT 最近 5 条 + 摘要
          - > 80%                                         → HARD 最近 3 条 + 摘要
          - 摘要失败                                       → 仅最近 N 条原文
        """
        if not items:
            return []

        total_tokens = sum(
            self.estimate_tokens(m.get("content", "")) for m in items
        )
        ratio = (total_tokens / self.budget) if self.budget > 0 else 0.0

        # 条数不足或占比未到 SOFT 阈值 → 全量原文
        if (ratio < self.SOFT_THRESHOLD
                or len(items) <= self.SOFT_KEEP_RECENT):
            logger.info(
                f"history tokens: {total_tokens} / {self.budget} "
                f"({ratio*100:.1f}%) → PASS 全量 {len(items)} 条原文"
            )
            return self._to_messages(items)

        # 进入摘要档
        keep = (self.SOFT_KEEP_RECENT
                if ratio < self.HARD_THRESHOLD
                else self.HARD_KEEP_RECENT)
        tier = "SOFT" if ratio < self.HARD_THRESHOLD else "HARD"
        recent = items[-keep:]
        old = items[:-keep]

        logger.info(
            f"history tokens: {total_tokens} / {self.budget} "
            f"({ratio*100:.1f}%) → {tier} 保留最近 {keep} 条 + 摘要旧 {len(old)} 条"
        )

        if not old:
            return self._to_messages(recent)

        summary = await self._summarize(old)
        if not summary:
            # 摘要失败兜底：退化为只注入最近 N 条原文，不阻塞主链路
            logger.warning("上下文摘要为空，退化为仅最近 N 条原文")
            return self._to_messages(recent)

        return ([HumanMessage(content=f"[历史对话摘要]\n{summary}")]
                + self._to_messages(recent))

    async def _summarize(self, old: List[Dict[str, Any]]) -> str:
        """
        调 deepseek-v4-flash 把旧消息列表总结为 200 字内文字。
        失败返回空串（调用方判空走兜底）。

        注意：deepseek-v4-flash 是推理模型，思考过程在 reasoning_content，
        最终答案在 content；max_tokens=1000 确保推理后仍有余量输出 content。
        """
        if not old:
            return ""

        text = "\n".join(
            f"[{m.get('role', 'user')}]: {str(m.get('content', ''))[:self.PER_MSG_TRUNCATE]}"
            for m in old
        )
        prompt = (
            "请用 200 字以内总结以下对话历史，保留关键事实"
            "（用户身份、贷款产品、金额、还款方式、已给建议），"
            "不要加入新的信息或观点。\n\n"
            f"{text}"
        )

        try:
            response = await asyncio.wait_for(
                self.client.chat.completions.create(
                    model="deepseek-v4-flash",
                    messages=[{"role": "user", "content": prompt}],
                    temperature=0.0,
                    max_tokens=1000,
                ),
                timeout=15,
            )
            content = (response.choices[0].message.content or "").strip()
            if not content:
                logger.warning("上下文摘要 content 为空，可能 max_tokens 不足或模型异常")
            return content
        except asyncio.TimeoutError:
            logger.warning("上下文摘要超时（15s），退化为最近 N 条原文")
            return ""
        except Exception as e:
            logger.warning(f"上下文摘要失败: {e}")
            return ""

    @staticmethod
    def _to_messages(items: List[Dict[str, Any]]) -> List:
        """把 chat_messages 字典列表转为 LangChain Message 列表。"""
        msgs = []
        for m in items:
            role = m.get("role")
            content = m.get("content", "")
            if role == "user":
                msgs.append(HumanMessage(content=content))
            elif role == "assistant":
                msgs.append(AIMessage(content=content))
            # tool/system 不注入到 chat_history
        return msgs
