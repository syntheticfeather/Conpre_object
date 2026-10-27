"""H 组确定性特征测试：纯函数、无 I/O。"""
import pytest

from llm_features.deterministic import compute_deterministic_features
from llm_features.schema import (
    DETERMINISTIC_FEATURE_NAMES,
    DocumentInput,
    RiskRecordInput,
)


def _record(documents, note="") -> RiskRecordInput:
    return RiskRecordInput(application_id=1, documents=documents, application_note=note)


def test_empty_record_yields_zero_features():
    flat = compute_deterministic_features(_record([]))
    assert set(flat) == set(DETERMINISTIC_FEATURE_NAMES)
    assert flat == {
        "doc_total_chars": 0,
        "doc_count": 0,
        "doc_type_coverage": 0.0,
        "note_length": 0,
        "has_ocr_text": 0,
    }


def test_counts_non_empty_documents_only():
    flat = compute_deterministic_features(_record([
        DocumentInput(type="salary_cert", text="12345"),
        DocumentInput(type="credit_report", text="   "),
    ]))
    assert flat["doc_total_chars"] == 5
    assert flat["doc_count"] == 1
    assert flat["has_ocr_text"] == 1


def test_type_coverage_counts_distinct_expected_types():
    flat = compute_deterministic_features(_record([
        DocumentInput(type="salary_cert", text="a"),
        DocumentInput(type="salary_cert", text="b"),
        DocumentInput(type="credit_report", text="c"),
        DocumentInput(type="unknown_type", text="d"),
    ]))
    assert flat["doc_type_coverage"] == pytest.approx(2 / 6)


def test_note_length_is_counted():
    flat = compute_deterministic_features(_record([], note="急用钱"))
    assert flat["note_length"] == 3


def test_blank_documents_do_not_set_ocr_flag():
    flat = compute_deterministic_features(_record([
        DocumentInput(type="salary_cert", text="  "),
    ]))
    assert flat["has_ocr_text"] == 0
