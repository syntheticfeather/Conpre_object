"""G 组特征 schema 与输入契约测试。"""
import pytest
from pydantic import ValidationError

from llm_features.schema import (
    DETERMINISTIC_FEATURE_NAMES,
    LLM_FEATURE_NAMES,
    LLMRiskFeatures,
    RiskRecordInput,
    default_feature_dict,
)


def _valid_payload() -> dict:
    return {
        "reasoning": "收入证明与申报一致，但征信查询次数偏多。",
        "llm_verdict": True,
        "llm_confidence": 0.82,
        "doc_income_consistency": True,
        "doc_income_stability": True,
        "doc_employment_risk": False,
        "doc_multi_lender_signal": True,
        "doc_material_mismatch": False,
        "doc_suspicious_document": False,
        "doc_debt_pressure": True,
        "doc_negative_public_record": False,
        "doc_missing_key_info": False,
    }


def test_valid_features_convert_booleans_to_zero_one():
    features = LLMRiskFeatures(**_valid_payload())
    flat = features.to_feature_dict()

    assert set(flat) == set(LLM_FEATURE_NAMES)
    assert flat["llm_verdict"] == 1
    assert flat["doc_multi_lender_signal"] == 1
    assert flat["doc_employment_risk"] == 0
    assert flat["llm_confidence"] == pytest.approx(0.82)
    assert flat["llm_feature_available"] == 1


def test_feature_dict_excludes_reasoning():
    flat = LLMRiskFeatures(**_valid_payload()).to_feature_dict()
    assert "reasoning" not in flat


def test_confidence_out_of_range_is_rejected():
    payload = _valid_payload()
    payload["llm_confidence"] = 1.4
    with pytest.raises(ValidationError):
        LLMRiskFeatures(**payload)


def test_unknown_field_is_rejected():
    payload = _valid_payload()
    payload["unexpected"] = 1
    with pytest.raises(ValidationError):
        LLMRiskFeatures(**payload)


def test_default_feature_dict_marks_llm_unavailable():
    flat = default_feature_dict()
    assert set(flat) == set(LLM_FEATURE_NAMES)
    assert flat["llm_feature_available"] == 0
    assert flat["llm_verdict"] == 0
    assert flat["llm_confidence"] == 0.0


def test_risk_record_input_has_safe_defaults():
    record = RiskRecordInput(application_id=7)
    assert record.documents == []
    assert record.application_note == ""
    assert record.label is None


def test_deterministic_feature_names_are_stable():
    assert DETERMINISTIC_FEATURE_NAMES == [
        "doc_total_chars",
        "doc_count",
        "doc_type_coverage",
        "note_length",
        "has_ocr_text",
    ]
