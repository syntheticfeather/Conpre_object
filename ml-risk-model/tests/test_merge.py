"""特征合并测试：左连接、缺失填充、标签隔离。"""
import json

import pandas as pd

from llm_features.merge import (
    load_and_merge,
    load_jsonl,
    merge_records,
)
from llm_features.schema import (
    DETERMINISTIC_FEATURE_NAMES,
    LLM_FEATURE_NAMES,
)


def _tabular():
    return [
        {
            "application_id": 1,
            "label": 0,
            "age": 30,
            "monthly_income": 8000,
        },
        {
            "application_id": 2,
            "label": 1,
            "age": 45,
            "monthly_income": 4000,
        },
    ]


def _llm(application_id=1, ok=True):
    if not ok:
        return {
            "application_id": application_id,
            "ok": False,
            "error": "timeout",
            "features": {},
            "deterministic_features": {},
        }
    return {
        "application_id": application_id,
        "ok": True,
        "features": {
            "llm_verdict": 1,
            "llm_confidence": 0.9,
            "doc_income_consistency": 0,
            "doc_income_stability": 1,
            "doc_employment_risk": 0,
            "doc_multi_lender_signal": 1,
            "doc_material_mismatch": 0,
            "doc_suspicious_document": 0,
            "doc_debt_pressure": 1,
            "doc_negative_public_record": 0,
            "doc_missing_key_info": 0,
            "llm_feature_available": 1,
        },
        "deterministic_features": {
            "doc_total_chars": 500,
            "doc_count": 3,
            "doc_type_coverage": 0.5,
            "note_length": 10,
            "has_ocr_text": 1,
        },
    }


def test_all_feature_columns_exist():
    merged = merge_records(_tabular(), [_llm()])
    for name in LLM_FEATURE_NAMES + DETERMINISTIC_FEATURE_NAMES:
        assert name in merged.columns


def test_missing_llm_record_falls_back_to_defaults():
    merged = merge_records(_tabular(), [_llm(application_id=1)]).set_index(
        "application_id"
    )
    assert merged.loc[1, "llm_feature_available"] == 1
    assert merged.loc[2, "llm_feature_available"] == 0
    assert merged.loc[2, "llm_verdict"] == 0
    assert merged.loc[2, "doc_total_chars"] == 0


def test_label_comes_from_tabular_not_llm_output():
    llm = _llm()
    llm["label"] = 999
    merged = merge_records(_tabular(), [llm]).set_index("application_id")
    assert merged.loc[1, "label"] == 0


def test_tabular_llm_columns_are_dropped_before_merge():
    tabular = _tabular()
    tabular[0]["llm_verdict"] = 999
    merged = merge_records(tabular, [_llm()]).set_index("application_id")
    assert "llm_verdict_llm" not in merged.columns
    assert merged.loc[1, "llm_verdict"] == 1


def test_empty_llm_input_is_supported():
    merged = merge_records(_tabular(), [])
    assert len(merged) == 2
    assert (merged["llm_feature_available"] == 0).all()


def test_load_jsonl_roundtrip(tmp_path):
    path = tmp_path / "records.jsonl"
    rows = [{"application_id": 1, "label": 0}]
    path.write_text(
        "\n".join(json.dumps(row, ensure_ascii=False) for row in rows),
        encoding="utf-8",
    )
    assert load_jsonl(path) == rows


def test_load_and_merge_from_files(tmp_path):
    tabular_path = tmp_path / "tabular.jsonl"
    llm_path = tmp_path / "llm.jsonl"
    tabular_path.write_text(
        json.dumps(_tabular()[0], ensure_ascii=False), encoding="utf-8"
    )
    llm_path.write_text(
        json.dumps(_llm(), ensure_ascii=False), encoding="utf-8"
    )

    merged = load_and_merge(tabular_path, llm_path)
    assert isinstance(merged, pd.DataFrame)
    assert len(merged) == 1
    assert merged.iloc[0]["llm_feature_available"] == 1
