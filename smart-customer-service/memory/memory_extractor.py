# smart-customer-service/memory/memory_extractor.py
"""
从对话中自动提取用户偏好和事实，写入长期记忆。

提取时机: 每次对话结束后异步调用
提取内容: 偏好(还款方式/风格)、事实(已申请的产品)、习惯(常用功能)
"""
import json, os, logging
from dotenv import load_dotenv
from openai import OpenAI

from memory.user_memory import UserMemoryStore

load_dotenv()
logger = logging.getLogger(__name__)


class MemoryExtractor:
    """从用户消息和 AI 回复中提取值得记忆的内容。"""

    def __init__(self):
        self._client = None
        self._store = UserMemoryStore()

    @property
    def client(self):
        if self._client is None:
            self._client = OpenAI(
                api_key=os.getenv("LLM_API_KEY"),
                base_url=(os.getenv("LLM_BASE_URL", "https://aihubmix.com")
                          .rstrip("/") + "/v1"),
            )
        return self._client

    async def extract_and_save(self, user_id: str, user_message: str,
                                ai_response: str):
        """从一轮对话中提取记忆并存储。失败静默，不影响主流程。"""
        if not user_id:
            return

        memories = self._extract(user_message, ai_response)
        for m in memories:
            self._store.save(
                user_id=user_id,
                content=m["content"],
                memory_type=m.get("type", "preference"),
                key=m.get("key", ""),
                importance=m.get("importance", 0.5),
            )
        if memories:
            logger.info(f"为用户 {user_id} 提取了 {len(memories)} 条记忆")

    def _extract(self, user_message: str, ai_response: str) -> list:
        """调 LLM 提取结构化记忆"""
        prompt = f"""从以下对话中提取值得长期记住的用户信息。只提取明确表达的，不要推测。

用户消息: {user_message}
AI 回复: {ai_response}

输出 JSON 数组，每个元素包含:
- type: preference(偏好) / fact(事实) / habit(习惯)
- content: 一句完整的话描述这条记忆，如"用户偏好等额本息还款方式"
- key: 简短关键词，如"repaid_type"
- importance: 重要程度 0.0~1.0，用户明确说过的偏好或事实给 0.7+

没有值得记住的信息则输出空数组 []。
只输出 JSON 数组，不要解释。"""

        try:
            response = self.client.chat.completions.create(
                model="deepseek-v4-flash",
                messages=[{"role": "user", "content": prompt}],
                temperature=0.0, max_tokens=500,
            )
            content = response.choices[0].message.content.strip()
            return json.loads(content)
        except Exception as e:
            logger.debug(f"记忆提取失败: {e}")
            return []
