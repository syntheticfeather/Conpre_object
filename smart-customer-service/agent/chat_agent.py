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
from memory.chat_history_service import chat_history_service
from utils.mongodb_client import mongodb_client

load_dotenv()
logger = logging.getLogger(__name__)

_chat_agent_instance = None


class ChatAgent:
    """对外统一入口，内部按 mode 组装 Agent 链。"""

    def __init__(self, mode: str = "react"):
        self.mode = mode   # 记住模式，供日志/调试参考；会话不持久化 agent_mode
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

    async def chat(self, message: str, session_id: str, user_id: str = None,
                   token: str = None, intent: str = None, confidence: float = 1.0):
        # ===== 对话前：注入长期记忆（Semantic 优先 + Episode 兜底） =====
        # 会话兜底创建 / 首条判断 / 落库 / 标题生成统一由 ChatHistoryService 处理，
        # WORKFLOW 路径（Java 本地处理后回调 /api/chat/persist）也走同一服务。
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
                        raw = mongodb_client.get_messages_paginated(
                            session_id, limit=4
                        ).get("items", [])
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
        # intent/confidence 为 Router 分类结果（Java 原样透传），缺省时内层按 general 组装
        full_response = ""
        async for chunk in self._agent.chat(
            enriched_message, session_id, user_id, token,
            intent=intent, confidence=confidence,
        ):
            yield chunk
            try:
                evt = json.loads(chunk)
                if evt.get("type") == "message":
                    full_response += evt.get("content", "")
            except (json.JSONDecodeError, AttributeError):
                pass

        # ===== 对话后：统一持久化（对话落库唯一入口） =====
        # 服务内部完成：会话兜底创建 → 追加「用户原话 + 最终答案」两条 → 首条消息 LLM 标题回填。
        # 注意存的是原始 message，不是带记忆前缀的 enriched_message；
        # 内层 agent（react/plan/reflection）均不写库，中间步骤不会落库。
        if full_response and session_id:
            await chat_history_service.persist_turn(
                session_id, user_id, message, full_response
            )

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
