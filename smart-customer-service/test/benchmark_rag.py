"""
RAG 检索性能基准测试框架 (2026-06-09)

用法:
    cd smart-customer-service
    PYTHONIOENCODING=utf-8 python test/benchmark_rag.py

功能:
  - 消融实验 (Ablation Study)：逐步启用各 RAG 组件，量化每个组件的贡献
  - 计算指标：Hit Rate@k, MRR, NDCG@k, Recall@k, 延迟
  - 输出对比报告到 test/benchmark_results/

消融配置:
  A: 纯向量检索 (Baseline)
  B: 向量 + Query改写
  C: 向量 + BM25 + RRF
  D: 向量 + BM25 + RRF + Reranker
  E: 向量 + BM25 + RRF + Query改写 + Reranker (Full)
"""
import os, sys, json, time, shutil, tempfile, ssl, io, math
from collections import defaultdict
from dataclasses import dataclass, field
from typing import List, Dict, Optional, Any, Tuple

# Ensure project root is in path
_PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if _PROJECT_ROOT not in sys.path:
    sys.path.insert(0, _PROJECT_ROOT)

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8') if hasattr(sys.stdout, 'buffer') else sys.stdout

# Setup: local ChromaDB for testing
ssl._create_default_https_context = ssl._create_unverified_context
os.environ["CHROMADB_MODE"] = "local"

# Use HuggingFace mirror (direct access blocked)
os.environ["HF_ENDPOINT"] = "https://hf-mirror.com"

# Use multilingual model for Chinese support (384 dims)
os.environ["LOCAL_EMBEDDING_MODEL"] = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"

# Disable API embedding (balance insufficient) — use local model directly
os.environ["SILICONFLOW_API_KEY"] = ""  # Force local fallback

BENCH_DB_DIR = tempfile.mkdtemp(prefix="bench_rag_")
os.environ["CHROMADB_PERSIST_DIRECTORY"] = BENCH_DB_DIR

# Lazy imports — resolved at first use
_chromadb_client_mod = None
_vector_store_mod = None
_init_data_mod = None
_bm25_mod = None
_rewriter_mod = None
_reranker_mod = None

def _get_chromadb():
    global _chromadb_client_mod
    if _chromadb_client_mod is None:
        from utils.chromadb_client import ChromaDBClient
        _chromadb_client_mod = ChromaDBClient
    return _chromadb_client_mod

def _get_vector_store():
    global _vector_store_mod
    if _vector_store_mod is None:
        from knowledge.vector_store import VectorStore
        _vector_store_mod = VectorStore
    return _vector_store_mod

def _get_init_data():
    global _init_data_mod
    if _init_data_mod is None:
        from api.init_data import KnowledgeInitializer
        _init_data_mod = KnowledgeInitializer
    return _init_data_mod

def _get_bm25():
    global _bm25_mod
    if _bm25_mod is None:
        from utils.bm25_retriever import BM25Retriever, rrf_fusion
        _bm25_mod = (BM25Retriever, rrf_fusion)
    return _bm25_mod

def _get_rewriter():
    global _rewriter_mod
    if _rewriter_mod is None:
        from utils.query_rewriter import QueryRewriter
        _rewriter_mod = QueryRewriter
    return _rewriter_mod

def _get_reranker():
    global _reranker_mod
    if _reranker_mod is None:
        from utils.reranker import Reranker
        _reranker_mod = Reranker
    return _reranker_mod


# ═══════════════════════════════════════════════════════════
#  Custom Embedding Function (Chromadb 1.5.x compatible)
# ═══════════════════════════════════════════════════════════

