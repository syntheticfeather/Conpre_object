"""抽取器测试：解析、修复重试、降级、缓存。"""
import json

import pytest

from llm_features.cache import FileCache
from llm_features.extractor import LLMFeatureExtractor
from llm_features.mock import MockLLMClient
from llm_features.schema import DocumentInput, RiskRecordInput

VALID_JSON = json.dumps({
    "reasoning": "材料一致，收入稳定。",
    "llm_verdict": False,
    "llm_confidence": 0.8,
    "doc_income_consistency": True,
    "doc_income_stability": True,
    "doc_employment_risk": False,
    "doc_multi_lender_signal": False,
    "doc_material_mismatch": False,
    "doc_suspicious_document": False,
    "doc_debt_pressure": False,
    "doc_negative_public_record": False,
    "doc_missing_key_info": False,
}, ensure_ascii=False)


class FakeClient:
    def __init__(self, responses):
        self.responses = list(responses)
        self.calls = []

    def complete(self, system: str, user: str) -> str:
        self.calls.append((system, user))
        value = self.responses.pop(0)
        if isinstance(value, Exception):
            raise value
        return value


def _record() -> RiskRecordInput:
    return RiskRecordInput(
        application_id=1,
        documents=[DocumentInput(type="salary_cert", text="月收入 8000 元")],
        application_note="",
    )


def test_extract_success_returns_features_and_deterministic():
    client = FakeClient([VALID_JSON])
    result = LLMFeatureExtractor(client, retry_delay=0).extract(_record())

    assert result.ok is True
    assert result.features["llm_feature_available"] == 1
    assert result.features["doc_income_consistency"] == 1
    assert result.deterministic_features["doc_count"] == 1
    assert result.error is None
    assert len(client.calls) == 1


def test_extract_parses_json_inside_code_fence():
    client = FakeClient(["```json\n" + VALID_JSON + "\n```"])
    result = LLMFeatureExtractor(client, retry_delay=0).extract(_record())
    assert result.ok is True


def test_invalid_output_triggers_one_repair_attempt():
    client = FakeClient(["not json at all", VALID_JSON])
    result = LLMFeatureExtractor(client, retry_delay=0).extract(_record())

    assert result.ok is True
    assert len(client.calls) == 2
    assert "无法解析" in client.calls[1][1]


def test_repair_failure_falls_back_with_available_zero():
    client = FakeClient(["bad", "still bad"])
    result = LLMFeatureExtractor(client, retry_delay=0).extract(_record())

    assert result.ok is False
    assert result.features["llm_feature_available"] == 0
    assert result.error
    assert len(client.calls) == 2


def test_transient_exception_is_retried():
    client = FakeClient([RuntimeError("timeout"), VALID_JSON])
    result = LLMFeatureExtractor(client, retries=2, retry_delay=0).extract(_record())

    assert result.ok is True
    assert len(client.calls) == 2


def test_persistent_exception_falls_back():
    client = FakeClient([RuntimeError("down"), RuntimeError("down")])
    result = LLMFeatureExtractor(client, retries=2, retry_delay=0).extract(_record())

    assert result.ok is False
    assert result.features["llm_feature_available"] == 0
    assert len(client.calls) == 2


def test_cache_hit_skips_second_llm_call(tmp_path):
    client = FakeClient([VALID_JSON])
    cache = FileCache(tmp_path / "cache.json")
    extractor = LLMFeatureExtractor(client, cache=cache, retry_delay=0)

    first = extractor.extract(_record())
    second = extractor.extract(_record())

    assert first.cached is False
    assert second.cached is True
    assert len(client.calls) == 1
    assert second.features == first.features


def test_flush_cache_persists_deferred_entries(tmp_path):
    path = tmp_path / "cache.json"
    client = FakeClient([VALID_JSON])
    extractor = LLMFeatureExtractor(
        client, cache=FileCache(path), retry_delay=0
    )

    extractor.extract(_record())
    assert len(FileCache(path)) == 0

    extractor.flush_cache()
    assert len(FileCache(path)) == 1


def test_mock_client_end_to_end_flags_risk():
    record = RiskRecordInput(
        application_id=2,
        documents=[DocumentInput(
            type="credit_report",
            text="材料不一致，征信频繁查询、多头借贷，存在逾期记录。",
        )],
    )
    result = LLMFeatureExtractor(MockLLMClient(), retry_delay=0).extract(record)

    assert result.ok is True
    assert result.features["llm_verdict"] == 1
    assert result.features["doc_multi_lender_signal"] == 1
    assert result.reasoning


def test_missing_required_field_is_treated_as_invalid():
    broken = json.loads(VALID_JSON)
    broken.pop("doc_debt_pressure")
    client = FakeClient([json.dumps(broken, ensure_ascii=False), VALID_JSON])
    result = LLMFeatureExtractor(client, retry_delay=0).extract(_record())
    assert result.ok is True
    assert len(client.calls) == 2
