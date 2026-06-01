# smart-customer-service/tools/search_tools.py
from langchain_core.tools import tool
from tavily import TavilyClient
import os
from tools.tool_manager import tool_manager
from knowledge.vector_store import VectorStore
from utils.query_rewriter import QueryRewriter
from utils.bm25_retriever import BM25Retriever, rrf_fusion
from utils.reranker import Reranker

# 初始化 Tavily 客户端
tavily_client = TavilyClient(api_key=os.getenv("TAVILY_API_KEY", ""))

# 初始化查询改写器
query_rewriter = QueryRewriter()

# 初始化 BM25 检索引擎 + Reranker（全局单例）
bm25_retriever = BM25Retriever()
reranker = Reranker()
_bm25_initialized = False


def _ensure_bm25_index():
    """确保 BM25 索引已构建（延迟初始化）"""
    global _bm25_initialized
    if not _bm25_initialized:
        try:
            from utils.chromadb_client import chromadb_client
            bm25_retriever.rebuild_from_chromadb(chromadb_client)
            _bm25_initialized = True
        except Exception as e:
            print(f"[BM25] 索引初始化失败: {e}")


def rebuild_bm25_index():
    """手动重建 BM25 索引（知识库更新后调用）"""
    global _bm25_initialized
    _bm25_initialized = False
    _ensure_bm25_index()


@tool
async def search_web(query: str, max_results: int = 3) -> str:
    """使用 Tavily 搜索引擎搜索网络信息。当需要获取最新的外部信息时使用。"""
    try:
        print(f"【开始调用 Tavily 搜索】: {query}")
        result = tavily_client.search(
            query=query, max_results=max_results, search_depth="advanced"
        )
        response = f"搜索结果（{len(result['results'])}条）：\n\n"
        for i, item in enumerate(result['results'], 1):
            response += f"{i}. {item['title']}\n"
            response += f"   链接：{item['url']}\n"
            response += f"   摘要：{item['content'][:150]}...\n\n"
        return response
    except Exception as e:
        return f"抱歉，搜索失败：{str(e)}"


@tool
async def search_knowledge(query: str, top_k: int = 2) -> str:
    """在知识库中搜索相关信息。当用户询问常见问题、政策规则、产品信息时使用。"""
    try:
        # 1. Query 改写
        search_query = query_rewriter.rewrite(query)
        if search_query != query:
            print(f"【Query改写】: '{query[:50]}...' → '{search_query[:50]}...'")
        else:
            print(f"【开始调用知识库搜索】: {query}")

        vector_store = VectorStore()

        # 2. 多路召回：向量检索 + BM25 关键词检索
        _ensure_bm25_index()

        vector_raw = vector_store.search(search_query, top_k=top_k * 3)
        bm25_raw = bm25_retriever.search(search_query, top_k=top_k * 3) \
            if bm25_retriever.is_ready else []

        # 3. RRF 融合
        if bm25_raw:
            fused = rrf_fusion(
                _to_raw_format(vector_raw), bm25_raw, top_k=top_k * 3
            )
            # 4. Rerank 精排：Cross-Encoder 对融合结果重打分
            reranked = reranker.rerank(search_query, fused)
            results = _format_fused_results(reranked)
        else:
            results = vector_raw[:top_k]

        # 4. 回退：改写查询无结果 → 用原始查询
        if not results and search_query != query:
            print(f"【改写查询无结果，回退原始查询】: {query[:50]}...")
            results = vector_store.search(query, top_k=top_k)

        if not results:
            return "知识库中未找到相关信息"

        response = f"知识库搜索结果（{len(results)}条）：\n\n"
        for i, item in enumerate(results, 1):
            if item.get("type") == "faq":
                response += f"{i}. 问题：{item.get('question', '')}\n"
                response += f"回答：{item.get('answer', '')}\n"
                response += f"分类：{item.get('category', '通用')}\n\n"
            elif item.get("type") == "document":
                response += f"{i}.文档：{item.get('document_name', '')}\n"
                response += f"章节：{item.get('section_path', '')}\n"
                response += f"内容：{item.get('content', '')[:200]}...\n\n"
            else:
                response += f"{i}. 内容：{item.get('document', '')[:200]}...\n\n"

        return response
    except Exception as e:
        return f"抱歉，知识库搜索失败：{str(e)}"


def _to_raw_format(vector_results: list) -> list:
    """将 VectorStore 业务格式转为 RRF 兼容格式"""
    raw = []
    for item in vector_results:
        raw.append({
            "id": item.get("id", ""),
            "document": (
                f"{item.get('question', '')} {item.get('answer', '')}"
                if item.get("type") == "faq"
                else item.get("content", "")
            ),
            "metadata": {
                "source_type": item.get("type", ""),
                "question": item.get("question", ""),
                "answer": item.get("answer", ""),
                "category": item.get("category", ""),
                "document_name": item.get("document_name", ""),
                "section_path": item.get("section_path", ""),
            },
            "source": "vector",
        })
    return raw


def _format_fused_results(fused: list) -> list:
    """将 RRF 融合结果转为 search_knowledge 输出格式"""
    results = []
    for item in fused:
        meta = item.get("metadata", {})
        source_type = meta.get("source_type", "")

        if source_type == "faq":
            results.append({
                "id": item.get("id", ""),
                "type": "faq",
                "question": meta.get("question", ""),
                "answer": meta.get("answer", ""),
                "category": meta.get("category", "通用"),
            })
        else:
            results.append({
                "id": item.get("id", ""),
                "type": "document",
                "document_name": meta.get("document_name", ""),
                "section_path": meta.get("section_path", ""),
                "content": item.get("document", "")[:500],
            })
    return results


# 注册工具
tool_manager.register_tool(search_web)
tool_manager.register_tool(search_knowledge)
