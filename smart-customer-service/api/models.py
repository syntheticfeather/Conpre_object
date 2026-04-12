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
    message: str
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