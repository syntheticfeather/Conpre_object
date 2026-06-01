# smart-customer-service/utils/bm25_retriever.py
"""
BM25 关键词检索 + RRF 多路召回融合。

流程:
  向量检索 (语义)  ─┐
                    ├→ RRF 融合 → 最终 Top-K
  BM25 检索 (关键词) ─┘

BM25 擅长精确匹配（产品型号、专有名词），互补向量检索的语义匹配。
"""
import logging
from typing import List, Dict, Any, Optional
from rank_bm25 import BM25Okapi
import jieba

logger = logging.getLogger(__name__)


class BM25Retriever:
    """BM25 关键词检索引擎。对中文用 jieba 分词，英文按空格切分。"""

    def __init__(self):
        self._documents: List[Dict[str, Any]] = []  # [{id, document, metadata}, ...]
        self._bm25: Optional[BM25Okapi] = None
        self._tokenized: List[List[str]] = []

    # ========== 索引管理 ==========

    def build_index(self, documents: List[Dict[str, Any]]):
        """
        从文档列表构建 BM25 索引。
        documents: [{id, document, metadata}, ...]，来自 ChromaDB get_all()
        """
        if not documents:
            self._documents = []
            self._tokenized = []
            self._bm25 = None
            return

        self._documents = documents
        self._tokenized = [self._tokenize(d["document"]) for d in documents]
        self._bm25 = BM25Okapi(self._tokenized)
        logger.info(f"BM25 索引构建完成，共 {len(documents)} 条")

    def rebuild_from_chromadb(self, chromadb_client):
        """从 ChromaDB 全量重建索引（文档更新后调用）"""
        try:
            all_docs = chromadb_client.get_all()
            if all_docs:
                self.build_index(all_docs)
        except Exception as e:
            logger.error(f"BM25 索引重建失败: {e}")

    # ========== 检索 ==========

    def search(self, query: str, top_k: int = 5) -> List[Dict[str, Any]]:
        """BM25 关键词检索，返回 top_k 条结果"""
        if not self._bm25 or not self._tokenized:
            return []

        query_tokens = self._tokenize(query)
        if not query_tokens:
            return []

        scores = self._bm25.get_scores(query_tokens)

        # 按分数降序排列
        ranked = sorted(
            enumerate(scores), key=lambda x: x[1], reverse=True
        )

        results = []
        for idx, score in ranked:
            if score <= 0 or len(results) >= top_k:
                break
            doc = self._documents[idx]
            results.append({
                "id": doc.get("id", ""),
                "document": doc.get("document", ""),
                "metadata": doc.get("metadata", {}),
                "score": float(score),
                "source": "bm25",
            })
        return results

    # ========== 辅助 ==========

    def _tokenize(self, text: str) -> List[str]:
        """混合分词：中文用 jieba，英文/数字保持原样"""
        tokens = []
        # jieba 分词
        for word in jieba.cut(text):
            word = word.strip()
            if word and not self._is_stopword(word):
                tokens.append(word)
        return tokens

    def _is_stopword(self, word: str) -> bool:
        """简单停用词过滤"""
        if len(word) <= 1 and not word.isalnum():
            return True
        if word in {"的", "了", "吗", "呢", "吧", "啊", "是", "在", "和", "及"}:
            return True
        return False

    @property
    def is_ready(self) -> bool:
        return self._bm25 is not None and len(self._tokenized) > 0


# ========== RRF 融合算法 ==========

def rrf_fusion(
    vector_results: List[Dict],
    bm25_results: List[Dict],
    top_k: int = 5,
    k: int = 60,
) -> List[Dict]:
    """
    RRF (Reciprocal Rank Fusion) 融合两路检索结果。

    对每条结果，根据它在各路中的排名加权：
      RRF_score = Σ 1 / (k + rank_i)
    然后按总分数降序排列。

    k=60 是经典参数，控制排名靠前 vs 靠后的权重差异。
    """
    fused: Dict[str, Dict] = {}  # id → {doc info + cumulative score}

    for results in [vector_results, bm25_results]:
        for rank, item in enumerate(results, start=1):
            doc_id = item.get("id", "")
            if doc_id not in fused:
                fused[doc_id] = {**item, "rrf_score": 0.0}
            fused[doc_id]["rrf_score"] += 1.0 / (k + rank)

    # 按 RRF 分数降序
    ranked = sorted(fused.values(), key=lambda x: x["rrf_score"], reverse=True)
    return ranked[:top_k]
