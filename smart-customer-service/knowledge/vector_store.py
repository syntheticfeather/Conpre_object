# smart-customer-service/knowledge/vector_store.py
import chromadb
from chromadb.config import Settings
from typing import List, Optional
from knowledge.models import KnowledgeItem

class VectorStore:
    # 初始化方法
    def __init__(self, persist_directory: str = "./chroma_db"):
        # 创建一个客户端，并将所有数据存储在 persist_directory 目录下
        self.client = chromadb.PersistentClient(
            path=persist_directory,
            settings=chromadb.Settings(anonymized_telemetry=False) # 禁用匿名遥测
        )
        # 获取或创建集合（表）
        self.collection = self.client.get_or_create_collection(
            name="knowledge_base",
            metadata={"description": "智能客服知识库"} # 添加描述信息
        )

    # 添加知识项
    def add_item(self, item: KnowledgeItem) -> None:
        combined_text = f"问题: {item.question}\n答案: {item.answer}"
        self.collection.add(
            documents=[combined_text], # 存入组合文本
            metadatas=[
                {
                    "question": item.question, # 原始问题仍保留在元数据中，方便前端展示
                    "answer": item.answer,     # 原始答案也保留
                    "category": item.category
                }
            ],
            ids=[item.id]
        )

    # 搜索知识项
    def search(self, query: str, top_k: int = 3) -> List[dict]:
        results = self.collection.query(
            query_texts=[query],
            n_results=top_k
        )
        return [
            {
                "id": results["ids"][0][i],
                "question": results["metadatas"][0][i]["question"], # 从元数据取原始问题
                "answer": results["metadatas"][0][i]["answer"],     # 从元数据取原始答案
                "category": results["metadatas"][0][i]["category"]
            }
            for i in range(len(results["ids"][0]))
        ]

    # 删除知识项
    def delete(self, item_id: str) -> None:
        self.collection.delete(ids=[item_id])

    # 获取所有知识项
    def get_all(self) -> List[dict]:
        # 获取所有数据
        results = self.collection.get()
        
        # 确保数据存在
        if not results["ids"]:
            return []
        
        return [
            {
                "id": results["ids"][i],
                 "question": results["metadatas"][i]["question"], # 从元数据取原始问题
                 "answer": results["metadatas"][i]["answer"],     # 从元数据取原始答案
                "category": results["metadatas"][i].get("category", "通用") # 使用 get 防止 category 缺失
            }
            for i in range(len(results["ids"]))
        ]