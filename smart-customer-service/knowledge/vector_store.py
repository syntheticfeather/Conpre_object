# smart-customer-service/knowledge/vector_store.py
from typing import List, Optional
from knowledge.models import KnowledgeItem
from utils.chromadb_client import chromadb_client

class VectorStore:
    # 初始化方法
    def __init__(self, persist_directory: str = "./chroma_db"):
        # 使用全局的 chromadb 客户端
        self.client = chromadb_client

    # 添加知识项
    def add_item(self, item: KnowledgeItem) -> None:
        combined_text = f"问题: {item.question}\n答案: {item.answer}"
        metadata = {
            "question": item.question, # 原始问题仍保留在元数据中，方便前端展示
            "answer": item.answer,     # 原始答案也保留
            "category": item.category
        }
        self.client.add_item(item.id, combined_text, metadata)

    # 搜索知识项
    def search(self, query: str, top_k: int = 3) -> List[dict]:
        results = self.client.search(query, top_k)
        return [
            {
                "id": result["id"],
                "question": result["metadata"]["question"], # 从元数据取原始问题
                "answer": result["metadata"]["answer"],     # 从元数据取原始答案
                "category": result["metadata"]["category"]
            }
            for result in results
        ]

    # 删除知识项
    def delete(self, item_id: str) -> None:
        self.client.delete(item_id)

    # 获取所有知识项
    def get_all(self) -> List[dict]:
        results = self.client.get_all()
        return [
            {
                "id": result["id"],
                "question": result["metadata"]["question"], # 从元数据取原始问题
                "answer": result["metadata"]["answer"],     # 从元数据取原始答案
                "category": result["metadata"].get("category", "通用") # 使用 get 防止 category 缺失
            }
            for result in results
        ]
    
    # 更新知识项
    def update(self, item: KnowledgeItem) -> None:
        combined_text = f"问题: {item.question}\n答案: {item.answer}"
        metadata = {
            "question": item.question,
            "answer": item.answer,
            "category": item.category
        }
        self.client.update(item.id, combined_text, metadata)