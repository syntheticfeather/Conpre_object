# smart-customer-service/utils/chromadb_client.py
import chromadb
from chromadb.config import Settings
from typing import List, Optional, Dict, Any
import os
from dotenv import load_dotenv
from sentence_transformers import SentenceTransformer

# 加载环境变量
load_dotenv()

class ChineseEmbeddingFunction:
    def __init__(self):
        try:
            # 尝试加载支持中文的多语言模型
            self.model = SentenceTransformer('sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2', device='cpu')
            self.available = True
        except Exception as e:
            print(f"Warning: Failed to load embedding model: {e}")
            print("Using simple embedding function as fallback")
            self.available = False
    
    def __call__(self, input):
        # 确保输入是列表
        if isinstance(input, str):
            input = [input]
        
        # 如果模型加载失败，使用简单的字符数嵌入作为 fallback
        if not self.available:
            return [[len(text)] for text in input]
        
        # 生成embedding
        embeddings = self.model.encode(input)
        return embeddings.tolist()

class ChromaDBClient:
    _instance = None
    
    def __new__(cls):
        if cls._instance is None:
            cls._instance = super(ChromaDBClient, cls).__new__(cls)
            cls._instance._initialize()
        return cls._instance
    
    def _initialize(self):
        # 从环境变量读取配置
        chromadb_mode = os.getenv('CHROMADB_MODE', 'local')
        chromadb_host = os.getenv('CHROMADB_HOST', 'chromadb')
        chromadb_port = int(os.getenv('CHROMADB_PORT', '8000'))
        persist_directory = os.getenv('CHROMADB_PERSIST_DIRECTORY', './chromadb_data')
        
        # 检查是否使用远程服务器模式
        if chromadb_mode == 'remote':
            # 远程服务器模式
            self.client = chromadb.HttpClient(
                host=chromadb_host,
                port=chromadb_port,
                settings=Settings(
                    anonymized_telemetry=False
                ),
                tenant='default_tenant',
                database='default_database'
            )
            # 远程模式下，embedding由服务器处理
            embedding_function = None
        else:
            # 本地文件模式
            self.client = chromadb.PersistentClient(
                path=persist_directory,
                settings=Settings(
                    anonymized_telemetry=False
                )
            )
            # 本地模式下，使用自定义的中文embedding function
            embedding_function = ChineseEmbeddingFunction()
        
        # 获取或创建默认集合
        self.collection = self.client.get_or_create_collection(
            name="knowledge_base",
            metadata={"description": "智能客服知识库"},
            embedding_function=embedding_function
        )
    
    def add_item(self, item_id: str, document: str, metadata: Dict[str, Any]) -> None:
        """添加项目到向量库"""
        self.collection.add(
            documents=[document],
            metadatas=[metadata],
            ids=[item_id]
        )
    
    def search(self, query: str, top_k: int = 3) -> List[Dict[str, Any]]:
        """搜索相似项目"""
        results = self.collection.query(
            query_texts=[query],
            n_results=top_k
        )
        
        return [
            {
                "id": results["ids"][0][i],
                "document": results["documents"][0][i],
                "metadata": results["metadatas"][0][i],
                "distance": results["distances"][0][i] if "distances" in results else None
            }
            for i in range(len(results["ids"][0]))
        ]
    
    def delete(self, item_id: str) -> None:
        """删除项目"""
        self.collection.delete(ids=[item_id])
    
    def get_all(self) -> List[Dict[str, Any]]:
        """获取所有项目"""
        results = self.collection.get()
        
        if not results["ids"]:
            return []
        
        return [
            {
                "id": results["ids"][i],
                "document": results["documents"][i],
                "metadata": results["metadatas"][i]
            }
            for i in range(len(results["ids"]))
        ]
    
    def update(self, item_id: str, document: str, metadata: Dict[str, Any]) -> None:
        """更新项目"""
        self.collection.update(
            documents=[document],
            metadatas=[metadata],
            ids=[item_id]
        )
    
    def get_collection(self, collection_name: str = "knowledge_base"):
        """获取指定集合"""
        return self.client.get_or_create_collection(
            name=collection_name,
            metadata={"description": f"{collection_name}集合"}
        )

# 创建全局实例
chromadb_client = ChromaDBClient()
