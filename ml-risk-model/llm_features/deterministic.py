"""H 组确定性特征：不调用 LLM，成本为零，可作为兜底信号。"""
from __future__ import annotations

from typing import Any, Dict

from llm_features.schema import EXPECTED_DOC_TYPES, RiskRecordInput


def compute_deterministic_features(record: RiskRecordInput) -> Dict[str, Any]:
    """计算材料层面的确定性特征。"""
    non_empty = [doc for doc in record.documents if doc.text.strip()]
    total_chars = sum(len(doc.text.strip()) for doc in non_empty)
    distinct_types = {doc.type for doc in non_empty}
    covered = distinct_types.intersection(EXPECTED_DOC_TYPES)

    return {
        "doc_total_chars": total_chars,
        "doc_count": len(non_empty),
        "doc_type_coverage": len(covered) / len(EXPECTED_DOC_TYPES),
        "note_length": len(record.application_note),
        "has_ocr_text": 1 if total_chars > 0 else 0,
    }
