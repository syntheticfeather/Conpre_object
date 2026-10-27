"""源文件 → 同名 Markdown 懒转换测试。"""
import json

import pymupdf
import pytest

from llm_features.documents import (
    CONVERTER_VERSION,
    TRUNCATION_MARKER,
    DocumentConverter,
)
from llm_features.vlm_client import MockVLMClient


class FailingVLM:
    model = "failing-vlm"

    def transcribe(self, **kwargs):
        raise RuntimeError("VLM service unavailable")


def _base(tmp_path):
    base = tmp_path / "uploads"
    base.mkdir()
    return base


def _write_image(base, name="salary.png", fixture="月收入 8000 元，收入稳定。"):
    source = base / name
    source.write_bytes(b"fake-image-bytes")
    if fixture is not None:
        (base / f"{name}.mock.md").write_text(fixture, encoding="utf-8")
    return source


def _write_text_pdf(path):
    doc = pymupdf.open()
    page = doc.new_page()
    page.insert_text((72, 72), "Salary certificate: monthly income 8000 CNY.")
    doc.save(path)
    doc.close()


def _write_scanned_pdf(path):
    doc = pymupdf.open()
    doc.new_page()
    doc.save(path)
    doc.close()


def test_image_conversion_writes_markdown_and_meta(tmp_path):
    base = _base(tmp_path)
    source = _write_image(base)
    converter = DocumentConverter(MockVLMClient(), base_dir=base)

    result = converter.resolve(source)

    assert result.ok is True
    assert result.text == "月收入 8000 元，收入稳定。"
    assert result.status == "ready"
    assert result.mode == "image_vlm"
    assert result.cached is False

    md_path = base / "salary.png.md"
    meta_path = base / "salary.png.md.meta.json"
    assert md_path.read_text(encoding="utf-8") == result.text
    meta = json.loads(meta_path.read_text(encoding="utf-8"))
    assert meta["status"] == "ready"
    assert meta["source_sha256"] == result.source_sha256
    assert meta["converter_version"] == CONVERTER_VERSION
    assert meta["model"] == "mock-vlm"
    assert meta["mode"] == "image_vlm"


def test_second_call_hits_cache(tmp_path):
    base = _base(tmp_path)
    source = _write_image(base)
    client = MockVLMClient()
    converter = DocumentConverter(client, base_dir=base)

    first = converter.resolve(source)
    second = converter.resolve(source)

    assert first.cached is False
    assert second.cached is True
    assert client.calls == 1


def test_source_change_invalidates_cache(tmp_path):
    base = _base(tmp_path)
    source = _write_image(base)
    client = MockVLMClient()
    converter = DocumentConverter(client, base_dir=base)

    converter.resolve(source)
    source.write_bytes(b"changed-image")
    result = converter.resolve(source)

    assert result.cached is False
    assert client.calls == 2


def test_force_reconverts(tmp_path):
    base = _base(tmp_path)
    source = _write_image(base)
    client = MockVLMClient()
    converter = DocumentConverter(client, base_dir=base)

    converter.resolve(source)
    result = converter.resolve(source, force=True)

    assert result.cached is False
    assert client.calls == 2


def test_markdown_source_is_read_directly(tmp_path):
    base = _base(tmp_path)
    source = base / "note.md"
    source.write_text("已有文本材料", encoding="utf-8")
    client = MockVLMClient()
    result = DocumentConverter(client, base_dir=base).resolve(source)

    assert result.ok is True
    assert result.text == "已有文本材料"
    assert result.mode == "source_text"
    assert result.md_path == source.resolve()
    assert client.calls == 0


def test_path_outside_base_dir_is_rejected(tmp_path):
    base = _base(tmp_path)
    outside = tmp_path / "outside.png"
    outside.write_bytes(b"x")
    converter = DocumentConverter(MockVLMClient(), base_dir=base)

    with pytest.raises(ValueError, match="uploads"):
        converter.resolve(outside)


def test_unsupported_extension_is_reported(tmp_path):
    base = _base(tmp_path)
    source = base / "resume.docx"
    source.write_bytes(b"x")
    result = DocumentConverter(MockVLMClient(), base_dir=base).resolve(source)

    assert result.ok is False
    assert result.status == "unsupported"
    assert result.error


def test_missing_source_is_reported(tmp_path):
    base = _base(tmp_path)
    result = DocumentConverter(MockVLMClient(), base_dir=base).resolve(
        base / "missing.png"
    )
    assert result.ok is False
    assert result.status == "missing"


def test_text_pdf_uses_text_layer_without_vlm(tmp_path):
    base = _base(tmp_path)
    source = base / "credit.pdf"
    _write_text_pdf(source)
    client = MockVLMClient()

    result = DocumentConverter(client, base_dir=base).resolve(source)

    assert result.ok is True
    assert "Salary certificate" in result.text
    assert result.mode == "pdf_text"
    assert result.page_count == 1
    assert client.calls == 0


def test_pdf_text_mode_respects_max_pages(tmp_path):
    base = _base(tmp_path)
    source = base / "multi.pdf"
    doc = pymupdf.open()
    for index in range(3):
        page = doc.new_page()
        page.insert_text(
            (72, 72),
            f"Page {index + 1} salary certificate monthly income 8000 CNY.",
        )
    doc.save(source)
    doc.close()

    converter = DocumentConverter(MockVLMClient(), base_dir=base, max_pages=1)
    result = converter.resolve(source)

    assert result.ok is True
    assert "Page 1" in result.text
    assert "Page 2" not in result.text
    assert "已截断" in result.text
    assert result.page_count == 3


def test_scanned_pdf_uses_vlm_per_page(tmp_path):
    base = _base(tmp_path)
    source = base / "scan.pdf"
    _write_scanned_pdf(source)
    (base / "scan.pdf.page1.mock.md").write_text("扫描件第一页内容", encoding="utf-8")
    client = MockVLMClient()

    result = DocumentConverter(client, base_dir=base).resolve(source)

    assert result.ok is True
    assert "扫描件第一页内容" in result.text
    assert "<!-- page 1 -->" in result.text
    assert result.mode == "pdf_vlm"
    assert client.calls == 1


def test_vlm_failure_marks_meta_failed(tmp_path):
    base = _base(tmp_path)
    source = _write_image(base, fixture=None)
    result = DocumentConverter(FailingVLM(), base_dir=base).resolve(source)

    assert result.ok is False
    assert result.status == "failed"
    assert "unavailable" in result.error

    meta = json.loads(
        (base / "salary.png.md.meta.json").read_text(encoding="utf-8")
    )
    assert meta["status"] == "failed"
    assert not (base / "salary.png.md").exists()


def test_empty_transcription_is_not_ok(tmp_path):
    base = _base(tmp_path)
    source = _write_image(base, fixture="   \n")
    result = DocumentConverter(MockVLMClient(), base_dir=base).resolve(source)

    assert result.ok is False
    assert result.status == "empty"


def test_long_transcription_is_truncated(tmp_path):
    base = _base(tmp_path)
    source = _write_image(base, fixture="长" * 100)
    converter = DocumentConverter(MockVLMClient(), base_dir=base, max_chars=10)
    result = converter.resolve(source)

    assert result.ok is True
    assert result.text == "长" * 10 + TRUNCATION_MARKER
