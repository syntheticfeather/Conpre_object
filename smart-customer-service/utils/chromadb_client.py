# smart-customer-service/utils/chromadb_client.py
import chromadb
from chromadb.config import Settings
from typing import List, Optional, Dict, Any
import os
import hashlib
from dotenv import load_dotenv

# 加载环境变量
load_dotenv()

class SimpleEmbeddingFunction:
    """简单的嵌入函数作为fallback，符合ChromaDB接口"""
    
    def __call__(self, input: List[str]) -> List[List[float]]:
        """ChromaDB期望的接口签名: __call__(self, input)"""
        results = []
        for text in input:
            # 使用MD5哈希生成固定长度的嵌入向量（128维）
            hash_val = hashlib.md5(text.encode('utf-8')).digest()
            embedding = [int(h) for h in hash_val]
            results.append(embedding)
        return results

class OpenAICompatibleEmbeddingFunction:
    """使用原生openai库的嵌入函数，符合ChromaDB接口"""
    
    def __init__(self):
        self.api_key = os.getenv('SILICONFLOW_API_KEY')
        self.api_base = os.getenv('SILICONFLOW_API_BASE', 'https://api.siliconflow.cn/v1')
        self.model_name = os.getenv('SILICONFLOW_EMBEDDING_MODEL', 'BAAI/bge-m3')
        self.client = None
        self.available = False
        self._initialize()
    
    def _initialize(self):
        """初始化OpenAI客户端"""
        if self.api_key:
            try:
                from openai import OpenAI
                self.client = OpenAI(
                    api_key=self.api_key,
                    base_url=self.api_base
                )
                self.available = True
                print(f"使用硅基流动嵌入API，模型: {self.model_name}")
            except ImportError:
                print("Warning: openai 库未安装，使用简单嵌入函数")
                self.available = False
        else:
            print("Warning: SILICONFLOW_API_KEY 未设置，使用简单嵌入函数")
            self.available = False
    
    def __call__(self, input: List[str]) -> List[List[float]]:
        """ChromaDB期望的接口签名: __call__(self, input)"""
        if not self.available or not self.client:
            return SimpleEmbeddingFunction()(input)
        
        try:
            response = self.client.embeddings.create(
                input=input,
                model=self.model_name
            )
            return [item.embedding for item in response.data]
        except Exception as e:
            print(f"Warning: 调用嵌入API失败: {e}")
            return SimpleEmbeddingFunction()(input)

