# smart-customer-service/agent/chat_agent.py
"""
ChatAgent 工厂 — 按 AGENT_MODE 环境变量组装 Agent 链。

AGENT_MODE:
  react           → ReactAgent（默认，纯 ReAct）
  plan            → PlanExecuteAgent(ReactAgent)
  reflection      → ReflectionAgent(ReactAgent)
  plan+reflection → PlanExecuteAgent(ReflectionAgent(ReactAgent))
"""
import os, asyncio, logging, json
from dotenv import load_dotenv

from agent.react_agent import ReactAgent
from agent.plan_execute_agent import PlanExecuteAgent
from agent.reflection_agent import ReflectionAgent
from memory.user_memory import UserMemoryStore
from memory.memory_extractor import MemoryExtractor

load_dotenv()
logger = logging.getLogger(__name__)

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
        # ===== 对话前：注入长期记忆（Semantic 优先 + Episode 兜底） =====
        enriched_message = message
        if user_id:
            try:
                memory_store = UserMemoryStore()
                memories = memory_store.search(user_id, message, top_k=3)
                memory_texts = []

                # 1. Semantic 层（提炼后的记忆）
                if memories and memories[0].get("score", 0) > 0.3:
                    memory_texts = [f"- {m['document']}" for m in memories if m.get("score", 0) > 0.3]
                else:
                    # 2. Semantic 不够近 → 补 Episode 兜底（MongoDB 最近会话）
                    try:
                        from utils.mongodb_client import mongodb_client
                        raw = mongodb_client.get_session_history(session_id, limit=4)
                        if raw:
                            memory_texts = ["## 用户最近对话:"]
                            for h in raw:
                                memory_texts.append(f"- {h['content'][:200]}")
                    except Exception:
                        pass

                if memory_texts:
                    enriched_message = (
                        "[用户历史信息]\n" + "\n".join(memory_texts)
                        + "\n\n[当前问题]\n" + message
                    )
            except Exception as e:
                logger.debug(f"记忆检索跳过: {e}")

        # ===== Agent 执行 =====
        full_response = ""
        async for chunk in self._agent.chat(enriched_message, session_id, user_id, token):
            yield chunk
            try:
                evt = json.loads(chunk)
                if evt.get("type") == "message":
                    full_response += evt.get("content", "")
            except (json.JSONDecodeError, AttributeError):
                pass

        # ===== 对话后：提取新记忆（后台任务，不影响返回） =====
        if user_id and full_response:
            try:
                extractor = MemoryExtractor()
                asyncio.create_task(
                    extractor.extract_and_save(user_id, message, full_response)
                )
            except Exception as e:
                logger.debug(f"记忆提取跳过: {e}")


# 缓存多种模式的 Agent 实例，避免重复创建
_agent_cache = {}


def get_chat_agent(mode: str = "react"):
    if mode not in _agent_cache:
        _agent_cache[mode] = ChatAgent(mode)
    return _agent_cache[mode]
