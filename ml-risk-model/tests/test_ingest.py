"""申请记录与源文件解析测试。"""
from llm_features.documents import DocumentConverter
from llm_features.ingest import resolve_record
from llm_features.vlm_client import MockVLMClient


def _converter(tmp_path):
    base = tmp_path / "uploads"
    base.mkdir(exist_ok=True)
    return DocumentConverter(MockVLMClient(), base_dir=base), base


def _raw(documents, application_id=1):
    return {
        "application_id": application_id,
        "label": 0,
        "documents": documents,
        "application_note": "",
    }


def test_text_documents_are_kept_as_is(tmp_path):
    converter, _ = _converter(tmp_path)
    raw = _raw([{"type": "salary_cert", "text": "月收入 8000 元"}])

    resolved, infos = resolve_record(raw, converter)

    assert resolved["documents"] == [{"type": "salary_cert", "text": "月收入 8000 元"}]
    assert infos[0]["status"] == "provided"
    assert infos[0]["cached"] is False


def test_source_path_is_converted(tmp_path):
    converter, base = _converter(tmp_path)
    source = base / "salary.png"
    source.write_bytes(b"image")
    (base / "salary.png.mock.md").write_text("工资流水稳定", encoding="utf-8")

    resolved, infos = resolve_record(
        _raw([{"type": "salary_cert", "source_path": "salary.png"}]),
        converter,
    )

    assert resolved["documents"][0]["text"] == "工资流水稳定"
    assert infos[0]["status"] == "ready"
    assert infos[0]["md_path"].endswith("salary.png.md")


def test_missing_source_is_reported_without_crashing(tmp_path):
    converter, _ = _converter(tmp_path)
    resolved, infos = resolve_record(
        _raw([{"type": "salary_cert", "source_path": "missing.png"}]),
        converter,
    )

    assert resolved["documents"][0]["text"] == ""
    assert infos[0]["status"] == "missing"
    assert infos[0]["error"]


def test_document_without_text_or_path_is_reported(tmp_path):
    converter, _ = _converter(tmp_path)
    resolved, infos = resolve_record(
        _raw([{"type": "salary_cert"}]), converter
    )

    assert resolved["documents"][0]["text"] == ""
    assert infos[0]["status"] == "missing_source"


def test_mixed_documents_preserve_order(tmp_path):
    converter, base = _converter(tmp_path)
    (base / "credit.png").write_bytes(b"img")
    (base / "credit.png.mock.md").write_text("存在逾期记录", encoding="utf-8")

    raw = _raw([
        {"type": "salary_cert", "text": "收入一致"},
        {"type": "credit_report", "source_path": "credit.png"},
    ])
    resolved, infos = resolve_record(raw, converter)

    assert [doc["type"] for doc in resolved["documents"]] == [
        "salary_cert", "credit_report"
    ]
    assert [info["status"] for info in infos] == ["provided", "ready"]


def test_record_without_documents_is_supported(tmp_path):
    converter, _ = _converter(tmp_path)
    resolved, infos = resolve_record(_raw([]), converter)

    assert resolved["documents"] == []
    assert infos == []


def test_original_record_is_not_mutated(tmp_path):
    converter, base = _converter(tmp_path)
    (base / "salary.png").write_bytes(b"img")
    (base / "salary.png.mock.md").write_text("文本", encoding="utf-8")
    raw = _raw([{"type": "salary_cert", "source_path": "salary.png"}])

    resolve_record(raw, converter)

    assert raw["documents"][0] == {
        "type": "salary_cert", "source_path": "salary.png"
    }
