"""LLM 客户端与 Mock 客户端测试。"""
import json

import pytest

from llm_features.client import OpenAICompatibleClient
from llm_features.mock import MockLLMClient


def test_mock_client_returns_valid_json():
    client = MockLLMClient()
    raw = client.complete("system", "收入证明与申报一致，工作稳定。")
    payload = json.loads(raw)
    assert payload["llm_verdict"] is False
    assert payload["doc_income_consistency"] is True


def test_mock_client_flags_high_risk_materials():
    client = MockLLMClient()
    raw = client.complete(
        "system",
        "材料不一致，收入证明模糊无法辨认；征信显示频繁查询、多头借贷；"
        "存在逾期和诉讼记录；客户无固定工作。",
    )
    payload = json.loads(raw)
    assert payload["llm_verdict"] is True
    assert payload["doc_material_mismatch"] is True
    assert payload["doc_suspicious_document"] is True
    assert payload["doc_multi_lender_signal"] is True
    assert payload["doc_negative_public_record"] is True
    assert payload["doc_employment_risk"] is True
    assert payload["llm_confidence"] > 0.5


def test_mock_client_is_deterministic():
    client = MockLLMClient()
    prompt = "收入证明与申报一致"
    assert client.complete("s", prompt) == client.complete("s", prompt)


def test_mock_client_marks_empty_materials_as_missing_info():
    payload = json.loads(MockLLMClient().complete("s", "(无材料)"))
    assert payload["doc_missing_key_info"] is True


def test_openai_client_requires_api_key(monkeypatch):
    monkeypatch.delenv("LLM_API_KEY", raising=False)
    monkeypatch.delenv("OPENAI_API_KEY", raising=False)
    with pytest.raises(ValueError, match="LLM_API_KEY"):
        OpenAICompatibleClient()


def test_openai_client_uses_env_defaults(monkeypatch):
    monkeypatch.setenv("LLM_API_KEY", "test-key")
    monkeypatch.setenv("LLM_MODEL", "test-model")
    monkeypatch.setenv("LLM_BASE_URL", "https://example.com/v1")
    client = OpenAICompatibleClient()
    assert client.model == "test-model"
    assert client.base_url == "https://example.com/v1"
