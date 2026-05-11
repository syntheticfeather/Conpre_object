from pymongo import MongoClient
from pymongo.collection import Collection
from pymongo.database import Database
from pymongo import ReturnDocument
from dotenv import load_dotenv
import os
from typing import List, Dict, Optional
from datetime import datetime

load_dotenv()

class MongoDBClient:
    _instance = None # 保存该类的唯一实例
    _client: MongoClient = None
    _db: Database = None
    _chat_collection: Collection = None
    _prompts_collection: Collection = None
    _mcp_servers_collection: Collection = None

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
        chat_collection_name = os.getenv("MONGODB_CHAT_COLLECTION", "chat_history")
        prompts_collection_name = os.getenv("MONGODB_PROMPTS_COLLECTION", "prompts")
        mcp_servers_collection_name = os.getenv("MONGODB_MCP_COLLECTION", "mcp_servers")

        self._client = MongoClient(mongodb_url)
        self._db = self._client[db_name]
        self._chat_collection = self._db[chat_collection_name]
        self._prompts_collection = self._db[prompts_collection_name]
        self._mcp_servers_collection = self._db[mcp_servers_collection_name]

    # def get_chat_collection(self) -> Collection:
    #     return self._chat_collection

    def get_prompts_collection(self) -> Collection:
        return self._prompts_collection

    def get_session_history(self, session_id: str, limit: int = 10) -> List[Dict]:
        doc = self._chat_collection.find_one({"session_id": session_id})
        if not doc:
            return []

        messages = doc.get("messages", [])
        if limit:
            # 返回最新的 10 条
            messages = messages[-limit:]

        return messages

    def save_session_history(self, session_id: str, user_id: Optional[str], new_messages: List[Dict], max_history: int = 50) -> bool:
        try:
            now = datetime.now()
            
            # 组合更新操作
            # $push: 将新消息追加到数组末尾
            # $slice: 只保留数组的最后 max_history 条数据 (自动实现滑动窗口)
            # $set: 更新元数据
            update_query = {
                "$push": {
                    "messages": {
                        "$each": new_messages, # 支持一次插入多条消息
                        "$slice": -max_history # 核心：只保留最后 50 条，自动丢弃最旧的
                    }
                },
                "$set": {
                    "user_id": user_id,
                    "updated_at": now
                },
                "$setOnInsert": { # 仅在插入新文档时设置 (即 created_at 不会随更新改变)
                    "created_at": now
                }
            }

            # 使用 find_one_and_update 一步完成：
            # 1. 查找匹配 session_id 的文档
            # 2. 执行更新 (push + slice)
            # 3. 如果没找到 (upsert=True)，则插入新文档
            # 4. 返回更新后的文档 (return_document=AFTER)
            self._chat_collection.find_one_and_update(
                {"session_id": session_id},
                update_query,
                upsert=True,
                return_document=ReturnDocument.AFTER
            )
            
            return True

        except Exception as e:
            print(f"Error saving session history: {e}")
            return False

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
