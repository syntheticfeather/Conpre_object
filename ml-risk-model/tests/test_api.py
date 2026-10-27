"""在线服务与安全测试。"""
import json
import time

import pytest
from fastapi.testclient import TestClient

from llm_features.api.app import create_app
from llm_features.api.service import RiskFeatureService
from llm_features.cache import FileCache
from llm_features.deterministic import compute_deterministic_features
from llm_features.documents import DocumentConverter
from llm_features.extractor import ExtractionResult, LLMFeatureExtractor
from llm_features.schema import default_feature_dict
from llm_features.vlm_client import MockVLMClient

API_KEY = "test-key"


class FakeExtractor:
    model = "fake-model"

    def __init__(self, ok=True, delay=0.0):
        self.ok = ok
        self.delay = delay
        self.records = []

    def extract(self, record):
        self.records.append(record)
        if self.delay:
            time.sleep(self.delay)
        if not self.ok:
            return ExtractionResult(
                application_id=record.application_id,
                features=default_feature_dict(available=False),
                deterministic_features=compute_deterministic_features(record),
                ok=False,
                error="internal detail that must not leak",
            )
        return ExtractionResult(
            application_id=record.application_id,
            features=default_feature_dict(available=True),
            deterministic_features=compute_deterministic_features(record),
            ok=True,
            reasoning="fake reasoning",
            cached=False,
        )


def _body(application_id=1, text="收入证明与申报收入一致。"):
    return {
        "application_id": application_id,
        "application": {"amount": 50000, "term": 12, "income": 8000},
        "documents": [{"type": "salary_cert", "text": text}],
        "application_note": "正常申请",
    }


def _client(service=None, api_key=API_KEY):
    service = service or RiskFeatureService(FakeExtractor(), timeout=5)
    return TestClient(create_app(service=service, api_key=api_key))


def _post(client, body, key=API_KEY):
    headers = {"X-API-Key": key} if key is not None else {}
    return client.post("/v1/risk/features", json=body, headers=headers)


def test_health_is_public():
    response = _client().get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "ok"
    assert response.json()["auth_configured"] is True


def test_missing_api_key_is_rejected():
    response = _post(_client(), _body(), key=None)
    assert response.status_code == 401


def test_wrong_api_key_is_rejected():
    response = _post(_client(), _body(), key="wrong")
    assert response.status_code == 401


def test_valid_request_returns_features():
    response = _post(_client(), _body())
    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "ok"
    assert body["error_code"] is None
    assert body["features"]["llm_feature_available"] == 1
    assert body["model"] == "fake-model"
    assert response.headers["X-Request-ID"]


def test_pii_is_redacted_before_extraction():
    extractor = FakeExtractor()
    client = _client(RiskFeatureService(extractor, timeout=5))
    text = "身份证 110101199001011234，手机 13800138000，银行卡 6222020200112233445。"

    response = _post(client, _body(text=text))

    assert response.status_code == 200
    sent_text = extractor.records[0].documents[0].text
    assert "110101199001011234" not in sent_text
    assert "13800138000" not in sent_text
    assert "6222020200112233445" not in sent_text
    assert "[身份证]" in sent_text
    assert "[手机号]" in sent_text
    assert "[银行卡]" in sent_text


def test_extraction_failure_returns_degraded_200():
    client = _client(RiskFeatureService(FakeExtractor(ok=False), timeout=5))
    response = _post(client, _body())

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "error"
    assert body["error_code"] == "LLM_EXTRACTION_FAILED"
    assert body["error"] == "特征抽取失败"
    assert "internal detail" not in response.text
    assert body["features"]["llm_feature_available"] == 0


def test_timeout_returns_degraded_200():
    service = RiskFeatureService(FakeExtractor(delay=0.2), timeout=0.05)
    client = _client(service)
    response = _post(client, _body())

    assert response.status_code == 200
    body = response.json()
    assert body["status"] == "error"
    assert body["error_code"] == "TIMEOUT"
    assert body["features"]["llm_feature_available"] == 0