class _BenchEmbeddingFunction:
    """
    ChromaDB 1.5.x compatible embedding function using local
    paraphrase-multilingual-MiniLM-L12-v2 (384-dim, supports Chinese).
    """
    _model = None

    def __init__(self):
        if _BenchEmbeddingFunction._model is None:
            from sentence_transformers import SentenceTransformer
            model_name = "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2"
            print(f"  加载本地嵌入模型: {model_name} ...")
            _BenchEmbeddingFunction._model = SentenceTransformer(model_name)
            print(f"  模型就绪 (dim={_BenchEmbeddingFunction._model.get_embedding_dimension()})")

    def name(self) -> str:
        return "paraphrase-multilingual-MiniLM-L12-v2"

    # ChromaDB 1.5.x interface: __call__ for batch
    def __call__(self, input: List[str]) -> List[List[float]]:
        """ChromaDB batch embedding: texts → list of vectors"""
        embeddings = self._model.encode(input, normalize_embeddings=True)
        return embeddings.tolist()

    # ChromaDB 1.5.x: embed_query must return List[List[float]]
    def embed_query(self, input=None, texts=None) -> List[List[float]]:
        """ChromaDB 1.x query embedding: returns list of vectors"""
        data = input if input is not None else texts
        if isinstance(data, str):
            data = [data]
        embeddings = self._model.encode(data, normalize_embeddings=True)
        return embeddings.tolist()  # List[List[float]], even for single query

    # ChromaDB 1.5.x: embed_documents for document batch
    def embed_documents(self, input=None, texts=None) -> List[List[float]]:
        """ChromaDB 1.x document batch embedding"""
        data = input if input is not None else texts
        if isinstance(data, str):
            data = [data]
        embeddings = self._model.encode(data, normalize_embeddings=True)
        return embeddings.tolist()

# ═══════════════════════════════════════════════════════════
#  Data Structures
# ═══════════════════════════════════════════════════════════

@dataclass
class BenchmarkQuery:
    query_id: str
    query: str
    difficulty: str           # easy / medium / hard
    query_type: str           # faq_exact / faq_paraphrase / document_* / ...
    relevant_conditions: Dict[str, Any]  # matching rules for auto-evaluation

@dataclass
class SearchResult:
    """Single search result item"""
    id: str
    document: str
    metadata: Dict[str, Any]
    score: float = 0.0

@dataclass
class QueryResult:
    """Benchmark result for one query"""
    query_id: str
    query: str
    difficulty: str
    results: List[SearchResult]
    relevant_hits: List[int]  # 0-indexed positions of relevant results
    latency_ms: float

@dataclass
class AblationReport:
    """Results for one ablation configuration"""
    name: str
    description: str
    query_results: List[QueryResult] = field(default_factory=list)
    metrics: Dict[str, float] = field(default_factory=dict)


# ═══════════════════════════════════════════════════════════
#  Metrics Calculation
# ═══════════════════════════════════════════════════════════

def _calc_dcg(relevances: List[int], k: int) -> float:
    """Discounted Cumulative Gain"""
    dcg = 0.0
    for i, rel in enumerate(relevances[:k]):
        dcg += rel / math.log2(i + 2)  # i+2 because log2(1)=0 is problematic
    return dcg

def _calc_idcg(relevances: List[int], k: int) -> float:
    """Ideal DCG (perfect ordering)"""
    ideal = sorted(relevances, reverse=True)
    return _calc_dcg(ideal, k)

