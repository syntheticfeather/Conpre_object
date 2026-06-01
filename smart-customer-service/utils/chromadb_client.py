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

class LocalEmbeddingFunction:
    """
    本地 sentence-transformers 嵌入函数，作为 API 不可用时的兜底方案。
    首次加载模型后会缓存，后续调用不再加载。
    默认模型: all-MiniLM-L6-v2 (英文), 可通过环境变量指定中文模型。

    target_dim: 目标维度，用于补齐到和主模型一致的维度。
                例如本地模型384维 + target_dim=1024 → 后面640维填0。
                None 则不补齐，使用模型原生维度。
    """

    _model = None
    _model_name = None

    def __init__(self, target_dim: int = None):
        self.target_dim = target_dim
        self._name = os.getenv('LOCAL_EMBEDDING_MODEL', 'local-model')

    def name(self) -> str:
        return self._name

    @classmethod
    def _get_model(cls):
        """延迟加载模型，只加载一次"""
        model_name = os.getenv(
            'LOCAL_EMBEDDING_MODEL',
            'sentence-transformers/all-MiniLM-L6-v2'
        )
        if cls._model is None or cls._model_name != model_name:
            from sentence_transformers import SentenceTransformer
            logger.info(f"加载本地嵌入模型: {model_name}")
            cls._model = SentenceTransformer(model_name)
            cls._model_name = model_name
        return cls._model

    def __call__(self, input: List[str]) -> List[List[float]]:
        """ChromaDB 兼容: __call__(self, input)"""
        return self._encode(input)

    def embed_query(self, input=None, texts=None) -> List[List[float]]:
        """ChromaDB 0.5+ — 返回 List[List[float]] 兼容 Rust 后端"""
        data = input if input is not None else texts
        if isinstance(data, str):
            data = [data]
        if not isinstance(data, list):
            data = [data]
        return self._encode(data)

    def embed_documents(self, input=None, texts=None) -> List[List[float]]:
        """ChromaDB 0.5+ - input 是字符串列表"""
        data = input if input is not None else texts
        if isinstance(data, str):
            data = [data]
        if not isinstance(data, list):
            data = [data]
        return self._encode(data)

    def _encode(self, input: List[str]) -> List[List[float]]:
        try:
            model = self._get_model()
            embeddings = model.encode(input, normalize_embeddings=True)
            result = embeddings.tolist()

            # 补齐到目标维度
            if self.target_dim:
                for i, vec in enumerate(result):
                    if len(vec) < self.target_dim:
                        result[i] = vec + [0.0] * (self.target_dim - len(vec))

            return result
        except Exception as e:
            logger.error(f"本地嵌入模型调用失败: {e}，回退到 MD5 哈希")
            return _md5_fallback(input)


def _md5_fallback(input: List[str]) -> List[List[float]]:
    """最终兜底：MD5哈希生成128维向量。语义检索基本无效，仅保系统不崩溃。"""
    results = []
    for text in input:
        hash_val = hashlib.md5(text.encode('utf-8')).digest()
        embedding = [float(h) / 255.0 for h in hash_val]  # 归一化到 [0, 1]
        results.append(embedding)
    return results

class CachedEmbeddingFunction:
    """
    Embedding 缓存包装器：对相同文本复用已计算的向量，避免重复调 API。
    支持两种模式：
      - exact: 文本精确匹配（默认，零精度损失）
      - semantic: 语义相似匹配（用余弦相似度找"意思相近"的已缓存文本，

    成本节省估算：重复查询/FAQ 文本命中率通常 30-50%。
    """

    def __init__(self, inner_func, mode: str = "exact", similarity_threshold: float = 0.95):
        self._inner = inner_func
        self._cache: Dict[str, List[float]] = {}
        self._mode = mode
        self._sim_threshold = similarity_threshold
        self._hits = 0
        self._misses = 0
        self._name = getattr(inner_func, '_name', 'cached-embedding')

    def name(self) -> str:
        return self._name

    def __call__(self, input: List[str]) -> List[List[float]]:
        return self._encode(input)

    def embed_query(self, input=None, texts=None) -> List[float]:
        data = input if input is not None else texts
        if isinstance(data, str): data = [data]
        return self._encode(data)[0]

    def embed_documents(self, input=None, texts=None) -> List[List[float]]:
        data = input if input is not None else texts
        if isinstance(data, str): data = [data]
        return self._encode(data)

    def _encode(self, input: List[str]) -> List[List[float]]:
        result = [None] * len(input)
        uncached_texts = []
        uncached_indices = []

        for i, text in enumerate(input):
            if self._mode == "exact":
                key = hashlib.md5(text.encode('utf-8')).hexdigest()
                if key in self._cache:
                    result[i] = self._cache[key]
                    self._hits += 1
                else:
                    uncached_texts.append(text)
                    uncached_indices.append(i)
            else:
                found = self._semantic_lookup(text)
                if found is not None:
                    result[i] = found
                    self._hits += 1
                else:
                    uncached_texts.append(text)
                    uncached_indices.append(i)

        if uncached_texts:
            # 优先用 embed_documents 避免 LocalEmbeddingFunction 递归错误
            if hasattr(self._inner, 'embed_documents'):
                new_vectors = self._inner.embed_documents(uncached_texts)
            else:
                new_vectors = self._inner(uncached_texts)
            self._misses += len(uncached_texts)
            for j, idx in enumerate(uncached_indices):
                vec = new_vectors[j]
                result[idx] = vec
                key = hashlib.md5(uncached_texts[j].encode('utf-8')).hexdigest()
                self._cache[key] = vec

        return result

    def _semantic_lookup(self, text: str) -> Optional[List[float]]:
        """语义模式：在缓存中找余弦相似度 > threshold 的文本。这是一个轻量 ANN"""
        if not self._cache:
            return None
        # 先计算新文本的向量（用内部函数）
        new_vec = self._inner([text])[0]
        # 在缓存中遍历找最相似的（缓存通常不大，暴力遍历即可）
        best_sim = 0
        best_vec = None
        for cached_vec in self._cache.values():
            sim = _cosine_similarity(new_vec, cached_vec)
            if sim > best_sim:
                best_sim = sim
                best_vec = cached_vec
        if best_sim >= self._sim_threshold:
            return best_vec
        return None

    @property
    def stats(self) -> Dict[str, int]:
        return {"hits": self._hits, "misses": self._misses, "total": self._hits + self._misses}


