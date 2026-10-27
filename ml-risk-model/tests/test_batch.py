"""批量抽取与 CLI 端到端测试。"""
import json

from llm_features.batch import main, run_batch, write_jsonl
from llm_features.documents import DocumentConverter
from llm_features.extractor import LLMFeatureExtractor
from llm_features.mock import MockLLMClient
from llm_features.schema import DocumentInput, RiskRecordInput
from llm_features.vlm_client import MockVLMClient


class AlwaysBadClient:
    def __init__(self):
        self.calls = 0

    def complete(self, system, user):
        self.calls += 1
        return "not json"


def _raw(application_id=1, text="收入证明与申报一致，工作稳定。"):
    return {
        "application_id": application_id,
        "user_id": 9,
        "label": 0,
        "application": {"amount": 50000, "term": 12, "income": 8000},
        "documents": [{"type": "salary_cert", "text": text}],
        "application_note": "",
    }


def test_run_batch_outputs_one_record_per_input():
    extractor = LLMFeatureExtractor(MockLLMClient(), retry_delay=0)
    outputs, errors = run_batch([_raw(1), _raw(2)], extractor)

    assert len(outputs) == 2
    assert errors == []
    assert outputs[0]["application_id"] == 1
    assert outputs[0]["ok"] is True
    assert outputs[0]["status"] == "ok"
    assert outputs[0]["error_code"] is None
    assert "features" in outputs[0]
    assert "deterministic_features" in outputs[0]


def test_run_batch_respects_limit():
    extractor = LLMFeatureExtractor(MockLLMClient(), retry_delay=0)
    outputs, _ = run_batch([_raw(1), _raw(2), _raw(3)], extractor, limit=2)
    assert [row["application_id"] for row in outputs] == [1, 2]


def test_failed_extraction_is_reported_separately():
    extractor = LLMFeatureExtractor(AlwaysBadClient(), retry_delay=0)
    outputs, errors = run_batch([_raw(1)], extractor)

    assert len(outputs) == 1
    assert outputs[0]["ok"] is False
    assert outputs[0]["status"] == "error"
    assert outputs[0]["error_code"] == "LLM_EXTRACTION_FAILED"
    assert outputs[0]["features"]["llm_feature_available"] == 0
    assert len(errors) == 1
    assert errors[0]["error"]


def test_invalid_input_record_is_reported_without_crashing():
    extractor = LLMFeatureExtractor(MockLLMClient(), retry_delay=0)
    outputs, errors = run_batch([{"application_id": "not-an-int"}], extractor)

    assert len(errors) == 1
    assert "输入校验失败" in errors[0]["error"]
    assert outputs[0]["ok"] is False
    assert outputs[0]["status"] == "error"
    assert outputs[0]["error_code"] == "INVALID_INPUT"


def test_write_jsonl_roundtrip_unicode(tmp_path):
    path = tmp_path / "out.jsonl"
    rows = [{"reasoning": "收入证明一致"}]
    write_jsonl(path, rows)
    content = path.read_text(encoding="utf-8").strip()
    assert json.loads(content) == rows[0]


def test_batch_converts_source_path_documents(tmp_path):
    base = tmp_path / "uploads"
    base.mkdir()
    (base / "salary.png").write_bytes(b"fake-image")
    (base / "salary.png.mock.md").write_text(
        "收入证明与申报收入不一致。", encoding="utf-8"
    )
    converter = DocumentConverter(MockVLMClient(), base_dir=base)
    extractor = LLMFeatureExtractor(MockLLMClient(), retry_delay=0)

    raw = _raw(1)
    raw["documents"] = [{"type": "salary_cert", "source_path": "salary.png"}]
    outputs, errors = run_batch([raw], extractor, converter=converter)

    assert errors == []
    assert outputs[0]["ok"] is True
    assert outputs[0]["document_conversion"][0]["status"] == "ready"
    assert outputs[0]["features"]["doc_income_consistency"] == 0


def test_conversion_exception_is_reported(tmp_path):
    base = tmp_path / "uploads"
    base.mkdir()
    outside = tmp_path / "outside.png"
    outside.write_bytes(b"x")
    converter = DocumentConverter(MockVLMClient(), base_dir=base)
    extractor = LLMFeatureExtractor(MockLLMClient(), retry_delay=0)

    raw = _raw(1)
    raw["documents"] = [{"type": "salary_cert", "source_path": str(outside)}]
    outputs, errors = run_batch([raw], extractor, converter=converter)

    assert outputs[0]["ok"] is False
    assert outputs[0]["status"] == "error"
    assert outputs[0]["error_code"] == "DOCUMENT_CONVERSION_FAILED"
    assert "文档转换失败" in outputs[0]["error"]
    assert len(errors) == 1


def test_main_with_source_paths(tmp_path):
    base = tmp_path / "uploads"
    base.mkdir()
    (base / "salary.png").write_bytes(b"fake-image")
    (base / "salary.png.mock.md").write_text(
        "工资流水与申报收入一致，收入稳定。", encoding="utf-8"
    )
    input_path = tmp_path / "applications.jsonl"
    output_path = tmp_path / "features.jsonl"
    raw = _raw(1)
    raw["documents"] = [{"type": "salary_cert", "source_path": "salary.png"}]
    write_jsonl(input_path, [raw])

    exit_code = main([
        "--input", str(input_path),
        "--output", str(output_path),
        "--cache", str(tmp_path / "cache.json"),
        "--doc-base-dir", str(base),
        "--mock",
    ])

    assert exit_code == 0
    row = json.loads(output_path.read_text(encoding="utf-8").splitlines()[0])
    assert row["ok"] is True
    assert row["document_conversion"][0]["status"] == "ready"
    assert (base / "salary.png.md").exists()


def test_main_requires_base_dir_for_source_paths(tmp_path):
    input_path = tmp_path / "applications.jsonl"
    raw = _raw(1)
    raw["documents"] = [{"type": "salary_cert", "source_path": "salary.png"}]
    write_jsonl(input_path, [raw])

    exit_code = main([
        "--input", str(input_path),
        "--output", str(tmp_path / "out.jsonl"),
        "--mock",
    ])
    assert exit_code == 1


def test_main_mock_end_to_end(tmp_path):
    input_path = tmp_path / "applications.jsonl"
    output_path = tmp_path / "features.jsonl"
    cache_path = tmp_path / "cache.json"
    write_jsonl(input_path, [_raw(1), _raw(2, text="材料模糊，存在逾期记录。")])

    exit_code = main([
        "--input", str(input_path),
        "--output", str(output_path),
        "--cache", str(cache_path),
        "--mock",
    ])

    assert exit_code == 0
    rows = [json.loads(line) for line in output_path.read_text(
        encoding="utf-8").splitlines() if line.strip()]
    assert len(rows) == 2
    assert rows[1]["ok"] is True
    assert rows[1]["features"]["llm_feature_available"] == 1
    assert cache_path.exists()
    assert not (tmp_path / "features.jsonl.errors.jsonl").exists()