def test_path_traversal_returns_400(tmp_path):
    base = tmp_path / "uploads"
    base.mkdir()
    outside = tmp_path / "outside.png"
    outside.write_bytes(b"x")
    converter = DocumentConverter(MockVLMClient(), base_dir=base)
    service = RiskFeatureService(FakeExtractor(), converter=converter, timeout=5)

    body = _body()
    body["documents"] = [{"type": "salary_cert", "source_path": str(outside)}]
    response = _post(_client(service), body)

    assert response.status_code == 400
    assert response.json()["error_code"] == "INVALID_PATH"


def test_source_path_is_converted(tmp_path):
    base = tmp_path / "uploads"
    base.mkdir()
    (base / "salary.png").write_bytes(b"image")
    (base / "salary.png.mock.md").write_text("工资流水稳定", encoding="utf-8")
    converter = DocumentConverter(MockVLMClient(), base_dir=base)
    extractor = FakeExtractor()
    service = RiskFeatureService(extractor, converter=converter, timeout=5)

    body = _body()
    body["documents"] = [{"type": "salary_cert", "source_path": "salary.png"}]
    response = _post(_client(service), body)

    assert response.status_code == 200
    assert response.json()["document_conversion"][0]["status"] == "ready"
    assert extractor.records[0].documents[0].text == "工资流水稳定"


def test_invalid_payload_returns_422():
    body = _body()
    body.pop("application_id")
    response = _post(_client(), body)
    assert response.status_code == 422


def test_extra_label_field_is_rejected():
    body = _body()
    body["label"] = 1
    response = _post(_client(), body)
    assert response.status_code == 422


def test_large_body_is_rejected():
    body = _body(text="长" * 1_100_000)
    response = _post(_client(), body)
    assert response.status_code == 413


def test_unconfigured_auth_returns_503():
    client = _client(api_key="")
    response = _post(client, _body(), key="anything")
    assert response.status_code == 503


class StaticLLMClient:
    def __init__(self):
        self.calls = 0

    def complete(self, system, user):
        self.calls += 1
        return json.dumps({
            "reasoning": "静态测试输出",
            "llm_verdict": False,
            "llm_confidence": 0.7,
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


def _cached_service(tmp_path, llm):
    extractor = LLMFeatureExtractor(
        llm,
        cache=FileCache(tmp_path / "feature_cache.json"),
        retry_delay=0,
    )
    return RiskFeatureService(extractor, timeout=5)


def test_same_request_hits_feature_cache(tmp_path):
    llm = StaticLLMClient()
    client = _client(_cached_service(tmp_path, llm))

    first = _post(client, _body())
    second = _post(client, _body())

    assert first.json()["cached"] is False
    assert second.json()["cached"] is True
    assert llm.calls == 1


def test_changed_document_misses_feature_cache(tmp_path):
    llm = StaticLLMClient()
    client = _client(_cached_service(tmp_path, llm))

    _post(client, _body(text="收入证明与申报收入一致。"))
    response = _post(client, _body(text="收入证明与申报收入不一致。"))

    assert llm.calls == 2
    assert response.json()["cached"] is False


def test_changed_context_misses_feature_cache(tmp_path):
    llm = StaticLLMClient()
    client = _client(_cached_service(tmp_path, llm))

    _post(client, _body())
    body = _body()
    body["application"]["income"] = 12000
    response = _post(client, body)

    assert llm.calls == 2
    assert response.json()["cached"] is False


def test_feature_cache_persists_across_service_instances(tmp_path):
    first_llm = StaticLLMClient()
    _post(_client(_cached_service(tmp_path, first_llm)), _body())

    second_llm = StaticLLMClient()
    response = _post(_client(_cached_service(tmp_path, second_llm)), _body())

    assert response.json()["cached"] is True
    assert second_llm.calls == 0
