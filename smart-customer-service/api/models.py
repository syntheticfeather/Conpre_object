# smart-customer-service/api/models.py
from pydantic import BaseModel, Field, field_serializer, ConfigDict
from typing import Optional, List, TypeVar, Generic
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

class ChatResponse(BaseModel):
    message: str
    session_id: str

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

class PromptUpdate(BaseModel):
    name: Optional[str] = None
    content: Optional[PromptContent] = None
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
    server_id: str
    transport: str = "sse"  # sse, websocket, stdio
    url: Optional[str] = None  # SSE/WebSocket 时需要
    api_key: Optional[str] = None  # SSE/WebSocket 时需要
    command: Optional[str] = None  # stdio 时需要
    args: Optional[List[str]] = []  # stdio 时需要
    timeout: Optional[int] = 30

class MCPServerResponse(BaseModel):
    server_id: str
    transport: str
    url: str
    timeout: int
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None

    @field_serializer('created_at', 'updated_at')
    def serialize_datetime(self, value: datetime):
        if value:
            return value.strftime("%Y-%m-%d %H:%M:%S")
        return None

class MCPServerListResponse(BaseModel):
    server_id: str
    transport: str
    url: str
    timeout: int
    created_at: datetime
    updated_at: datetime

    @field_serializer('created_at', 'updated_at')
    def serialize_datetime(self, value: datetime):
        return value.strftime("%Y-%m-%d %H:%M:%S")