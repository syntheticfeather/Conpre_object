# smart-customer-service/utils/chromadb_client.py
import chromadb
from chromadb.config import Settings
from typing import List, Optional, Dict, Any
import os
import hashlib
import logging
from dotenv import load_dotenv

# 加载环境变量
load_dotenv()

# 创建日志记录器
logger = logging.getLogger(__name__)

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
                logger.info(f"使用硅基流动嵌入API，模型: {self.model_name}")
            except ImportError:
                logger.warning("openai 库未安装，使用简单嵌入函数")
                self.available = False
        else:
            logger.warning("SILICONFLOW_API_KEY 未设置，使用简单嵌入函数")
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
            logger.error(f"调用嵌入API失败: {e}")
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
            
            # 配置嵌入函数（符合ChromaDB接口）
            embedding_function = OpenAICompatibleEmbeddingFunction()
            
            if chromadb_mode == 'remote':
                self.client = chromadb.HttpClient(
                    host=chromadb_host,
                    port=chromadb_port,
                    settings=Settings(
                        anonymized_telemetry=False,  # 禁用 telemetry
                        allow_reset=True             # 允许重置数据库，清空数据（默认是False）
                    ),
                    tenant='default_tenant',       # 租户
                    database='default_database'    # 数据库
                )
            else:
                self.client = chromadb.PersistentClient(
                    path=persist_directory,
                    settings=Settings(
                        anonymized_telemetry=False,  # 禁用 telemetry
                        allow_reset=True             # 允许重置数据库，清空数据（默认是False）
                    )
                )
            
            self.collection = self.client.get_or_create_collection(
                name="smart_cs_knowledge_base",
                metadata={"description": "智能客服知识库"},
                embedding_function=embedding_function
            )
            
            # 检查当前集合状态
            count = self.collection.count()
            logger.info(f"[ChromaDB] 初始化成功 - 当前集合条目数: {count}")
            
        except Exception as e:
            logger.error(f"[ChromaDB] 初始化失败: {e}")
            self.client = None
            self.collection = None
    
    def add_item(self, item_id: str, document: str, metadata: Dict[str, Any]) -> bool:
        try:
            if not self.collection:
                logger.error("ChromaDB未初始化")
                return False
            
            logger.debug(f"[ChromaDB] 添加数据 - ID: {item_id}")
            
            # 添加数据
            self.collection.add(
                documents=[document],
                metadatas=[metadata],
                ids=[item_id]
            )
            
            # 检查是否添加成功
            count = self.collection.count()
            logger.debug(f"[ChromaDB] 添加后集合总条目数: {count}")

            # 再次确认数据
            count_after = self.collection.count()
            logger.debug(f"[ChromaDB] 持久化后集合总条目数: {count_after}")
            
            return True
        except Exception as e:
            logger.error(f"[ChromaDB] 添加数据失败: {e}")
            return False
    
    def search(self, query: str, top_k: int = 3) -> List[Dict[str, Any]]:
        try:
            if not self.collection:
                logger.error("ChromaDB未初始化")
                return []
            
            results = self.collection.query(
                query_texts=[query],
                n_results=top_k,
                where={"source_type": {"$in": ["document", "faq"]}},
                where_document={"$contains": " "}  # 匹配所有文档
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
            logger.error(f"[ChromaDB] 搜索失败: {e}")
            return []
    
    def delete(self, item_id: str) -> bool:
        try:
            if not self.collection:
                logger.error("ChromaDB未初始化")
                return False
            self.collection.delete(ids=[item_id])
            return True
        except Exception as e:
            logger.error(f"[ChromaDB] 删除数据失败: {e}")
            return False
    
    def delete_by_metadata(self, where: Dict[str, Any]) -> int:
        """按元数据条件删除数据
        
        Args:
            where: 元数据条件字典，如 {"document_name": "法律条例与合规声明"}
            
        Returns:
            成功删除的数量
        """
        try:
            # 转换为 ChromaDB 支持的 where 语法
            chromadb_where = {}
            for key, value in where.items():
                # 使用 $eq 操作符进行精确匹配
                chromadb_where[key] = {"$eq": value}
            logger.debug(f"[ChromaDB] 按元数据删除 - 查询条件: {chromadb_where}")
            
            # 获取符合条件的所有数据
            results = self.collection.get(where=chromadb_where)
            if not results or not results["ids"]:
                logger.debug(f"[ChromaDB] 按元数据删除 - 未找到匹配数据")
                return 0
            
            ids_to_delete = results["ids"]
            logger.debug(f"[ChromaDB] 按元数据删除 - 找到 {len(ids_to_delete)} 条匹配数据")
            
            # 删除数据
            self.collection.delete(ids=ids_to_delete)
            count = self.collection.count()
            logger.info(f"[ChromaDB] 按元数据删除完成 - 删除 {len(ids_to_delete)} 条，当前集合总条目数: {count}")
            
            return len(ids_to_delete)
        except Exception as e:
            logger.error(f"[ChromaDB] 按元数据删除失败: {e}")
            return 0
    
    def get_all(self) -> List[Dict[str, Any]]:
        try:
            if not self.collection:
                logger.error("ChromaDB未初始化")
                return []
            
            # 调试信息
            count = self.collection.count()
            logger.debug(f"[ChromaDB] get_all - 当前集合总条目数: {count}")
            
            results = self.collection.get()
            
            # 打印原始结果
            logger.debug(f"[ChromaDB] get_all 原始结果: {results}")
            
            if not results["ids"]:
                logger.debug("[ChromaDB] get_all - 集合为空")
                return []
            
            logger.debug(f"[ChromaDB] get_all - 成功获取 {len(results['ids'])} 条数据")
            
            return [
                {
                    "id": results["ids"][i],
                    "document": results["documents"][i],
                    "metadata": results["metadatas"][i]
                }
                for i in range(len(results["ids"]))
            ]
        except Exception as e:
            logger.error(f"[ChromaDB] 获取所有数据失败: {e}")
            return []
    
    def update(self, item_id: str, document: str, metadata: Dict[str, Any]) -> bool:
        try:
            if not self.collection:
                logger.error("ChromaDB未初始化")
                return False
            self.collection.update(
                documents=[document],
                metadatas=[metadata],
                ids=[item_id]
            )
            return True
        except Exception as e:
            logger.error(f"[ChromaDB] 更新数据失败: {e}")
            return False
    
    def batch_add(self, items: List[Dict[str, Any]]) -> int:
        """批量添加数据到向量库
        
        Args:
            items: 数据列表，每个元素包含 id, document, metadata
            
        Returns:
            成功添加的数量
        """
        try:
            if not self.collection:
                logger.error("ChromaDB未初始化")
                return 0
            
            if not items:
                logger.debug("[ChromaDB] 批量添加 - 空数据列表")
                return 0
            
            # 分离数据
            ids = [item["id"] for item in items]
            documents = [item["document"] for item in items]
            metadatas = [item["metadata"] for item in items]
            
            logger.info(f"[ChromaDB] 批量添加数据 - 数量: {len(items)}")
            
            # 批量添加
            self.collection.add(
                documents=documents,
                metadatas=metadatas,
                ids=ids
            )
            
            # 检查添加后数量
            count = self.collection.count()
            logger.info(f"[ChromaDB] 批量添加完成 - 当前集合总条目数: {count}")
            
            return len(items)
        except Exception as e:
            logger.error(f"[ChromaDB] 批量添加失败: {e}")
            return 0

# 创建全局实例
chromadb_client = ChromaDBClient()