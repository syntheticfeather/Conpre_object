# smart-customer-service/memory/user_memory.py
"""
用户长期记忆 — ChromaDB 独立 collection。

v2 改造:
  - Mem0 式多信号检索: 向量 + 实体匹配 → 融合排序
  - Zep 式时序感知: 时间衰减 + 冲突自动过期 + 有效期管理
"""
import logging, math
from typing import List, Dict, Optional
from datetime import datetime, timedelta

logger = logging.getLogger(__name__)

# 时间衰减半衰期（天）
DECAY_HALF_LIFE = 90  # 3个月后半衰


class UserMemoryStore:
    COLLECTION_NAME = "user_memory"

    def __init__(self):
        self._collection = None
        self._entity_collection = None  # Mem0 式实体索引

    @property
    def collection(self):
        if self._collection is None:
            from utils.chromadb_client import chromadb_client
            client = chromadb_client.client
            if client:
                emb = chromadb_client.collection._embedding_function
                self._collection = client.get_or_create_collection(
                    name=self.COLLECTION_NAME,
                    metadata={"description": "用户长期记忆"},
                    embedding_function=emb,
                )
        return self._collection

    @property
    def entity_collection(self):
        """Mem0 式实体索引 — 不存向量，只用 metadata 过滤"""
        if self._entity_collection is None:
            from utils.chromadb_client import chromadb_client
            client = chromadb_client.client
            if client:
                emb = chromadb_client.collection._embedding_function
                self._entity_collection = client.get_or_create_collection(
                    name=f"{self.COLLECTION_NAME}_entities",
                    metadata={"description": "记忆实体索引"},
                    embedding_function=emb,
                )
        return self._entity_collection

    # ========== 写入（Mem0 实体提取 + Zep 冲突检测） ==========

    def save(self, user_id: str, content: str, memory_type: str = "preference",
             key: str = None, importance: float = 0.5):
        try:
            if not self.collection:
                return False

            now = datetime.now()
            entities = self._extract_entities(content)

            meta = {
                "user_id": str(user_id),
                "type": memory_type,
                "key": key or "",
                "entities": ",".join(entities),
                "importance": importance,
                "created_at": now.isoformat(),
                "valid_until": (now + timedelta(days=365)).isoformat(),  # 默认1年有效期
            }

            # Zep 式冲突检测：同一 user + key 的旧记忆自动标为过期
            if key:
                self._expire_conflicts(user_id, key, now)

            doc_id = f"{user_id}_{hash(content) % 10**8}"
            self.collection.upsert(
                documents=[content], metadatas=[meta], ids=[doc_id],
            )

            # Mem0 式实体索引
            if entities and self.entity_collection:
                for e in entities:
                    eid = f"e_{user_id}_{e}"
                    self.entity_collection.upsert(
                        documents=[e], metadatas=[{"user_id": str(user_id), "entity": e}], ids=[eid],
                    )
            return True
        except Exception as e:
            logger.warning(f"保存记忆失败: {e}")
            return False

    def _extract_entities(self, content: str) -> List[str]:
        """简单实体提取：从文本中抽关键词（后续可升级为 LLM 提取）"""
        keywords = []
        for kw in ["等额本息", "等额本金", "先息后本", "一次性还本付息",
                    "消费贷", "经营贷", "个人消费贷", "企业经营贷",
                    "低利率", "短期", "长期", "月供"]:
            if kw in content:
                keywords.append(kw)
        return keywords

    def _expire_conflicts(self, user_id: str, key: str, now: datetime):
        """Zep 式冲突解决：同一 key 的旧记忆标记为已过期"""
        try:
            existing = self.collection.get(where={
                "user_id": str(user_id), "key": key,
            })
            for i, eid in enumerate(existing["ids"]):
                meta = existing["metadatas"][i]
                if meta.get("valid_until", "") >= now.isoformat():
                    meta["valid_until"] = now.isoformat()  # 过期
                    self.collection.update(ids=[eid], metadatas=[meta])
        except Exception:
            pass

    # ========== 检索（Mem0 多信号 + Zep 时间衰减） ==========

    def search(self, user_id: str, query: str, top_k: int = 5,
               memory_type: str = None) -> List[Dict]:
        """
        多信号检索 + 时序衰减。

        分数 = 语义距离倒排 × 重要性 × 时间衰减(e^(-λt))
        """
        try:
            if not self.collection:
                return []

            # 信号1: 向量检索（主通道，多拉一些候选后续再排序）
            where = {"user_id": str(user_id)}
            if memory_type:
                where["type"] = memory_type

            results = self.collection.query(
                query_texts=[query],
                n_results=top_k * 2,  # 多拉候选
                where=where,
            )

            now = datetime.now()
            scored = []
            for i in range(len(results["ids"][0])):
                meta = results["metadatas"][0][i]
                distance = results["distances"][0][i] if "distances" in results else 1.0

                # 检查是否过期（Zep 式）
                valid_until = meta.get("valid_until", "")
                if valid_until and valid_until < now.isoformat():
                    continue  # 已过期，跳过

                # 综合评分
                vector_score = 1.0 - min(distance, 1.0)          # 距离→相似度
                importance = float(meta.get("importance", 0.5))
                time_decay = self._calc_decay(meta.get("created_at", ""), now)

                # 信号2: 实体匹配（Mem0 式）
                entity_bonus = self._entity_match(query, meta.get("entities", ""))

                final = (
                    vector_score * 0.50
                    + importance * 0.20
                    + time_decay * 0.15
                    + entity_bonus * 0.15
                )
    
                scored.append((final, {
                    "document": results["documents"][0][i],
                    "metadata": meta,
                    "score": round(final, 4),
                }))

            scored.sort(key=lambda x: x[0], reverse=True)
            return [item for _, item in scored[:top_k]]

        except Exception as e:
            logger.warning(f"记忆搜索失败: {e}")
            return []

    def _calc_decay(self, created_at: str, now: datetime) -> float:
        """Zep 式时间衰减: e^(-λt)，半衰期 DECAY_HALF_LIFE 天"""
        if not created_at:
            return 0.5
        try:
            created = datetime.fromisoformat(created_at)
            days = (now - created).total_seconds() / 86400
            lam = math.log(2) / DECAY_HALF_LIFE
            return math.exp(-lam * days)
        except Exception:
            return 0.5

    def _entity_match(self, query: str, entities_str: str) -> float:
        """Mem0 式实体匹配: query 中的实体命中记忆的实体 → 加权"""
        if not entities_str or not query:
            return 0.0
        mem_entities = set(entities_str.split(","))
        query_entities = set()
        for e in self._extract_entities(query):
            query_entities.add(e)
        if not query_entities or not mem_entities:
            return 0.0
        overlap = len(query_entities & mem_entities)
        return min(1.0, overlap / max(len(query_entities), 1))

    # ========== 去重（文本重叠，不需要 LLM） ==========
    # NOTE: 当前未使用，O(n²) 开销较高，暂不接入实时路径；
    #       后续可配合定时 consolidate 触发。

    def dedup(self, user_id: str, memories: List[Dict]) -> int:
        """去重：语义相似的记忆合并"""
        count = 0
        merged = set()
        for i, m1 in enumerate(memories):
            if i in merged:
                continue
            for j, m2 in enumerate(memories):
                if j <= i or j in merged:
                    continue
                # 用向量距离判断（需从 ChromaDB 查，这里用简单的文本重叠）
                overlap = self._text_overlap(m1["document"], m2["document"])
                if overlap > 0.7:
                    # 合并：保留较长的那条
                    keep = m1 if len(m1["document"]) >= len(m2["document"]) else m2
                    merged.add(j)
                    # 标记被合并的为过期
                    doc_id = f"{user_id}_{hash(memories[j]['document']) % 10**8}"
                    try:
                        meta = m2["metadata"]
                        meta["valid_until"] = datetime.now().isoformat()
                        self.collection.update(ids=[doc_id], metadatas=[meta])
                        count += 1
                    except Exception:
                        pass
        return count

    def _text_overlap(self, a: str, b: str) -> float:
        """简单文本重叠率"""
        if not a or not b:
            return 0.0
        words_a = set(a)
        words_b = set(b)
        if not words_a or not words_b:
            return 0.0
        return len(words_a & words_b) / min(len(words_a), len(words_b))

    # ========== 管理 ==========

    def get_all(self, user_id: str) -> List[Dict]:
        try:
            if not self.collection:
                return []
            results = self.collection.get(where={"user_id": str(user_id)})
            return [
                {"document": results["documents"][i], "metadata": results["metadatas"][i]}
                for i in range(len(results["ids"]))
            ]
        except Exception as e:
            logger.warning(f"获取全部记忆失败: {e}")
            return []

    def delete(self, user_id: str, key: str = None):
        try:
            if not self.collection:
                return
            where = {"user_id": str(user_id)}
            if key:
                where["key"] = key
            existing = self.collection.get(where=where)
            if existing["ids"]:
                self.collection.delete(ids=existing["ids"])
        except Exception as e:
            logger.warning(f"删除记忆失败: {e}")

    def stats(self, user_id: str) -> Dict:
        """查看用户记忆统计"""
        try:
            all_mems = self.get_all(user_id)
            now = datetime.now()
            active = sum(1 for m in all_mems
                        if m["metadata"].get("valid_until", "") >= now.isoformat())
            expired = len(all_mems) - active
            return {"total": len(all_mems), "active": active, "expired": expired}
        except Exception:
            return {"total": 0, "active": 0, "expired": 0}
