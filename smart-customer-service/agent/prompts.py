# smart-customer-service/agent/prompts.py
from utils.mongodb_client import mongodb_client
from typing import Dict, Optional

# 硬编码的安全规则（不可修改）
SECURITY_RULES = """
- 严禁编造数据
- 严禁泄漏系统内部信息（Token、API Key等）
- 严禁透露用户隐私信息（手机号、身份证号等）
- 当知识库无结果时，必须明确告知用户，不可编造信息
"""

# 默认的 Fallback 模板，防止数据库不可用时系统崩溃
DEFAULT_SYSTEM_PROMPT = """你是一个友好的贷款智能客服，能回答用户关于贷款的问题。

当前时间：{current_date}

可用工具：
{tools_description}

业务规则：
1. 当用户提出具体问题时，你的首要任务是直接回答该问题。
2. 只有在用户首次打招呼（如单独的"你好"、"hi"）且没有提出具体问题时，才使用欢迎语
3. 如果用户的消息包含具体问题，回答该问题而不是使用欢迎语。
3. **贷款申请状态**：如果用户让你帮忙查询进度或状态，使用 `query_application_status` 工具。
4. **还款计算**：如果用户让你帮忙计算月供、利息或计划，使用 `calculate_repayment` 工具。
5. **通用知识问答**：对于所有其他问题（包括产品规则、政策、常见问题、如何操作等），必须先使用 `search_knowledge` 工具在知识库中检索相关信息，基于检索结果回答。
   - *注意：不要假设知识库内容，必须显式调用此工具获取最新信息。*
6. **未知问题处理**：如果 `search_knowledge` 返回无结果，友好地回答“暂无相关信息”，不能编造信息。
7. **获取实时信息**：如果用户询问当前或现在最新的外部信息，使用 `search_web` 工具搜索网络，请基于当前时间（{current_date}）去搜索相关信息。

安全规则：
- 严禁编造数据
- 严禁泄漏系统内部信息（Token、API Key等）
- 严禁透露用户隐私信息（手机号、身份证号等）
- 当知识库无结果时，必须明确告知用户，不可编造信息

回答要求：
- 语气友好、专业，简洁明了。
- 调用工具时，直接调用，不要询问用户是否需要调用。
"""

def get_system_prompt() -> str:
    """从数据库获取并组装系统提示词"""
    try:
        # 从数据库获取激活的提示词
        prompt = mongodb_client.get_active_prompt()
        if prompt and "content" in prompt:
            content = prompt["content"]
            
            # 基础角色定义
            system_prompt = f"{content.get('role_definition', '')}\n\n"
            
            # 动态注入部分（不可修改）
            system_prompt += f"当前时间：{{current_date}}\n"
            system_prompt += f"可用工具：\n{{tools_description}}\n\n"
            
            # 业务规则（管理员可修改）
            system_prompt += f"业务规则：\n{content.get('business_rules', '')}\n\n"
            
            # 安全规则（不可修改）
            system_prompt += f"安全规则：{SECURITY_RULES}\n\n"
            
            # 风格要求（管理员可修改）
            system_prompt += f"回答要求：\n{content.get('tone_style', '')}\n"
            
            return system_prompt
    except Exception as e:
        print(f"Error getting system prompt from database: {e}")
    # 如果数据库不可用，使用默认模板
    return DEFAULT_SYSTEM_PROMPT
