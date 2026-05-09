# smart-customer-service/tools/search_tools.py
from langchain_core.tools import tool
from tavily import TavilyClient
import os
from tools.tool_manager import tool_manager
from knowledge.vector_store import VectorStore

# 初始化 Tavily 客户端
tavily_client = TavilyClient(api_key=os.getenv("TAVILY_API_KEY", "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"))

@tool
async def search_web(query: str, max_results: int = 3) -> str:
    """使用 Tavily 搜索引擎搜索网络信息。当需要获取最新的外部信息时使用。"""
    try:
        print(f"【开始调用 Tavily 搜索】: {query}")
        
        # 调用 Tavily API 进行搜索
        result = tavily_client.search(
            query=query,
            max_results=max_results,
            search_depth="advanced"
        )
        
        # 格式化搜索结果
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
        print(f"【开始调用知识库搜索】: {query}")
        
        vector_store = VectorStore()
        results = vector_store.search(query, top_k=top_k)
        
        if not results:
            return "知识库中未找到相关信息"
        
        response = f"知识库搜索结果（{len(results)}条）：\n\n"

        for i, item in enumerate(results, 1):
            if item.get("type") == "faq":
                # FAQ 格式
                response += f"{i}. 问题：{item.get('question', '')}\n"
                response += f"回答：{item.get('answer', '')}\n"
                response += f"分类：{item.get('category', '通用')}\n\n"
            
            elif item.get("type") == "document":
                # 文档格式
                response += f"{i}.文档：{item.get('document_name', '')}\n"
                response += f"章节：{item.get('section_path', '')}\n"
                response += f"内容：{item.get('content', '')[:200]}...\n\n"
            
            else:
                # 其他格式
                response += f"{i}. 内容：{item.get('document', '')[:200]}...\n\n"
        
        return response
    except Exception as e:
        return f"抱歉，知识库搜索失败：{str(e)}"

# 注册工具
tool_manager.register_tool(search_web)
tool_manager.register_tool(search_knowledge)