def _cosine_similarity(a: List[float], b: List[float]) -> float:
    """计算两个向量的余弦相似度"""
    dot = sum(x * y for x, y in zip(a, b))
    norm_a = sum(x ** 2 for x in a) ** 0.5
    norm_b = sum(y ** 2 for y in b) ** 0.5
    if norm_a == 0 or norm_b == 0:
        return 0.0
    return dot / (norm_a * norm_b)


class OpenAICompatibleEmbeddingFunction:
    """使用原生openai库的嵌入函数，符合ChromaDB接口"""

    # 已知模型的默认维度(BAAI/bge-m3=1024, text-embedding-3-small=1536等)
    _MODEL_DIMS = {
        'BAAI/bge-m3': 1024,
        'BAAI/bge-large-zh-v1.5': 1024,
        'text-embedding-3-small': 1536,
        'text-embedding-3-large': 3072,
    }

    def __init__(self):
        self.api_key = os.getenv('SILICONFLOW_API_KEY')
        self.api_base = os.getenv('SILICONFLOW_API_BASE', 'https://api.siliconflow.cn/v1')
        self.model_name = os.getenv('SILICONFLOW_EMBEDDING_MODEL', 'BAAI/bge-m3')
        self.client = None
        self.available = False
        self._initialize()

    def name(self) -> str:
        return self.model_name

    @property
    def model_dimension(self) -> int:
        """主模型的目标维度，兜底方案需要补齐到这个维度"""
        env_dim = os.getenv('EMBEDDING_DIMENSION')
        if env_dim:
            return int(env_dim)
        return self._MODEL_DIMS.get(self.model_name, 1024)  # 未知模型默认1024

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
                logger.warning("openai 库未安装，使用本地嵌入模型")
                self.available = False
        else:
            logger.warning("SILICONFLOW_API_KEY 未设置，使用本地嵌入模型")
            self.available = False
    
    def __call__(self, input: List[str]) -> List[List[float]]:
        """ChromaDB 兼容: __call__(self, input)"""
        return self._encode(input)

    def embed_query(self, input=None, texts=None) -> List[List[float]]:
        """ChromaDB 0.5+ — 返回 List[List[float]] 兼容 Rust 后端"""
        data = input if input is not None else texts
        if isinstance(data, str):
            data = [data]
        if not isinstance(data, list):
            data = [data]
        return self._encode(data)

    def embed_documents(self, input=None, texts=None) -> List[List[float]]:
        """ChromaDB 0.5+ - input 是字符串列表"""
        data = input if input is not None else texts
        if isinstance(data, str):
            data = [data]
        if not isinstance(data, list):
            data = [data]
        return self._encode(data)

    def _encode(self, input: List[str]) -> List[List[float]]:
        if not self.available or not self.client:
            return LocalEmbeddingFunction(target_dim=self.model_dimension)._encode(input)

        try:
            response = self.client.embeddings.create(
                input=input,
                model=self.model_name
            )
            return [item.embedding for item in response.data]
        except Exception as e:
            logger.error(f"调用嵌入API失败: {e}")
            return LocalEmbeddingFunction(target_dim=self.model_dimension)._encode(input)

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
            
            # 配置嵌入函数（符合ChromaDB接口）+ 缓存
            raw_embedding = OpenAICompatibleEmbeddingFunction()
            cache_mode = os.getenv('EMBEDDING_CACHE_MODE', 'exact')
            embedding_function = CachedEmbeddingFunction(raw_embedding, mode=cache_mode)
            
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