# smart-customer-service/memory/chat_history_service.py
"""
聊天记录持久化服务 —— 对话落库的唯一入口。

两条调用链路汇聚于此，保证存储逻辑只有一处：
1. AGENT 路径：ChatAgent 流式结束后直接调用 persist_turn()（Python 内部）
2. WORKFLOW 路径：Java workflow 本地处理完后调 POST /api/chat/persist，
   由该接口调用 persist_turn()（服务间回调）

职责：
  - 会话兜底创建（请求带 session_id 但会话不存在时，用该 id 建会话）
  - 追加 user / assistant 两条消息（用户原话 + 最终回复，不含任何中间步骤）
  - 首条消息后异步触发 LLM 标题回填
"""
import asyncio
import logging
from datetime import datetime

from utils.mongodb_client import mongodb_client
from memory.title_generator import TitleGenerator

logger = logging.getLogger(__name__)


class ChatHistoryService:

    async def persist_turn(self, session_id: str, user_id: str,
                           user_message: str, assistant_response: str):
        """
        持久化一轮对话（用户消息 + 助手回复），并在首轮触发标题生成。

        - user_message 必须是用户原话（不能是带记忆前缀的 enriched_message）
        - assistant_response 是最终回复（agent 的 full_response / workflow 的回复文本）
        - 任何失败仅告警，不影响主链路
        """
        if not session_id or not user_message or not assistant_response:
            return

        try:
            # 1. 会话兜底创建：前端/Java 未预先建会话时，用该 session_id 直接建
            #    （跳过空会话复用；并发撞唯一索引时 create_session 内部返回已存在文档）
            if not mongodb_client.get_session(session_id):
                mongodb_client.create_session(
                    user_id=user_id, title=None, session_id=session_id
                )

            # 2. 首条消息判断 —— 必须在 append 之前，否则 count 已 +2
            session = mongodb_client.get_session(session_id)
            is_first_message = bool(session and session.get("message_count", 0) == 0)

            # 3. 追加两条消息（先 user 后 assistant，时间戳自然递增）
            now = datetime.now()
            mongodb_client.append_message(session_id, user_id, {
                "role": "user",
                "content": user_message,
                "timestamp": now,
            })
            mongodb_client.append_message(session_id, user_id, {
                "role": "assistant",
                "content": assistant_response,
                "timestamp": now,
            })

            # 4. 首条消息 → LLM 标题回填（带 15s 超时，失败保留占位标题）
            if is_first_message:
                await self._generate_title(session_id, user_message)
        except Exception as e:
            logger.warning(f"对话持久化失败: {e}")

    async def _generate_title(self, session_id: str, first_message: str):
        try:
            title = await asyncio.wait_for(
                TitleGenerator().generate(first_message), timeout=15
            )
            if title:
                mongodb_client.update_session_title(session_id, title)
                logger.info(f"会话标题已回填: session={session_id} title={title}")
        except asyncio.TimeoutError:
            logger.warning(f"标题生成超时: session={session_id}")
        except Exception as e:
            logger.warning(f"标题回写失败: {e}")


chat_history_service = ChatHistoryService()