def compute_metrics(reports: List[QueryResult], k_values: List[int] = None) -> Dict[str, float]:
    """Compute all evaluation metrics from query results."""
    if k_values is None:
        k_values = [1, 3, 5]

    metrics = {}
    n = len(reports)

    if n == 0:
        return {"total_queries": 0}

    metrics["total_queries"] = n

    # Hit Rate @ k
    for k in k_values:
        hits = sum(1 for r in reports if any(pos < k for pos in r.relevant_hits))
        metrics[f"hit_rate@{k}"] = hits / n if n > 0 else 0.0

    # MRR (Mean Reciprocal Rank)
    mrr_sum = 0.0
    for r in reports:
        if r.relevant_hits:
            mrr_sum += 1.0 / (r.relevant_hits[0] + 1)  # +1 because positions are 0-indexed
    metrics["mrr"] = mrr_sum / n if n > 0 else 0.0

    # Recall @ k (binary: 1 if at least one relevant doc in top-k)
    for k in k_values:
        recall_sum = 0.0
        for r in reports:
            has_relevant = any(pos < k for pos in r.relevant_hits)
            recall_sum += 1.0 if has_relevant else 0.0
        metrics[f"recall@{k}"] = recall_sum / n if n > 0 else 0.0

    # NDCG @ k (simplified: binary relevance)
    for k in k_values:
        ndcg_sum = 0.0
        for r in reports:
            # Binary relevance: 1 if hit, 0 otherwise
            rels = [1 if i in r.relevant_hits else 0 for i in range(k)]
            dcg = _calc_dcg(rels, k)
            idcg = _calc_idcg(rels, k)
            ndcg_sum += (dcg / idcg) if idcg > 0 else 0.0
        metrics[f"ndcg@{k}"] = ndcg_sum / n if n > 0 else 0.0

    # Latency stats
    latencies = [r.latency_ms for r in reports]
    latencies.sort()
    metrics["latency_avg_ms"] = sum(latencies) / n if n > 0 else 0.0
    metrics["latency_p50_ms"] = latencies[n // 2] if n > 0 else 0.0
    metrics["latency_p99_ms"] = latencies[min(int(n * 0.99), n - 1)] if n > 0 else 0.0
    metrics["latency_min_ms"] = min(latencies) if latencies else 0.0
    metrics["latency_max_ms"] = max(latencies) if latencies else 0.0

    # Difficulty breakdown
    for diff in ["easy", "medium", "hard"]:
        diff_reports = [r for r in reports if r.difficulty == diff]
        if diff_reports:
            hits_3 = sum(1 for r in diff_reports if any(pos < 3 for pos in r.relevant_hits))
            metrics[f"hit_rate@3_{diff}"] = hits_3 / len(diff_reports)

    return metrics


# ═══════════════════════════════════════════════════════════
#  Relevance Checker
# ═══════════════════════════════════════════════════════════

def check_relevance(result: SearchResult, conditions: Dict[str, Any]) -> bool:
    """Check if a search result matches the ground truth conditions."""
    metadata = result.metadata
    document = result.document.lower()

    # Check source_type
    if "source_type" in conditions:
        if metadata.get("source_type") != conditions["source_type"]:
            return False

    # Check document_name
    if "document_name" in conditions:
        if metadata.get("document_name", "").lower() != conditions["document_name"].lower():
            return False

    # Check category (for FAQ)
    if "category" in conditions:
        if metadata.get("category", "") != conditions["category"]:
            return False

    # Check must_contain keywords (ALL must be present)
    if "must_contain" in conditions:
        for keyword in conditions["must_contain"]:
            kw_lower = keyword.lower()
            # Search in both document text and metadata text
            found_in_doc = kw_lower in document
            found_in_meta = any(
                kw_lower in str(v).lower()
                for v in metadata.values()
                if isinstance(v, str)
            )
            if not (found_in_doc or found_in_meta):
                return False

    return True


def find_relevant_positions(results: List[SearchResult], conditions: Dict[str, Any]) -> List[int]:
    """Return 0-indexed positions of all relevant results."""
    positions = []
    for i, result in enumerate(results):
        if check_relevance(result, conditions):
            positions.append(i)
    return positions


# ═══════════════════════════════════════════════════════════
#  Ablation Configurations
# ═══════════════════════════════════════════════════════════

ABLATION_CONFIGS = {
    "A": {
        "name": "Baseline (Vector Only)",
        "description": "纯向量语义检索，无改写/BM25/Reranker",
        "enable_rewrite": False,
        "enable_bm25": False,
        "enable_rerank": False,
    },
    "B": {
        "name": "Vector + Query Rewrite",
        "description": "向量检索 + Query改写",
        "enable_rewrite": True,
        "enable_bm25": False,
        "enable_rerank": False,
    },
    "C": {
        "name": "Vector + BM25 + RRF",
        "description": "多路召回：向量 + BM25关键词 + RRF融合",
        "enable_rewrite": False,
        "enable_bm25": True,
        "enable_rerank": False,
    },
    "D": {
        "name": "Vector + BM25 + RRF + Reranker",
        "description": "多路召回 + RRF融合 + Cross-Encoder精排",
        "enable_rewrite": False,
        "enable_bm25": True,
        "enable_rerank": True,
    },
    "E": {
        "name": "Full Pipeline (All Components)",
        "description": "Query改写 + 多路召回 + RRF融合 + 精排 — 完整流水线",
        "enable_rewrite": True,
        "enable_bm25": True,
        "enable_rerank": True,
    },
}


# ═══════════════════════════════════════════════════════════
#  RAG Benchmark Runner
# ═══════════════════════════════════════════════════════════

class RAGBenchmark:
    """RAG 检索基准测试框架"""

    def __init__(self, top_k: int = 5):
        self.top_k = top_k
        self.queries: List[BenchmarkQuery] = []
        self.reports: Dict[str, AblationReport] = {}

        # Lazy-initialized components
        self._vector_store = None
        self._bm25 = None
        self._rewriter = None
        self._reranker = None
        self._chromadb_client = None

    # ── Initialization ──

    def load_queries(self, queries_path: str = None):
        """Load benchmark queries from JSON file."""
        if queries_path is None:
            queries_path = os.path.join(os.path.dirname(__file__), "benchmark_queries.json")

        with open(queries_path, "r", encoding="utf-8") as f:
            data = json.load(f)

        self.queries = [
            BenchmarkQuery(
                query_id=q["query_id"],
                query=q["query"],
                difficulty=q["difficulty"],
                query_type=q.get("type", q.get("query_type", "")),
                relevant_conditions=q["relevant_conditions"],
            )
            for q in data["queries"]
        ]
        print(f"[Benchmark] 加载 {len(self.queries)} 条测试查询")
        for diff in ["easy", "medium", "hard"]:
            count = sum(1 for q in self.queries if q.difficulty == diff)
            print(f"  {diff}: {count} 条")

    def init_knowledge_base(self, data_dir: str = None):
        """Initialize ChromaDB with all benchmark data using a custom
        embedding function that works with ChromaDB 1.5.x."""
        if data_dir is None:
            data_dir = os.path.join(os.path.dirname(__file__), "..", "knowledge-init")

        print(f"\n[Benchmark] 初始化知识库...")

        # Create a fresh ChromaDB client with our custom embedding function
        import chromadb
        from chromadb.config import Settings

        self._embedding_fn = _BenchEmbeddingFunction()

        self._chroma_client_raw = chromadb.PersistentClient(
            path=BENCH_DB_DIR,
            settings=Settings(anonymized_telemetry=False, allow_reset=True),
        )

        # Reset any existing collection
        try:
            self._chroma_client_raw.delete_collection("benchmark_kb")
        except Exception:
            pass

        self._collection = self._chroma_client_raw.create_collection(
            name="benchmark_kb",
            embedding_function=self._embedding_fn,
            metadata={"description": "RAG benchmark knowledge base"},
        )

        # --- Now load data and add directly to our collection ---
        from api.init_data import KnowledgeInitializer

        initializer = KnowledgeInitializer()
        total_added = 0

        # Helper to add items to our custom collection
        def add_to_collection(items, source_type):
            nonlocal total_added
            if not items:
                return 0
            batch_ids, batch_docs, batch_metas = [], [], []
            for item in items:
                if source_type == "faq":
                    doc_id = item.get("id", f"faq_{len(batch_ids)}")
                    question = item.get("question", "")
                    answer = item.get("answer", "")
                    batch_ids.append(doc_id)
                    batch_docs.append(f"问题: {question}\n答案: {answer}")
                    batch_metas.append({
                        "question": question, "answer": answer,
                        "category": item.get("category", "通用"),
                        "source_type": "faq",
                    })
                else:  # document chunk
                    chunk = item
                    batch_ids.append(chunk.chunk_id)
                    batch_docs.append(chunk.combined)
                    batch_metas.append({
                        "document_name": chunk.document_name,
                        "section": chunk.section,
                        "section_level": chunk.section_level,
                        "section_path": chunk.section_path,
                        "chunk_index": chunk.chunk_index,
                        "source_path": chunk.source_path,
                        "source_type": "document",
                        "content": chunk.content,
                    })
            if batch_ids:
                self._collection.add(
                    ids=batch_ids, documents=batch_docs, metadatas=batch_metas
                )
                total_added += len(batch_ids)
            return len(batch_ids)

        # 1. Load FAQ pairs
        faq_json = os.path.join(data_dir, "benchmark_faq.json")
        _, qa_pairs = initializer._load_json(faq_json)

        # Generate IDs for FAQ items
        for i, qa in enumerate(qa_pairs):
            qa["id"] = f"bench_faq_{i:03d}"
        add_to_collection(qa_pairs, "faq")
        print(f"  FAQ 基准数据: 添加 {len(qa_pairs)} 条")

        # 2. Load markdown documents
        docs_dir = os.path.join(data_dir, "benchmark_docs")
        doc_chunks = initializer._load_directory(docs_dir)
        add_to_collection(doc_chunks, "document")
        print(f"  文档基准数据: 添加 {len(doc_chunks)} 个分块")

        total = self._collection.count()
        print(f"  知识库总条目: {total}")

        # Store for later use
        self._vector_store = None  # Will be lazily created
        self._chromadb_client = None  # Not using ChromaDBClient wrapper

        return total

    # ── Component Lazy Init ──

    @property
    def vector_store(self):
        """Return self — we query the collection directly."""
        return self

    def _vector_search(self, query: str, top_k: int = 10) -> List[Dict]:
        """Search our custom ChromaDB collection directly, returning formatted dicts."""
        try:
            results = self._collection.query(
                query_texts=[query],
                n_results=top_k,
            )
            if not results or not results.get("ids") or not results["ids"][0]:
                return []

            items = []
            for i in range(len(results["ids"][0])):
                meta = results["metadatas"][0][i] if results.get("metadatas") else {}
                doc = results["documents"][0][i] if results.get("documents") else ""
                source_type = meta.get("source_type", "faq")

                if source_type == "faq":
                    items.append({
                        "id": results["ids"][0][i],
                        "type": "faq",
                        "question": meta.get("question", ""),
                        "answer": meta.get("answer", ""),
                        "category": meta.get("category", "通用"),
                        "source_type": "faq",
                    })
                else:
                    items.append({
                        "id": results["ids"][0][i],
                        "type": "document",
                        "document_name": meta.get("document_name", ""),
                        "section": meta.get("section", ""),
                        "section_path": meta.get("section_path", ""),
                        "content": meta.get("content", ""),
                        "source_type": "document",
                    })
            return items
        except Exception as e:
            print(f"  [WARN] Vector search failed: {e}")
            return []

    @property
    def bm25(self):
        if self._bm25 is None:
            BM25Retriever, _ = _get_bm25()
            self._bm25 = BM25Retriever()
            # Build index from our raw collection
            self._rebuild_bm25()
        return self._bm25

    def _rebuild_bm25(self):
        """Build BM25 index from the raw ChromaDB collection."""
        if self._bm25 is None or self._collection is None:
            return
        raw = self._collection.get()
        if not raw or not raw.get("ids"):
            return
        docs = [
            {"id": raw["ids"][i], "document": raw["documents"][i],
             "metadata": raw["metadatas"][i]}
            for i in range(len(raw["ids"]))
        ]
        self._bm25.build_index(docs)

    @property
    def rewriter(self):
        if self._rewriter is None:
            QueryRewriter = _get_rewriter()
            self._rewriter = QueryRewriter()
        return self._rewriter

    @property
    def reranker(self):
        if self._reranker is None:
            Reranker = _get_reranker()
            self._reranker = Reranker()
        return self._reranker

    # ── Core Search Logic (configurable) ──

    def search_with_config(
        self,
        query: str,
        config: Dict[str, bool],
        top_k: int = None,
    ) -> Tuple[List[SearchResult], float]:
        """
        Execute a search with the given component toggles.
        Returns (results, latency_ms).
        """
        if top_k is None:
            top_k = self.top_k

        t_start = time.perf_counter()

        # Step 1: Query rewrite (if enabled)
        search_query = query
        if config.get("enable_rewrite", False):
            try:
                rewritten = self.rewriter.rewrite(query)
                if rewritten and rewritten != query:
                    search_query = rewritten
            except Exception:
                pass  # Fall through with original query

        # Step 2: Vector search (always)
        vector_raw = self._vector_search(search_query, top_k=top_k * 3)
        vector_for_rrf = _vector_results_to_search_results(vector_raw, source="vector")

        # Step 3: BM25 search (if enabled)
        bm25_for_rrf = []
        if config.get("enable_bm25", False):
            try:
                # Rebuild index if needed
                if not self.bm25.is_ready:
                    self.bm25.rebuild_from_chromadb(self._chromadb_client)
                bm25_raw = self.bm25.search(search_query, top_k=top_k * 3)
                bm25_for_rrf = _bm25_results_to_search_results(bm25_raw, source="bm25")
            except Exception:
                pass

        # Step 4: RRF fusion (if BM25 enabled and has results)
        fused = vector_for_rrf  # default: just vector results
        if config.get("enable_bm25", False) and bm25_for_rrf:
            _, rrf_fusion = _get_bm25()
            rrf_raw = rrf_fusion(
                _to_rrf_format(vector_for_rrf),
                _to_rrf_format(bm25_for_rrf),
                top_k=top_k * 2,
            )
            fused = _from_rrf_format(rrf_raw)

        # Step 5: Reranker (if enabled)
        final = fused
        if config.get("enable_rerank", False) and len(fused) > 1:
            try:
                if self.reranker.is_available:
                    candidates = [
                        {"id": r.id, "document": r.document, "metadata": r.metadata}
                        for r in fused
                    ]
                    reranked = self.reranker.rerank(search_query, candidates)
                    final = [
                        SearchResult(
                            id=r.get("id", ""),
                            document=r.get("document", ""),
                            metadata=r.get("metadata", {}),
                            score=r.get("score", r.get("relevance_score", 0.0)),
                        )
                        for r in reranked[:top_k]
                    ]
            except Exception:
                final = fused

        # Limit to top_k
        results = final[:top_k]

        t_end = time.perf_counter()
        latency_ms = (t_end - t_start) * 1000

        return results, latency_ms

    # ── Run Benchmark ──

    def run_ablation(self, config_key: str) -> AblationReport:
        """Run all queries under one ablation configuration."""
        config = ABLATION_CONFIGS[config_key]
        report = AblationReport(
            name=config["name"],
            description=config["description"],
        )

        print(f"\n{'─' * 60}")
        print(f"  [{config_key}] {config['name']}")
        print(f"  {config['description']}")
        print(f"{'─' * 60}")

        for bq in self.queries:
            results, latency = self.search_with_config(
                bq.query,
                {"enable_rewrite": config["enable_rewrite"],
                 "enable_bm25": config["enable_bm25"],
                 "enable_rerank": config["enable_rerank"]},
                top_k=self.top_k,
            )

            # Find relevant positions
            positions = find_relevant_positions(results, bq.relevant_conditions)

            qr = QueryResult(
                query_id=bq.query_id,
                query=bq.query,
                difficulty=bq.difficulty,
                results=results,
                relevant_hits=positions,
                latency_ms=latency,
            )
            report.query_results.append(qr)

            # Print per-query status
            status = "✓" if positions else "✗"
            pos_str = f"pos={positions[0]+1}" if positions else "miss"
            print(f"  {status} {bq.query_id} [{bq.difficulty}] "
                  f"\"{bq.query[:40]}...\" → {pos_str} ({latency:.1f}ms)")

        # Compute metrics
        report.metrics = compute_metrics(report.query_results)
        self.reports[config_key] = report

        # Print summary for this config
        self._print_ablation_summary(report)

        return report

    def run_all(self):
        """Run all ablation configurations."""
        print("\n" + "=" * 60)
        print("  RAG 检索性能基准测试 — 消融实验")
        print("=" * 60)
        print(f"  总查询数: {len(self.queries)}")
        print(f"  Top-K: {self.top_k}")
        print(f"  消融配置数: {len(ABLATION_CONFIGS)}")

        for key in ABLATION_CONFIGS:
            self.run_ablation(key)

        self._print_final_report()

    # ── Reporting ──

    def _print_ablation_summary(self, report: AblationReport):
        """Print a one-line summary for an ablation config."""
        m = report.metrics
        print(f"  ── 小结: HR@3={m.get('hit_rate@3', 0):.2%}  "
              f"MRR={m.get('mrr', 0):.3f}  "
              f"NDCG@3={m.get('ndcg@3', 0):.3f}  "
              f"AvgLat={m.get('latency_avg_ms', 0):.0f}ms")

    def _print_final_report(self):
        """Print the final comparison report."""
        print("\n\n")
        print("=" * 80)
        print("  📊 最终对比报告 — RAG 消融实验结果")
        print("=" * 80)

        # ── Table 1: Core Metrics ──
        headers = ["Config", "HR@1", "HR@3", "HR@5", "MRR", "NDCG@3", "NDCG@5",
                   "Recall@3", "AvgLat(ms)", "P99(ms)"]
        col_widths = [30, 8, 8, 8, 8, 8, 8, 10, 12, 10]

        # Header
        header_line = ""
        for h, w in zip(headers, col_widths):
            header_line += f"{h:>{w}}  "
        print(header_line)
        print("-" * len(header_line))

        baseline_hr3 = 0.0
        rows = []
        for key in ["A", "B", "C", "D", "E"]:
            if key not in self.reports:
                continue
            r = self.reports[key]
            m = r.metrics
            if key == "A":
                baseline_hr3 = m.get("hit_rate@3", 0)

            row_data = [
                f"{key}: {r.name[:20]}" if len(r.name) > 20 else f"{key}: {r.name}",
                f"{m.get('hit_rate@1', 0):.3f}",
                f"{m.get('hit_rate@3', 0):.3f}",
                f"{m.get('hit_rate@5', 0):.3f}",
                f"{m.get('mrr', 0):.3f}",
                f"{m.get('ndcg@3', 0):.3f}",
                f"{m.get('ndcg@5', 0):.3f}",
                f"{m.get('recall@3', 0):.3f}",
                f"{m.get('latency_avg_ms', 0):.0f}",
                f"{m.get('latency_p99_ms', 0):.0f}",
            ]
            rows.append((key, row_data))

        for key, row_data in rows:
            line = ""
            for d, w in zip(row_data, col_widths):
                line += f"{d:>{w}}  "
            print(line)

        # ── Improvement percentages ──
        print(f"\n{'─' * 80}")
        print("  📈 相对 Baseline 提升幅度")
        print(f"{'─' * 80}")
        if baseline_hr3 > 0:
            for key in ["B", "C", "D", "E"]:
                if key not in self.reports:
                    continue
                r = self.reports[key]
                hr3 = r.metrics.get("hit_rate@3", 0)
                improvement = (hr3 - baseline_hr3) / baseline_hr3 * 100
                bar = "█" * int(abs(improvement) / 2) if improvement > 0 else ""
                print(f"  {key} ({r.name}):  "
                      f"HR@3: {baseline_hr3:.3f} → {hr3:.3f}  "
                      f"Δ={improvement:+.1f}%  {bar}")
        else:
            print("  (Baseline HR@3 = 0, 无法计算相对提升)")

        # ── Difficulty Breakdown ──
        print(f"\n{'─' * 80}")
        print("  🎯 按难度分类 — Hit Rate@3")
        print(f"{'─' * 80}")
        diff_headers = ["Config"] + ["easy", "medium", "hard"]
        diff_widths = [30, 12, 12, 12]
        diff_line = ""
        for h, w in zip(diff_headers, diff_widths):
            diff_line += f"{h:>{w}}  "
        print(diff_line)
        print("-" * len(diff_line))

        for key in ["A", "B", "C", "D", "E"]:
            if key not in self.reports:
                continue
            r = self.reports[key]
            m = r.metrics
            diff_data = [
                f"{key}: {r.name[:20]}",
                f"{m.get('hit_rate@3_easy', 0):.3f}",
                f"{m.get('hit_rate@3_medium', 0):.3f}",
                f"{m.get('hit_rate@3_hard', 0):.3f}",
            ]
            line = ""
            for d, w in zip(diff_data, diff_widths):
                line += f"{d:>{w}}  "
            print(line)

        print(f"\n{'=' * 80}")
        print("  测试完成！")
        print(f"{'=' * 80}")

    def save_results(self, output_dir: str = None):
        """Save detailed results to JSON."""
        if output_dir is None:
            output_dir = os.path.join(os.path.dirname(__file__), "benchmark_results")
        os.makedirs(output_dir, exist_ok=True)

        # Serialize all reports
        output = {
            "config": {
                "top_k": self.top_k,
                "total_queries": len(self.queries),
            },
            "ablations": {},
        }

        for key, report in self.reports.items():
            ablation_data = {
                "name": report.name,
                "description": report.description,
                "metrics": report.metrics,
                "query_details": [],
            }
            for qr in report.query_results:
                ablation_data["query_details"].append({
                    "query_id": qr.query_id,
                    "query": qr.query,
                    "difficulty": qr.difficulty,
                    "relevant_positions": qr.relevant_hits,
                    "hit": len(qr.relevant_hits) > 0,
                    "first_rank": qr.relevant_hits[0] + 1 if qr.relevant_hits else None,
                    "latency_ms": round(qr.latency_ms, 2),
                    "top3_ids": [r.id for r in qr.results[:3]],
                })
            output["ablations"][key] = ablation_data

        output_path = os.path.join(output_dir, "benchmark_report.json")
        with open(output_path, "w", encoding="utf-8") as f:
            json.dump(output, f, ensure_ascii=False, indent=2)
        print(f"\n[Benchmark] 详细结果已保存至: {output_path}")

        return output_path


# ═══════════════════════════════════════════════════════════
#  Conversion Helpers
# ═══════════════════════════════════════════════════════════

def _vector_results_to_search_results(vector_raw: List[Dict], source: str = "vector") -> List[SearchResult]:
    """Convert VectorStore output to SearchResult list."""
    results = []
    for item in vector_raw:
        doc_text = ""
        if item.get("type") == "faq":
            doc_text = f"{item.get('question', '')} {item.get('answer', '')}"
        elif item.get("type") == "document":
            doc_text = item.get("content", "")
        else:
            doc_text = item.get("document", "")

        results.append(SearchResult(
            id=item.get("id", ""),
            document=doc_text,
            metadata={
                "source_type": item.get("type", ""),
                "question": item.get("question", ""),
                "answer": item.get("answer", ""),
                "category": item.get("category", ""),
                "document_name": item.get("document_name", ""),
                "section_path": item.get("section_path", ""),
            },
        ))
    return results


def _bm25_results_to_search_results(bm25_raw: List[Dict], source: str = "bm25") -> List[SearchResult]:
    """Convert BM25 output to SearchResult list."""
    results = []
    for item in bm25_raw:
        results.append(SearchResult(
            id=item.get("id", ""),
            document=item.get("document", ""),
            metadata=item.get("metadata", {}),
            score=item.get("score", 0.0),
        ))
    return results


def _to_rrf_format(results: List[SearchResult]) -> List[Dict]:
    """Convert SearchResult to RRF-compatible dict format."""
    return [
        {
            "id": r.id,
            "document": r.document,
            "metadata": r.metadata,
            "source": "vector" if "bm25" not in str(r.score) else "bm25",
        }
        for r in results
    ]


def _from_rrf_format(rrf_results: List[Dict]) -> List[SearchResult]:
    """Convert RRF output back to SearchResult list."""
    results = []
    for item in rrf_results:
        results.append(SearchResult(
            id=item.get("id", ""),
            document=item.get("document", ""),
            metadata=item.get("metadata", {}),
            score=item.get("rrf_score", 0.0),
        ))
    return results


# ═══════════════════════════════════════════════════════════
#  Main Entry Point
# ═══════════════════════════════════════════════════════════

def main():
    """Run the full RAG benchmark."""
    print("=" * 60)
    print("  Smart Customer Service — RAG Benchmark Suite")
    print("=" * 60)

    benchmark = RAGBenchmark(top_k=5)

    # Step 1: Load test queries
    benchmark.load_queries()

    # Step 2: Initialize knowledge base with benchmark data
    count = benchmark.init_knowledge_base()
    if count == 0:
        print("\n[ERROR] 知识库为空，请检查数据文件！")
        sys.exit(1)

    # Step 3: Run all ablation experiments
    benchmark.run_all()

    # Step 4: Save detailed results
    benchmark.save_results()

    # Cleanup
    try:
        shutil.rmtree(BENCH_DB_DIR, ignore_errors=True)
    except Exception:
        pass

    print("\nDone.")


if __name__ == "__main__":
    main()
