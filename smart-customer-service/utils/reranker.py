# smart-customer-service/utils/reranker.py
"""
Rerank 精排：Cross-Encoder 对粗排候选重打分。

支持两种模式（通过 RERANK_MODE 环境变量切换）:
  - api:   调 SiliconFlow Reranker API（默认，推荐，无需本地模型）
  - local: 本地加载 Cross-Encoder 模型
"""
import logging
import os
from typing import List, Dict, Optional
from openai import OpenAI
from dotenv import load_dotenv

load_dotenv()

logger = logging.getLogger(__name__)


class Reranker:
    """精排器。默认使用 SiliconFlow Reranker API。"""

    _available = None

    def __init__(self):
        self.enabled = os.getenv("RERANK_ENABLED", "true").lower() == "true"
        self.top_n = int(os.getenv("RERANK_TOP_N", "3"))
        self.mode = os.getenv("RERANK_MODE", "api")

    @property
    def is_available(self) -> bool:
        if not self.enabled:
            return False
        if self._available is not None:
            return self._available

        if self.mode == "api":
            self._available = self._init_api()
        else:
            self._available = self._init_local()
        return self._available

    # ========== API 模式 ==========

    def _init_api(self) -> bool:
        key = os.getenv("SILICONFLOW_API_KEY")
        if not key:
            logger.warning("Reranker API: SILICONFLOW_API_KEY 未设置")
            return False
        self._api_key = key
        self._api_base = os.getenv("SILICONFLOW_API_BASE", "https://api.siliconflow.cn/v1")
        self._api_model = os.getenv("RERANK_API_MODEL", "BAAI/bge-reranker-v2-m3")
        logger.info(f"Reranker API 模式: {self._api_model}")
        return True

    def _rerank_api(self, query: str, candidates: List[Dict]) -> List[Dict]:
        """调用 SiliconFlow Reranker API"""
        try:
            client = OpenAI(api_key=self._api_key, base_url=self._api_base)

            documents = [c.get("document", c.get("content", "")) for c in candidates]

            response = client.post(
                "/rerank",
                body={
                    "model": self._api_model,
                    "query": query,
                    "documents": documents,
                    "top_n": min(self.top_n, len(documents)),
                },
            )
            results = response.json().get("results", [])

            logger.debug(
                f"Rerank API: {len(candidates)} → {len(results)} "
                f"(最高分: {results[0].get('relevance_score', 0):.4f})"
                if results else "Rerank API: 无结果"
            )

            # 按 API 返回的顺序重新排列
            ranked_ids = {r["index"]: r["relevance_score"] for r in results}
            reranked = sorted(
                [(i, c) for i, c in enumerate(candidates) if i in ranked_ids],
                key=lambda x: ranked_ids[x[0]],
                reverse=True,
            )
            return [c for _, c in reranked]
        except Exception as e:
            logger.warning(f"Reranker API 调用失败: {e}")
            return candidates[:self.top_n]

    # ========== 本地模式 ==========

    _model = None
    _model_name = None

    @classmethod
    def _init_local(cls) -> bool:
        model_name = os.getenv("RERANKER_MODEL", "BAAI/bge-reranker-base")
        try:
            from sentence_transformers import CrossEncoder
            logger.info(f"Reranker 本地模式: {model_name}")
            cls._model = CrossEncoder(model_name)
            cls._model_name = model_name
            return True
        except Exception as e:
            logger.warning(f"Reranker 本地模型加载失败: {e}")
            return False

    def _rerank_local(self, query: str, candidates: List[Dict]) -> List[Dict]:
        try:
            pairs = [
                [query, c.get("document", c.get("content", ""))]
                for c in candidates
            ]
            scores = self._model.predict(pairs)
            ranked = sorted(
                zip(candidates, scores), key=lambda x: x[1], reverse=True
            )
            return [item for item, _ in ranked[:self.top_n]]
        except Exception as e:
            logger.warning(f"Reranker 本地失败: {e}")
            return candidates[:self.top_n]

    # ========== 统一入口 ==========

    def rerank(self, query: str, candidates: List[Dict]) -> List[Dict]:
        if not self.is_available or not candidates:
            return candidates
        if len(candidates) <= self.top_n:
            return candidates

        if self.mode == "api":
            return self._rerank_api(query, candidates)
        return self._rerank_local(query, candidates)