class ChromaDBClient:
    _instance = None
    
    def __new__(cls):
        if cls._instance is None:
            cls._instance = super(ChromaDBClient, cls).__new__(cls)
            cls._instance._initialize()
        return cls._instance
    
    def _initialize(self):
        try:
            # 禁用 ChromaDB telemetry
            os.environ['CHROMA_TELEMETRY_ENABLED'] = 'false'
            
            chromadb_mode = os.getenv('CHROMADB_MODE', 'local')
            chromadb_host = os.getenv('CHROMADB_HOST', 'chromadb')
            chromadb_port = int(os.getenv('CHROMADB_PORT', '8000'))
            persist_directory = os.getenv('CHROMADB_PERSIST_DIRECTORY', './chromadb_data')
            
            print(f"[ChromaDB] 初始化 - 模式: {chromadb_mode}, 持久化目录: {persist_directory}")
            
            # 配置嵌入函数（符合ChromaDB接口）
            embedding_function = OpenAICompatibleEmbeddingFunction()
            
            if chromadb_mode == 'remote':
                self.client = chromadb.HttpClient(
                    host=chromadb_host,
                    port=chromadb_port,
                    settings=Settings(
                        anonymized_telemetry=False,
                        allow_reset=True
                    ),
                    tenant='default_tenant',
                    database='default_database'
                )
            else:
                self.client = chromadb.PersistentClient(
                    path=persist_directory,
                    settings=Settings(
                        anonymized_telemetry=False,
                        allow_reset=True
                    )
                )
            
            self.collection = self.client.get_or_create_collection(
                name="smart_cs_knowledge_base",
                metadata={"description": "智能客服知识库"},
                embedding_function=embedding_function
            )
            
            # 检查当前集合状态
            count = self.collection.count()
            print(f"[ChromaDB] 初始化成功 - 当前集合条目数: {count}")
            
        except Exception as e:
            print(f"[ChromaDB] 初始化失败: {e}")
            self.client = None
            self.collection = None
    
    def add_item(self, item_id: str, document: str, metadata: Dict[str, Any]) -> bool:
        try:
            if not self.collection:
                print("Error: ChromaDB未初始化")
                return False
            
            print(f"[ChromaDB] 添加数据 - ID: {item_id}")
            
            # 添加数据
            self.collection.add(
                documents=[document],
                metadatas=[metadata],
                ids=[item_id]
            )
            
            # 检查是否添加成功
            count = self.collection.count()
            print(f"[ChromaDB] 添加后集合总条目数: {count}")

            # 再次确认数据
            count_after = self.collection.count()
            print(f"[ChromaDB] 持久化后集合总条目数: {count_after}")
            
            return True
        except Exception as e:
            print(f"[ChromaDB] Error adding item: {e}")
            return False
    
    def search(self, query: str, top_k: int = 3) -> List[Dict[str, Any]]:
        try:
            if not self.collection:
                print("Error: ChromaDB未初始化")
                return []
            
            results = self.collection.query(
                query_texts=[query],
                n_results=top_k
            )
            
            if not results or not results.get("ids") or not results["ids"][0]:
                return []
            
            return [
                {
                    "id": results["ids"][0][i],
                    "document": results["documents"][0][i],
                    "metadata": results["metadatas"][0][i],
                    "distance": results["distances"][0][i] if "distances" in results else None
                }
                for i in range(len(results["ids"][0]))
            ]
        except Exception as e:
            print(f"Error searching: {e}")
            return []
    
    def delete(self, item_id: str) -> bool:
        try:
            if not self.collection:
                print("Error: ChromaDB未初始化")
                return False
            self.collection.delete(ids=[item_id])
            return True
        except Exception as e:
            print(f"Error deleting item: {e}")
            return False
    
    def get_all(self) -> List[Dict[str, Any]]:
        try:
            if not self.collection:
                print("Error: ChromaDB未初始化")
                return []
            
            # 调试信息
            count = self.collection.count()
            print(f"get_all - 当前集合总条目数: {count}")
            
            results = self.collection.get()
            
            # 打印原始结果
            print(f"get_all 原始结果: {results}")
            
            if not results["ids"]:
                print("get_all - 集合为空")
                return []
            
            print(f"get_all - 成功获取 {len(results['ids'])} 条数据")
            
            return [
                {
                    "id": results["ids"][i],
                    "document": results["documents"][i],
                    "metadata": results["metadatas"][i]
                }
                for i in range(len(results["ids"]))
            ]
        except Exception as e:
            print(f"Error getting all items: {e}")
            return []
    
    def update(self, item_id: str, document: str, metadata: Dict[str, Any]) -> bool:
        try:
            if not self.collection:
                print("Error: ChromaDB未初始化")
                return False
            self.collection.update(
                documents=[document],
                metadatas=[metadata],
                ids=[item_id]
            )
            return True
        except Exception as e:
            print(f"Error updating item: {e}")
            return False
    
    def get_collection(self, collection_name: str = "knowledge_base"):
        try:
            if not self.client:
                print("Error: ChromaDB未初始化")
                return None
            return self.client.get_or_create_collection(
                name=collection_name,
                metadata={"description": f"{collection_name}集合"}
            )
        except Exception as e:
            print(f"Error getting collection: {e}")
            return None

# 创建全局实例
chromadb_client = ChromaDBClient()