# smart-customer-service/agent/models.py
from typing import Dict, Any
from pydantic import BaseModel, Field
import json


class ToolCallEvent(BaseModel):
    """工具调用事件模型"""
    type: str = Field(default="tool_call", description="事件类型")
    tool_name: str = Field(..., description="工具名称")
    arguments: Dict[str, Any] = Field(default_factory=dict, description="工具调用参数")

    def to_json(self) -> str:
        return json.dumps(self.model_dump(), ensure_ascii=False)


class ToolResultEvent(BaseModel):
    """工具执行结果事件模型"""
    type: str = Field(default="tool_result", description="事件类型")
    tool_name: str = Field(..., description="工具名称")
    result: Any = Field(default=None, description="工具执行结果")

    def to_json(self) -> str:
        return json.dumps(self.model_dump(), ensure_ascii=False)


class ErrorEvent(BaseModel):
    """错误事件模型"""
    type: str = Field(default="error", description="事件类型")
    message: str = Field(..., description="错误信息")

    def to_json(self) -> str:
        return json.dumps(self.model_dump(), ensure_ascii=False)
