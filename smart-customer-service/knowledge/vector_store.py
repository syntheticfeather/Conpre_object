# smart-customer-service/knowledge/vector_store.py
from typing import List, Optional, Dict
from knowledge.models import KnowledgeItem
from utils.chromadb_client import chromadb_client

class VectorStore:
    """向量存储封装类 - 单例模式"""
    
    _instance = None
    
    def __new__(cls, persist_directory: str = "./chroma_db_data"):
        if cls._instance is None:
            cls._instance = super(VectorStore, cls).__new__(cls)
            cls._instance._initialize(persist_directory)
        return cls._instance
    
    def _initialize(self, persist_directory: str):
        """初始化向量存储（仅在首次创建时调用）"""
        # 使用全局的 chromadb 客户端
        self.client = chromadb_client
    
    # 添加知识项
    def add_item(self, item: KnowledgeItem) -> None:
        combined_text = f"问题: {item.question}\n答案: {item.answer}"
        metadata = {
            "question": item.question, # 原始问题仍保留在元数据中，方便前端展示
            "answer": item.answer,     # 原始答案也保留
            "category": item.category,
            "source_type": "faq"       # 标识为FAQ类型
        }
        self.client.add_item(item.id, combined_text, metadata)

    # 搜索知识项
    def search(self, query: str, top_k: int = 3) -> List[dict]:
        results = self.client.search(query, top_k)
        formatted_results = []
        
        for result in results:
            metadata = result["metadata"]
            source_type = metadata.get("source_type", "faq")
            
            if source_type == "faq":
                # FAQ 问答对
                formatted_results.append({
                    "id": result["id"],
                    "type": "faq",
                    "question": metadata.get("question", ""),
                    "answer": metadata.get("answer", ""),
                    "category": metadata.get("category", "通用"),
                    "source_type": "faq"
                })
            elif source_type == "document":
                # 文档分块
                formatted_results.append({
                    "id": result["id"],
                    "type": "document",
                    "document_name": metadata.get("document_name", ""),
                    "section": metadata.get("section", ""),
                    "section_path": metadata.get("section_path", ""),
                    "content": metadata.get("content", ""),
                    "source_type": "document"
                })
            else:
                # 其他类型，返回原始内容
                formatted_results.append({
                    "id": result["id"],
                    "type": "other",
                    "document": result.get("document", ""),
                    "metadata": metadata
                })
        
        return formatted_results

    # 删除知识项
    def delete(self, item_id: str) -> None:
        self.client.delete(item_id)
    
    # 按元数据条件删除（用于删除整篇文档）
    def delete_by_metadata(self, where: Dict[str, any]) -> int:
        """按元数据条件删除数据
        
        Args:
            where: 元数据条件字典，如 {"document_name": "法律条例与合规声明"}
            
        Returns:
            成功删除的数量
        """
        return self.client.delete_by_metadata(where)

    # 获取所有知识项（兼容旧接口）
    def get_all(self) -> List[dict]:
        results = self.client.get_all()
        items = []
        for result in results:
            source_type = result["metadata"].get("source_type", "faq")
            if source_type == "faq":
                items.append({
                    "id": result["id"],
                    "question": result["metadata"]["question"],
                    "answer": result["metadata"]["answer"],
                    "category": result["metadata"].get("category", "通用"),
                    "source_type": "faq"
                })
            else:
                items.append({
                    "id": result["id"],
                    "document_name": result["metadata"]["document_name"],
                    "section": result["metadata"]["section"],
                    "section_level": result["metadata"]["section_level"],
                    "section_path": result["metadata"]["section_path"],
                    "chunk_index": result["metadata"]["chunk_index"],
                    "source_path": result["metadata"]["source_path"],
                    "content": result["metadata"]["content"],
                    "source_type": "document"
                })
        return items
    
    # 获取所有问答对知识项
    def get_all_faq(self) -> List[dict]:
        results = self.client.get_all()
        return [
            {
                "id": result["id"],
                "question": result["metadata"]["question"],
                "answer": result["metadata"]["answer"],
                "category": result["metadata"].get("category", "通用"),
                "source_type": "faq"
            }
            for result in results
            if result["metadata"].get("source_type") == "faq"
        ]
    
    # 获取所有文档分块知识项
    def get_all_documents(self) -> List[dict]:
        results = self.client.get_all()
        return [
            {
                "id": result["id"],
                "document_name": result["metadata"]["document_name"],
                "section": result["metadata"]["section"],
                "section_level": result["metadata"]["section_level"],
                "section_path": result["metadata"]["section_path"],
                "chunk_index": result["metadata"]["chunk_index"],
                "source_path": result["metadata"]["source_path"],
                "content": result["metadata"]["content"],
                "source_type": "document"
            }
            for result in results
            if result["metadata"].get("source_type") == "document"
        ]
    
    # 更新知识项
    def update(self, item: KnowledgeItem) -> None:
        combined_text = f"问题: {item.question}\n答案: {item.answer}"
        metadata = {
            "question": item.question,
            "answer": item.answer,
            "category": item.category,
            "source_type": "faq"       # 标识为FAQ类型
        }
        self.client.update(item.id, combined_text, metadata)
    
    # # 批量添加知识项
    # def batch_add(self, items: List[KnowledgeItem]) -> int:
    #     """批量添加知识项
        
    #     Args:
    #         items: KnowledgeItem 对象列表
        
    #     Returns:
    #         成功添加的数量
    #     """
    #     success_count = 0
    #     for item in items:
    #         try:
    #             self.add_item(item)
    #             success_count += 1
    #         except Exception as e:
    #             print(f"批量添加失败，ID: {item.id}, 错误: {e}")
    #     return success_count