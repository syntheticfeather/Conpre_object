# smart-customer-service/memory/title_generator.py
"""
把首条用户消息生成简短会话标题，回写 sessions.title。

调用时机：ChatAgent.chat() 检测到 message_count==0（首条消息）时，
          直接 await 异步触发（带 15s 超时），不阻塞主响应流式输出。
失败静默：保留占位标题"新对话 yyyy-MM-dd HH:mm"，不影响主流程。
"""
import os, logging
from dotenv import load_dotenv
from openai import AsyncOpenAI

load_dotenv()
logger = logging.getLogger(__name__)


class TitleGenerator:
    """把首条用户消息生成 10-15 字会话标题。"""

    def __init__(self):
        self._client = None

    @property
    def client(self):
        # 延迟初始化，避免进程启动时就连 LLM
        if self._client is None:
            self._client = AsyncOpenAI(
                api_key=os.getenv("LLM_API_KEY"),
                base_url=(os.getenv("LLM_BASE_URL", "https://api.deepseek.com")
                          .rstrip("/") + "/v1"),
            )
        return self._client

    async def generate(self, first_message: str) -> str:
        """
        返回生成的标题文本；失败返回空串（调用方判空跳过回写）。

        - 10-15 字，中文，概括用户意图
        - 去引号、去句号、截断到 20 字兜底
        """
        prompt = f"""请把以下用户消息概括为一个简短的会话标题。

要求：
- 10-15 字以内，中文
- 概括用户意图，不要照抄原文
- 只输出标题文本，不要引号、不要解释、不要句号结尾

用户消息：{first_message[:200]}"""
        try:
            response = await self.client.chat.completions.create(
                model="deepseek-v4-flash",
                messages=[{"role": "user", "content": prompt}],
                temperature=0.0,
                max_tokens=1000,
            )
            msg = response.choices[0].message
            # deepseek-v4-flash 是推理模型：思考过程在 reasoning_content，
            # 最终答案在 content。只取 content，不取 reasoning_content。
            # max_tokens 设大确保推理后仍有余量输出 content。
            title = (msg.content or "").strip()
            # 清理：去引号、去句号、截断
            title = title.strip('"\'""').rstrip('。.').strip()
            if not title:
                logger.warning(f"标题 content 为空，可能 max_tokens 不足或模型异常")
            return title[:20] if title else ""
        except Exception as e:
            logger.warning(f"标题生成失败: {e}")
            return ""
