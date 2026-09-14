# smart-customer-service/api/models.py
from pydantic import BaseModel, Field, field_serializer, ConfigDict
from typing import Optional, List, TypeVar, Generic, Dict, Any
from datetime import datetime

# 定义一个泛型类型变量 T
T = TypeVar('T')

class ResponseModel(BaseModel, Generic[T]):
    """
    统一API响应结构
    """
    code: int = Field(..., description="状态码")
    data: Optional[T] = Field(None, description="业务数据")
    message: str = Field(..., description="提示信息")

    class Config:
        # 允许在示例中使用泛型
        arbitrary_types_allowed = True
        # 这里可以配置 json_encoders 等

class ChatRequest(BaseModel):
    message: str = Field(..., min_length=1, max_length=1000, description="用户消息")
    session_id: Optional[str] = None
    agent_mode: Optional[str] = "react"  # react / plan / reflection / plan+reflection

class ChatResponse(BaseModel):
    message: str
    session_id: str

class ChatTurnPersistRequest(BaseModel):
    """WORKFLOW 路径对话落库请求（Java workflow 处理完后回调）。

    WORKFLOW 请求在 Java 本地处理，不经过 /api/chat/stream，
    处理完成后由 Java 调 POST /api/chat/persist 把这一轮对话补录到 MongoDB。
    """
    session_id: str = Field(..., min_length=1, description="会话ID（与前端一致）")
    message: str = Field(..., min_length=1, max_length=2000, description="用户原话")
    response: str = Field(..., min_length=1, max_length=10000, description="workflow 最终回复文本")

class KnowledgeItemCreate(BaseModel):
    question: str
    answer: str
    category: str = "通用"

class KnowledgeItemResponse(BaseModel, Generic[T]):
    # 启用这个配置，允许从任意对象（如 ORM 模型）读取数据，而不仅仅是字典
    model_config = ConfigDict(from_attributes=True) 

    id: str
    question: str
    answer: str
    category: str
    created_at: datetime
    updated_at: datetime

    @field_serializer('created_at', 'updated_at')
    def serialize_datetime(self, value: datetime):
        return value.strftime("%Y-%m-%d %H:%M:%S")

class KnowledgeItemUpdate(BaseModel):
    question: Optional[str] = None
    answer: Optional[str] = None
    category: Optional[str] = None

class PromptContent(BaseModel):
    role_definition: str = Field(..., min_length=1, description="AI 的身份定义")
    business_rules: str = Field(..., min_length=1, description="业务逻辑约束")
    tone_style: str = Field(..., min_length=1, description="回复的语气风格")


class PromptContentUpdate(BaseModel):
    role_definition: Optional[str] = None
    business_rules: Optional[str] = None
    tone_style: Optional[str] = None


class PromptUpdate(BaseModel):
    name: Optional[str] = None
    content: Optional[PromptContentUpdate] = None
    is_active: Optional[bool] = None

class PromptResponse(BaseModel):
    prompt_id: str
    name: str
    category: str
    is_active: bool
    version: str
    content: PromptContent
    created_at: datetime
    updated_at: datetime

    @field_serializer('created_at', 'updated_at')
    def serialize_datetime(self, value: datetime):
        return value.strftime("%Y-%m-%d %H:%M:%S")

# MCP服务器配置模型
class MCPServerCreate(BaseModel):
    server_id: str = Field(..., description="服务器的唯一标识，例如 'tavily-mcp'")
    
    # 核心改动：使用 Dict 接收任意结构的配置
    # 这里可以存放 command/args/env (Stdio)，也可以存放 url/headers (SSE)
    config: Dict[str, Any] = Field(
        default_factory=dict, 
        description="服务器的具体配置，结构取决于服务器类型（Stdio 或 SSE）"
    )

# --- 新增：通用的 MCP 服务器响应模型 ---
class MCPServerResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True) # 允许从 ORM/Dict 读取
    
    server_id: str = Field(..., description="服务器唯一ID")
    config: Dict[str, Any] = Field(..., description="原始配置信息")
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None

    @field_serializer('created_at', 'updated_at')
    def serialize_datetime(self, value: datetime):
        if value:
            return value.strftime("%Y-%m-%d %H:%M:%S")
        return None


# --- 路由请求/响应模型 ---

class RouteRequest(BaseModel):
    message: str = Field(..., min_length=1, max_length=1000, description="用户消息")

class RouteResult(BaseModel):
    intent: str = Field(..., description="分类意图")
    confidence: float = Field(..., ge=0.0, le=1.0, description="置信度 0~1")
    action: str = Field(..., description="WORKFLOW 或 AGENT")


# --- LLM 参数提取模型 ---

class ExtractLoanParamsRequest(BaseModel):
    message: str = Field(..., min_length=1, description="用户消息")
    available_products: list[str] = Field(default_factory=list, description="可用产品名列表")

class LoanParams(BaseModel):
    product_name: Optional[str] = None   # 产品名
    amount: Optional[float] = None       # 贷款金额（元）
    months: Optional[int] = None         # 贷款期限（月）
    repaid_type: Optional[str] = None    # 等额本息 / 等额本金 / 先息后本 / 一次性还本付息
    intent: Optional[str] = None         # apply / calculate / modify / cancel / unknown


# ==================== 会话与消息模型 ====================
# 配套 utils/mongodb_client.py 中 chat_sessions / chat_messages 两集合方案
# 注：新建会话 POST /api/sessions 无请求体——标题由后端占位生成，
#     首条消息后 LLM 异步摘要回填，用户手动改名走 PATCH。

class SessionUpdate(BaseModel):
    """更新会话请求（部分字段）"""
    title: Optional[str] = None
    pinned: Optional[bool] = None
    archived: Optional[bool] = None


class SessionPinRequest(BaseModel):
    """置顶/取消置顶请求"""
    pinned: bool = Field(..., description="True=置顶，False=取消置顶")


class SessionItem(BaseModel):
    """会话响应模型（列表 / 详情 / 新建 / PATCH / pin 通用）"""
    model_config = ConfigDict(from_attributes=True)

    session_id: str
    user_id: Optional[str] = None
    title: str
    last_message_preview: Optional[str] = ""
    last_message_at: Optional[datetime] = None
    message_count: int = 0
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None
    pinned: bool = False
    archived: bool = False

    @field_serializer('last_message_at', 'created_at', 'updated_at')
    def serialize_datetime(self, value: datetime):
        if value:
            return value.strftime("%Y-%m-%d %H:%M:%S")
        return None


class SessionListResponse(BaseModel):
    """会话列表分页响应"""
    items: List[SessionItem]
    next_cursor: Optional[str] = None
    has_more: bool = False


class MessageItem(BaseModel):
    """单条消息"""
    model_config = ConfigDict(from_attributes=True)

    message_id: str
    session_id: str
    user_id: Optional[str] = None
    role: str
    content: str
    created_at: Optional[datetime] = None

    @field_serializer('created_at')
    def serialize_datetime(self, value: datetime):
        if value:
            return value.strftime("%Y-%m-%d %H:%M:%S")
        return None


class MessageListResponse(BaseModel):
    """消息分页响应"""
    items: List[MessageItem]
    next_cursor: Optional[str] = None
    has_more: bool = False
