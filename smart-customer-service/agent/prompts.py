# smart-customer-service/agent/prompts.py
from utils.mongodb_client import mongodb_client
from typing import Dict, Optional

# 默认的 Fallback 模板，防止数据库不可用时系统崩溃
DEFAULT_SYSTEM_PROMPT = """你是一个友好的贷款智能客服。

当前时间是：{current_date}
请严格遵守以下规则：

1. **贷款申请状态**：如果用户询问进度或状态，必须使用 `query_application_status` 工具。
2. **还款计算**：如果用户询问月供、利息或计划，必须使用 `calculate_repayment` 工具。
3. **通用知识问答**：对于所有其他问题（包括产品规则、政策、常见问题等），必须先使用 `search_knowledge` 工具在知识库中检索相关信息，基于检索结果回答。
   - *注意：不要假设知识库内容，必须显式调用此工具获取最新信息。*
4. **未知问题处理**：如果 `search_knowledge` 返回无结果，友好地回答“暂无相关信息”，不能编造信息。
5. **获取实时信息**：如果用户询问当前或现在最新的外部信息，使用 `search_web` 工具搜索网络，请基于当前时间（{current_date}）去搜索相关信息。
5. **拒绝行为**：严禁编造数据，严禁提及Token等技术术语。

回答要求：
- 语气友好、专业，简洁明了。
- 调用工具时，直接调用，不要询问用户是否需要调用。
"""

def get_system_prompt() -> str:
    """从数据库获取系统提示词，如果数据库不可用则使用默认模板"""
    try:
        # 从数据库获取激活的提示词
        prompt = mongodb_client.get_active_prompt()
        if prompt and "content" in prompt:
            content = prompt["content"]
            # 组装提示词
            system_prompt = f"{content.get('role_definition', '')}\n\n"
            system_prompt += f"当前时间是：{{current_date}}\n"
            system_prompt += f"请严格遵守以下规则：\n\n{content.get('business_rules', '')}\n\n"
            system_prompt += f"回答要求：\n{content.get('tone_style', '')}\n"
            return system_prompt
    except Exception as e:
        print(f"Error getting system prompt from database: {e}")
    # 如果数据库不可用，使用默认模板
    return DEFAULT_SYSTEM_PROMPT

def get_prompt_config() -> Dict:
    """获取提示词配置信息"""
    try:
        prompt = mongodb_client.get_active_prompt()
        if prompt and "config" in prompt:
            return prompt["config"]
    except Exception as e:
        print(f"Error getting prompt config from database: {e}")
    # 返回默认配置
    return {
        "protected_tools": [
            {"name": "query_application_status", "description": "查询贷款申请状态"},
            {"name": "calculate_repayment", "description": "计算贷款还款计划"},
            {"name": "search_knowledge", "description": "在知识库中搜索相关信息"},
            {"name": "search_web", "description": "搜索网络获取实时信息"}
        ],
        "variables": ["current_date"]
    }