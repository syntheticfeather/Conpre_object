# smart-customer-service/agent/reflection_agent.py
"""Reflection Agent — 对关键回复做自我审查，不通过则修正"""

import json, os, logging
from typing import AsyncGenerator

from dotenv import load_dotenv
from openai import OpenAI

from agent.base_agent import BaseAgent

load_dotenv()
logger = logging.getLogger(__name__)


class ReflectionAgent(BaseAgent):
    """包装任意 BaseAgent，对金融/推荐类回复做质量把关。"""

    SCORE_THRESHOLD = 0.7
    MAX_RETRIES = 2

    def __init__(self, inner: BaseAgent):
        self.inner = inner

    async def chat(
        self, message: str, session_id: str, user_id=None, token=None
    ) -> AsyncGenerator[str, None]:
        # 不需要反思的场景 → 直接走内层 Agent
        if not self._needs_reflection(message):
            async for chunk in self.inner.chat(message, session_id, user_id, token):
                yield chunk
            return

        # 收集完整回复
        full = ""
        chunks = []
        async for chunk in self.inner.chat(message, session_id, user_id, token):
            try:
                evt = json.loads(chunk)
                if evt.get("type") == "message":
                    full += evt.get("content", "")
                chunks.append(chunk)
            except json.JSONDecodeError:
                chunks.append(chunk)

        # 反思
        for attempt in range(self.MAX_RETRIES):
            score, issues = self._review(message, full)
            if score >= self.SCORE_THRESHOLD:
                break

            logger.info(f"Reflection: 第{attempt+1}次审查不通过 (score={score}), 修正中...")
            full = self._refine(message, full, issues)
            yield json.dumps({
                "type": "reflection",
                "attempt": attempt + 1,
                "issues": issues,
            }, ensure_ascii=False)

        # 先发修正后的文本
        yield json.dumps({"type": "message", "content": full}, ensure_ascii=False)
        # 再补发工具调用等事件
        for c in chunks:
            try:
                evt = json.loads(c)
                if evt.get("type") != "message":
                    yield c
            except json.JSONDecodeError:
                pass

        yield json.dumps({"event": "done", "data": "final"}, ensure_ascii=False)

    # ========== 辅助 ==========

    def _needs_reflection(self, message: str) -> bool:
        keywords = ["推荐", "评估", "计算", "还款", "利率", "建议", "方案", "月供"]
        return any(k in message for k in keywords)

    def _review(self, message: str, response: str) -> tuple:
        try:
            client = OpenAI(
                api_key=os.getenv("LLM_API_KEY"),
                base_url=os.getenv("LLM_BASE_URL", "https://aihubmix.com").rstrip("/") + "/v1",
            )
            prompt = f"""你是贷款风控审查员。审查以下 AI 回复，打分 0.0~1.0。

审查维度:
1. 数字/计算是否正确
2. 是否编造了数据
3. 推荐逻辑是否合理
4. 是否遗漏重要信息

用户问题: {message}
AI 回复: {response}

输出 JSON: {{"score": 0.85, "issues": "问题描述(通过则写'无')"}}"""

            resp = client.chat.completions.create(
                model="deepseek-v4-flash",
                messages=[{"role": "user", "content": prompt}],
                temperature=0.0, max_tokens=200,
            )
            content = resp.choices[0].message.content.strip()
            data = json.loads(content)
            return float(data.get("score", 0.5)), data.get("issues", "")
        except Exception as e:
            logger.warning(f"Reflection review 失败: {e}")
            return 1.0, ""  # 审查失败 → 放过

    def _refine(self, message: str, original: str, issues: str) -> str:
        try:
            client = OpenAI(
                api_key=os.getenv("LLM_API_KEY"),
                base_url=os.getenv("LLM_BASE_URL", "https://aihubmix.com").rstrip("/") + "/v1",
            )
            prompt = f"""根据审查反馈修改 AI 回复。

用户问题: {message}
原始回复: {original}
审查问题: {issues}

请输出修改后的完整回复。"""

            resp = client.chat.completions.create(
                model="deepseek-v4-flash",
                messages=[{"role": "user", "content": prompt}],
                temperature=0.3, max_tokens=1000,
            )
            return resp.choices[0].message.content.strip() or original
        except Exception as e:
            logger.warning(f"Reflection refine 失败: {e}")
            return original
