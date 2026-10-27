"""在线服务的请求/响应模型。"""
from __future__ import annotations

from typing import Dict, List, Literal, Optional

from pydantic import BaseModel, ConfigDict, Field


class ApplicationContext(BaseModel):
    model_config = ConfigDict(extra="forbid")

    amount: Optional[float] = None
    term: Optional[int] = None
    income: Optional[float] = None


class DocumentRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    type: str = Field(max_length=50)
    text: Optional[str] = Field(default=None, max_length=50000)
    source_path: Optional[str] = Field(default=None, max_length=500)


class RiskFeatureRequest(BaseModel):
    model_config = ConfigDict(extra="forbid")

    application_id: int
    application: ApplicationContext = Field(default_factory=ApplicationContext)
    documents: List[DocumentRequest] = Field(default_factory=list, max_length=20)
    application_note: str = Field(default="", max_length=4000)


class DocumentConversionInfo(BaseModel):
    type: str
    status: str
    cached: bool = False
    error: Optional[str] = None


class RiskFeatureResponse(BaseModel):
    application_id: int
    status: Literal["ok", "error"]
    error_code: Optional[str] = None
    error: Optional[str] = None
    features: Dict[str, float]
    deterministic_features: Dict[str, float]
    document_conversion: List[DocumentConversionInfo] = Field(default_factory=list)
    reasoning: str = ""
    model: str
    cached: bool = False
