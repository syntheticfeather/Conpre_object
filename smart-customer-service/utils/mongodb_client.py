from pymongo import MongoClient
from pymongo.collection import Collection
from pymongo.database import Database
from pymongo.errors import DuplicateKeyError
from dotenv import load_dotenv
import os
import uuid
import base64
import json
from typing import List, Dict, Optional, Tuple
from datetime import datetime, timedelta

load_dotenv()


def _encode_cursor(created_at: datetime, message_id: str) -> str:
    """将 (created_at, message_id) 编码为 base64 cursor"""
    payload = json.dumps({
        "ts": created_at.isoformat(),
        "mid": message_id,
    }, ensure_ascii=False)
    return base64.urlsafe_b64encode(payload.encode("utf-8")).decode("ascii")


def _decode_cursor(cursor: str) -> Tuple[Optional[datetime], Optional[str]]:
    """从 base64 cursor 解码出 (created_at, message_id)"""
    if not cursor:
        return None, None
    try:
        payload = base64.urlsafe_b64decode(cursor.encode("ascii")).decode("utf-8")
        obj = json.loads(payload)
        return datetime.fromisoformat(obj["ts"]), obj.get("mid")
    except Exception:
        return None, None


class MongoDBClient:
    _instance = None # 保存该类的唯一实例
    _client: MongoClient = None
    _db: Database = None
    _prompts_collection: Collection = None
    _mcp_servers_collection: Collection = None
    _sessions_collection: Collection = None   # 会话元信息（chat_sessions）
    _messages_collection: Collection = None   # 消息明细（chat_messages）

    # 创建实例
    def __new__(cls):
        if cls._instance is None:
            cls._instance = super().__new__(cls)
            cls._instance._initialize()
        return cls._instance

    # 初始化方法
    def _initialize(self):
        mongodb_url = os.getenv("MONGODB_URL", "mongodb://localhost:27017")
        db_name = os.getenv("MONGODB_DB_NAME", "smart_customer_service")
        prompts_collection_name = os.getenv("MONGODB_PROMPTS_COLLECTION", "prompts")
        mcp_servers_collection_name = os.getenv("MONGODB_MCP_COLLECTION", "mcp_servers")
        sessions_collection_name = os.getenv("MONGODB_SESSIONS_COLLECTION", "chat_sessions")
        messages_collection_name = os.getenv("MONGODB_MESSAGES_COLLECTION", "chat_messages")

        self._client = MongoClient(
            mongodb_url,
            connectTimeoutMS=5000,
            serverSelectionTimeoutMS=5000,
            ssl=False
        )
        self._db = self._client[db_name]
        self._prompts_collection = self._db[prompts_collection_name]
        self._mcp_servers_collection = self._db[mcp_servers_collection_name]
        self._sessions_collection = self._db[sessions_collection_name]
        self._messages_collection = self._db[messages_collection_name]

    def get_prompts_collection(self) -> Collection:
        return self._prompts_collection

    # ==================== 会话与消息 ====================
    # chat_sessions / chat_messages 两集合方案。

    def get_sessions_collection(self) -> Collection:
        return self._sessions_collection

    def get_messages_collection(self) -> Collection:
        return self._messages_collection

    # ---------- 会话元信息 ----------

    def create_session(self, user_id: Optional[str],
                       title: Optional[str] = None,
                       reuse_window_minutes: int = 5,
                       session_id: Optional[str] = None) -> Optional[Dict]:
        """
        新建会话，返回会话文档。title 为空时回填占位标题。

        两种调用路径：
        1. POST /api/sessions（侧栏新建）：不传 session_id，由本方法生成 uuid，
           并做"空会话复用"——
           - 当 user_id 非空且 reuse_window_minutes > 0 时，先查该用户最新的空会话
             （message_count=0，无时间限制）。命中则复用返回，不再新建。
           - 用户发首条消息后 message_count=1，下次点+ 就正常新建。
           - 传 reuse_window_minutes=0 可关闭复用，退化为"每次都新建"。
        2. ChatAgent 兜底（/chat/stream 请求已带 session_id）：传入 session_id，
           跳过复用逻辑直接用该 id 建会话；若并发已存在（唯一索引冲突），
           返回已存在的文档。这样会话 id 与请求 id 严格一致，
           is_first_message 判断和后续 append_message 都落在同一文档上。
        """
        try:
            now = datetime.now()
            fallback_title = f"新对话 {now.strftime('%Y-%m-%d %H:%M')}"
            doc = {
                "user_id": user_id,
                "title": title or fallback_title,
                "last_message_preview": "",
                "last_message_at": now,
                "message_count": 0,
                "created_at": now,
                "updated_at": now,
                "pinned": False,
                "archived": False,
            }

            # 路径 2：调用方指定了 session_id（ChatAgent 兜底），跳过复用
            if session_id:
                doc["session_id"] = session_id
                try:
                    self._sessions_collection.insert_one(dict(doc))
                except DuplicateKeyError:
                    # 并发下已被其他请求创建 → 返回已存在的文档
                    existing = self.get_session(session_id)
                    if existing:
                        return existing
                doc.pop("_id", None)
                return doc

            # 路径 1：有空会话则复用（无时间限制）→ 直接返回
            if user_id and reuse_window_minutes and reuse_window_minutes > 0:
                existing = self._sessions_collection.find_one({
                    "user_id": user_id,
                    "message_count": 0,
                }, sort=[("created_at", -1)])
                if existing:
                    existing.pop("_id", None)
                    return existing

            # 没有可复用的空会话 → 生成新 id 新建
            doc["session_id"] = str(uuid.uuid4())
            self._sessions_collection.insert_one(doc)
            doc.pop("_id", None)
            return doc
        except Exception as e:
            print(f"Error creating session: {e}")
            return None

    def get_session(self, session_id: str) -> Optional[Dict]:
        try:
            doc = self._sessions_collection.find_one({"session_id": session_id})
            if doc:
                doc.pop("_id", None)
            return doc
        except Exception as e:
            print(f"Error getting session: {e}")
            return None

    def list_sessions(self, user_id: Optional[str], cursor: Optional[str] = None,
                      limit: int = 20, include_archived: bool = False,
                      has_messages: bool = False) -> Dict:
        """
        列出某用户的会话，按 pinned desc + updated_at desc 排序。
        游标基于 updated_at（同毫秒并发极少，必要时由 session_id 二级排序兜底）。
        has_messages=True 时只返回 message_count > 0 的会话（用于"进入上次对话"场景）。
        """
        try:
            query: Dict = {"user_id": user_id}
            if not include_archived:
                query["archived"] = {"$ne": True}
            if has_messages:
                query["message_count"] = {"$gt": 0}

            cur_ts, _ = _decode_cursor(cursor) if cursor else (None, None)
            if cur_ts:
                query["updated_at"] = {"$lt": cur_ts}

            cursor_obj = self._sessions_collection.find(query) \
                .sort([("pinned", -1), ("updated_at", -1)]) \
                .limit(limit + 1)

            items = []
            for d in cursor_obj:
                d.pop("_id", None)
                items.append(d)

            has_more = len(items) > limit
            if has_more:
                items = items[:limit]

            next_cursor = None
            if has_more and items:
                last = items[-1]
                next_cursor = _encode_cursor(last.get("updated_at", datetime.now()),
                                             last.get("session_id", ""))

            return {"items": items, "next_cursor": next_cursor, "has_more": has_more}
        except Exception as e:
            print(f"Error listing sessions: {e}")
            return {"items": [], "next_cursor": None, "has_more": False}

    def update_session_meta(self, session_id: str, **fields) -> bool:
        """更新会话元信息字段：title / pinned / archived 等。"""
        if not fields:
            return False
        try:
            fields["updated_at"] = datetime.now()
            result = self._sessions_collection.update_one(
                {"session_id": session_id},
                {"$set": fields}
            )
            return result.modified_count > 0 or result.upserted_id is not None
        except Exception as e:
            print(f"Error updating session meta: {e}")
            return False

    def rename_session(self, session_id: str, title: str) -> bool:
        return self.update_session_meta(session_id, title=title)

    def pin_session(self, session_id: str, pinned: bool) -> bool:
        return self.update_session_meta(session_id, pinned=pinned)

    def archive_session(self, session_id: str, archived: bool) -> bool:
        return self.update_session_meta(session_id, archived=archived)

    def delete_session(self, session_id: str) -> bool:
        """删除会话：同时删 chat_sessions + 该会话下全部 chat_messages。"""
        try:
            with self._client.start_session() as s:
                with s.start_transaction():
                    self._messages_collection.delete_many({"session_id": session_id})
                    self._sessions_collection.delete_one({"session_id": session_id})
            return True
        except Exception as e:
            print(f"Error deleting session: {e}")
            return False

    def update_session_title(self, session_id: str, title: str) -> bool:
        """LLM 摘要回填 title 专用入口。"""
        return self.update_session_meta(session_id, title=title)

    # ---------- 消息明细 ----------

    def append_message(self, session_id: str, user_id: Optional[str],
                       message: Dict) -> bool:
        """
        单条消息插入 + 同步更新会话元信息（事务）。
        message 应至少包含 role / content；其余字段自动补全。
        """
        try:
            now = datetime.now()
            msg_doc = {
                "message_id": message.get("message_id") or str(uuid.uuid4()),
                "session_id": session_id,
                "user_id": user_id,
                "role": message.get("role", "user"),
                "content": message.get("content", ""),
                "created_at": message.get("timestamp") or message.get("created_at") or now,
            }

            preview = (msg_doc["content"] or "")[:50]

            with self._client.start_session() as s:
                with s.start_transaction():
                    self._messages_collection.insert_one(msg_doc)
                    # upsert=True：会话不存在时自动创建。
                    # $setOnInsert 仅在插入时生效，给新会话补齐默认元信息
                    # （title/pinned/archived/created_at/user_id），
                    # 避免裸 session_id 直接发消息时产生缺字段的会话文档
                    # （否则 list_sessions 按 user_id 过滤会查不到）。
                    self._sessions_collection.update_one(
                        {"session_id": session_id},
                        {
                            "$inc": {"message_count": 1},
                            "$set": {
                                "last_message_preview": preview,
                                "last_message_at": msg_doc["created_at"],
                                "updated_at": now,
                            },
                            "$setOnInsert": {
                                "user_id": user_id,
                                "title": f"新对话 {now.strftime('%Y-%m-%d %H:%M')}",
                                "created_at": now,
                                "pinned": False,
                                "archived": False,
                            },
                        },
                        upsert=True,
                    )
            return True
        except Exception as e:
            print(f"Error appending message: {e}")
            return False

    def get_messages_paginated(self, session_id: str, cursor: Optional[str] = None,
                               limit: int = 30) -> Dict:
        """
        分页查询某会话的消息（仅向上加载更早）。

        - 首屏无 cursor → 返回最新 N 条（created_at desc，再反转为升序）
        - 带 cursor → created_at < cursor.ts（同毫秒用 message_id 兜底）
        - 返回 items 已按 created_at 升序排好，前端"从上到下"直接渲染
        - next_cursor 指向当前列表最顶（最早）那条，用于下一次上滑加载
        """
        try:
            query: Dict = {"session_id": session_id}
            cur_ts, cur_mid = _decode_cursor(cursor) if cursor else (None, None)

            if cursor and cur_ts:
                query["$or"] = [
                    {"created_at": {"$lt": cur_ts}},
                    {"created_at": cur_ts, "message_id": {"$lt": cur_mid or ""}},
                ]

            # 统一用 desc 取，最后反转为升序
            cursor_obj = self._messages_collection.find(query) \
                .sort([("created_at", -1), ("message_id", -1)]) \
                .limit(limit + 1)

            items = []
            for d in cursor_obj:
                d.pop("_id", None)
                items.append(d)

            has_more = len(items) > limit
            if has_more:
                items = items[:limit]

            # 反转为升序，方便前端从上到下渲染
            items.reverse()

            next_cursor = None
            if has_more and items:
                # 上滑加载更早，next_cursor 指向当前列表最顶（最早）那条
                edge = items[0]
                next_cursor = _encode_cursor(edge.get("created_at", datetime.now()),
                                             edge.get("message_id", ""))

            return {"items": items, "next_cursor": next_cursor, "has_more": has_more}
        except Exception as e:
            print(f"Error paginating messages: {e}")
            return {"items": [], "next_cursor": None, "has_more": False}

    def get_active_prompt(self, category: str = "loan_customer_service") -> Optional[Dict]:
        """获取当前激活的提示词"""
        try:
            prompt = self._prompts_collection.find_one(
                {"category": category, "is_active": True},  # 默认类别：loan_customer_service（贷款客服）
                sort=[("version", -1)] # 版本降序
            )
            return prompt
        except Exception as e:
            print(f"Error getting active prompt: {e}")
            return None

    def get_prompt_by_id(self, prompt_id: str) -> Optional[Dict]:
        """根据ID获取提示词"""
        try:
            prompt = self._prompts_collection.find_one({"prompt_id": prompt_id})
            return prompt
        except Exception as e:
            print(f"Error getting prompt by id: {e}")
            return None

    def create_prompt(self, prompt_data: Dict) -> bool:
        """创建新的提示词"""
        try:
            # 生成唯一的prompt_id（如果没有提供）
            if "prompt_id" not in prompt_data:
                import uuid
                prompt_data["prompt_id"] = str(uuid.uuid4())

            # 插入新提示词
            self._prompts_collection.insert_one(prompt_data)
            # update_many + insert_one 之间不是原子的，极端并发时可能有短暂的双激活状态
            return True
        except Exception as e:
            print(f"Error creating prompt: {e}")
            return False

    def update_prompt(self, prompt_id: str, update_data: Dict) -> bool:
        """更新提示词"""
        try:
            update_data["updated_at"] = datetime.now()
            
            # 如果设置为激活状态，先停用其他同类别提示词
            if update_data.get("is_active", False):
                prompt = self.get_prompt_by_id(prompt_id)
                if prompt:
                    self._prompts_collection.update_many(
                        {"category": prompt.get("category", "loan_customer_service"), "is_active": True, "prompt_id": {"$ne": prompt_id}},
                        {"$set": {"is_active": False, "updated_at": update_data["updated_at"]}}
                    )
            
            result = self._prompts_collection.update_one(
                {"prompt_id": prompt_id},
                {"$set": update_data}
            )
            return result.modified_count > 0
        except Exception as e:
            print(f"Error updating prompt: {e}")
            return False

    def deactivate_prompt(self, prompt_id: str) -> bool:
        """停用提示词"""
        try:
            result = self._prompts_collection.update_one(
                {"prompt_id": prompt_id},
                {"$set": {"is_active": False, "updated_at": datetime.now()}}
            )
            return result.modified_count > 0
        except Exception as e:
            print(f"Error deactivating prompt: {e}")
            return False

    # MCP服务器配置管理

    def save_mcp_server(self, server_id: str, config: dict) -> bool:
        """保存或更新MCP服务器配置"""
        try:
            now = datetime.now()
            update_data = {
                "$set": {
                    "config": config,
                    "updated_at": now
                },
                "$setOnInsert": {
                    "server_id": server_id,
                    "created_at": now
                }
            }
            self._mcp_servers_collection.update_one(
                {"server_id": server_id},
                update_data,
                upsert=True
            )
            return True
        except Exception as e:
            print(f"Error saving MCP server: {e}")
            return False

    def get_mcp_server(self, server_id: str) -> Optional[Dict]:
        """获取MCP服务器配置"""
        try:
            return self._mcp_servers_collection.find_one({"server_id": server_id})
        except Exception as e:
            print(f"Error getting MCP server: {e}")
            return None

    def get_all_mcp_servers(self) -> List[Dict]:
        """获取所有MCP服务器配置"""
        try:
            servers = []
            for doc in self._mcp_servers_collection.find():
                doc["_id"] = str(doc["_id"])
                servers.append(doc)
            return servers
        except Exception as e:
            print(f"Error getting all MCP servers: {e}")
            return []

    def delete_mcp_server(self, server_id: str) -> bool:
        """删除MCP服务器配置"""
        try:
            result = self._mcp_servers_collection.delete_one({"server_id": server_id})
            return result.deleted_count > 0
        except Exception as e:
            print(f"Error deleting MCP server: {e}")
            return False

    def close(self):
        if self._client:
            self._client.close() # 断开与 MongoDB 的所有连接，清空连接池。

mongodb_client = MongoDBClient() # 实例化一次
