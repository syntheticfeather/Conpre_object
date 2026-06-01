# smart-customer-service/utils/query_rewriter.py
"""
Query 预处理：改写口语化问题，使检索更精准。

策略：
  - 简单改写: 补全代词、去除语气词、转成检索友好的独立问句
  - HyDE: 生成一个"假设答案"，用答案去检索（适用：问题和答案用词差异大的场景）
  - 可选: 对话上下文感知（从 chat_history 补全"上次说的那个"）
"""
from typing import List, Optional, Dict
import logging
from openai import OpenAI
import os

logger = logging.getLogger(__name__)


class QueryRewriter:
    """用轻量 LLM 改写用户查询"""

    def __init__(self):
        self.api_key = os.getenv("LLM_API_KEY")
        self.base_url = os.getenv("LLM_BASE_URL", "https://api.deepseek.com/v1")
        self.model = os.getenv("QUERY_REWRITE_MODEL", "deepseek-chat")
        self.enabled = os.getenv("QUERY_REWRITE_ENABLED", "true").lower() == "true"

    def rewrite(self, query: str, chat_history: List[Dict] = None) -> str:
        """
        将口语化问题改写成独立的、适合向量检索的查询文本。
        例如: "上次说的那个利率呢" → 结合历史 → "个人消费贷的年利率是多少"
        """
        if not self.enabled:
            return query

        # 简单问题不需要改写
        if self._is_simple(query):
            return query

        history_text = self._format_history(chat_history) if chat_history else "无"

        prompt = f"""你是一个查询改写助手。将用户的口语化问题改写成更适合向量检索的独立查询文本。

规则:
1. 补全代词和指代: "那个"、"上次说的" → 明确表述
2. 去除语气词: "呢"、"嘛"、"吧"
3. 保留核心语义，不添加新信息
4. 只输出改写后的查询，不要任何解释

对话历史:
{history_text}

用户问题: {query}

改写后的查询:"""

        try:
            client = OpenAI(api_key=self.api_key, base_url=self.base_url)
            response = client.chat.completions.create(
                model=self.model,
                messages=[{"role": "user", "content": prompt}],
                temperature=0.0,  # 确定性的，不要创造性
                max_tokens=200,
            )
            rewritten = response.choices[0].message.content.strip()
            if rewritten and len(rewritten) > 2:
                logger.debug(f"Query 改写: '{query[:50]}...' → '{rewritten[:80]}...'")
                return rewritten
            return query
        except Exception as e:
            logger.warning(f"Query 改写失败，使用原始查询: {e}")
            return query

    def hyde_rewrite(self, query: str) -> Optional[str]:
        """
        HyDE (Hypothetical Document Embeddings):
        让 LLM 先"假设"一个答案，用答案去检索。
        适合问题和答案用词差异大的场景。
        """
        if not self.enabled:
            return None

        prompt = f"""根据用户问题，写一段简短的假设性答案（100字以内）。
这段答案会被用来做向量检索，所以请使用知识库中可能出现的术语和表述。

用户问题: {query}

假设答案:"""

        try:
            client = OpenAI(api_key=self.api_key, base_url=self.base_url)
            response = client.chat.completions.create(
                model=self.model,
                messages=[{"role": "user", "content": prompt}],
                temperature=0.0,
                max_tokens=300,
            )
            hyde_text = response.choices[0].message.content.strip()
            if hyde_text and len(hyde_text) > 10:
                logger.debug(f"HyDE 生成: '{query[:50]}...' → '{hyde_text[:80]}...'")
                return hyde_text
            return None
        except Exception as e:
            logger.warning(f"HyDE 生成失败: {e}")
            return None

    def _is_simple(self, query: str) -> bool:
        """判断是否需要跳过改写（太简单的问题改了反而画蛇添足）"""
        # 纯数字计算跳过
        if any(
            op in query
            for op in ["+", "-", "×", "÷", "=", "计算", "1+1", "等于"]
        ):
            return True
        # 太短跳过
        if len(query) < 4:
            return True
        # 已经是完整独立问题，不需要改写
        if not any(w in query for w in ["那个", "上次", "之前", "这个", "呢", "嘛"]):
            if len(query) > 10:
                return True
        return False

    def _format_history(self, chat_history: List[Dict]) -> str:
        """将对话历史格式化为文本"""
        lines = []
        for msg in chat_history[-6:]:  # 只取最近 3 轮
            role = "用户" if msg.get("role") == "user" else "助手"
            content = msg.get("content", "")
            if len(content) > 150:
                content = content[:150] + "..."
            lines.append(f"{role}: {content}")
        return "\n".join(lines)
