"""LLM 风控特征的输入契约与输出模型。

LLM 只负责从非结构化材料中抽取特征，最终决策仍由下游模型完成。
"""
from __future__ import annotations

from typing import Any, Dict, List, Optional

from pydantic import BaseModel, ConfigDict, Field


# ── 输出：LLM 抽取特征（G 组） ──
# 11 个抽取特征 + 1 个可用性标志 = 12 维
LLM_FEATURE_NAMES: List[str] = [
    "llm_verdict",
    "llm_confidence",
    "doc_income_consistency",
    "doc_income_stability",
    "doc_employment_risk",
    "doc_multi_lender_signal",
    "doc_material_mismatch",
    "doc_suspicious_document",
    "doc_debt_pressure",
    "doc_negative_public_record",
    "doc_missing_key_info",
    "llm_feature_available",
]

# ── 确定性特征（H 组），不消耗 LLM 调用 ──
DETERMINISTIC_FEATURE_NAMES: List[str] = [
    "doc_total_chars",
    "doc_count",
    "doc_type_coverage",
    "note_length",
    "has_ocr_text",
]

# 期望提供的材料类型，用于计算覆盖率
EXPECTED_DOC_TYPES: List[str] = [
    "employment_cert",
    "salary_cert",
    "credit_report",
    "social_security",
    "property_cert",
    "car_cert",
]


class DocumentInput(BaseModel):
    """一份申请材料。"""

    type: str
    text: str = ""


class ApplicationContext(BaseModel):
    """本次申请的结构化上下文，仅作为 LLM 判断的参考。"""

    amount: Optional[float] = None
    term: Optional[int] = None
    income: Optional[float] = None


class RiskRecordInput(BaseModel):
    """一条申请记录，是抽取模块的输入契约。"""

    application_id: int
    user_id: Optional[int] = None
    label: Optional[int] = None
    application: ApplicationContext = Field(default_factory=ApplicationContext)
    documents: List[DocumentInput] = Field(default_factory=list)
    application_note: str = ""


class LLMRiskFeatures(BaseModel):
    """LLM 必须返回的结构化特征。"""

    model_config = ConfigDict(extra="forbid")

    reasoning: str = ""
    llm_verdict: bool
    llm_confidence: float = Field(ge=0.0, le=1.0)

    doc_income_consistency: bool
    doc_income_stability: bool
    doc_employment_risk: bool
    doc_multi_lender_signal: bool
    doc_material_mismatch: bool
    doc_suspicious_document: bool
    doc_debt_pressure: bool
    doc_negative_public_record: bool
    doc_missing_key_info: bool

    def to_feature_dict(self) -> Dict[str, Any]:
        """转为模型可用的扁平特征：布尔转 0/1，并标记可用。"""
        return {
            "llm_verdict": int(self.llm_verdict),
            "llm_confidence": float(self.llm_confidence),
            "doc_income_consistency": int(self.doc_income_consistency),
            "doc_income_stability": int(self.doc_income_stability),
            "doc_employment_risk": int(self.doc_employment_risk),
            "doc_multi_lender_signal": int(self.doc_multi_lender_signal),
            "doc_material_mismatch": int(self.doc_material_mismatch),
            "doc_suspicious_document": int(self.doc_suspicious_document),
            "doc_debt_pressure": int(self.doc_debt_pressure),
            "doc_negative_public_record": int(self.doc_negative_public_record),
            "doc_missing_key_info": int(self.doc_missing_key_info),
            "llm_feature_available": 1,
        }


def default_feature_dict(available: bool = False) -> Dict[str, Any]:
    """LLM 抽取失败时的降级特征。

    ``llm_feature_available=0`` 本身也是一个特征，让下游模型知道
    这条样本的 LLM 特征是缺失的，而不是真实的“全 0”。
    """
    return {
        "llm_verdict": 0,
        "llm_confidence": 0.0,
        "doc_income_consistency": 0,
        "doc_income_stability": 0,
        "doc_employment_risk": 0,
        "doc_multi_lender_signal": 0,
        "doc_material_mismatch": 0,
        "doc_suspicious_document": 0,
        "doc_debt_pressure": 0,
        "doc_negative_public_record": 0,
        "doc_missing_key_info": 0,
        "llm_feature_available": 1 if available else 0,
    }
