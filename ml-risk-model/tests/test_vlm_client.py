"""VLM 客户端测试：Mock 夹具、缺省输出、真实客户端配置。"""
import pytest

from llm_features.vlm_client import MockVLMClient, OpenAICompatibleVLMClient


def test_mock_reads_sidecar_fixture(tmp_path):
    source = tmp_path / "salary_cert.png"
    source.write_bytes(b"fake-image")
    fixture = tmp_path / "salary_cert.png.mock.md"
    fixture.write_text("月收入 8000 元，收入稳定。", encoding="utf-8")

    client = MockVLMClient()
    text = client.transcribe(
        source_path=source, image_bytes=b"fake-image", mime_type="image/png"
    )

    assert text == "月收入 8000 元，收入稳定。"
    assert client.calls == 1


def test_mock_prefers_page_fixture(tmp_path):
    source = tmp_path / "scan.pdf"
    source.write_bytes(b"fake-pdf")
    (tmp_path / "scan.pdf.mock.md").write_text("整份文档", encoding="utf-8")
    (tmp_path / "scan.pdf.page2.mock.md").write_text("第二页内容", encoding="utf-8")

    client = MockVLMClient()
    assert client.transcribe(
        source_path=source, image_bytes=b"", mime_type="image/png", page_index=1
    ) == "第二页内容"
    assert client.transcribe(
        source_path=source, image_bytes=b"", mime_type="image/png", page_index=5
    ) == "整份文档"


def test_mock_without_fixture_returns_placeholder(tmp_path):
    source = tmp_path / "unknown.jpg"
    source.write_bytes(b"fake")
    text = MockVLMClient().transcribe(
        source_path=source, image_bytes=b"fake", mime_type="image/jpeg"
    )
    assert "Mock VLM" in text
    assert "unknown.jpg" in text


def test_openai_vlm_requires_api_key(monkeypatch):
    monkeypatch.delenv("VLM_API_KEY", raising=False)
    monkeypatch.delenv("LLM_API_KEY", raising=False)
    monkeypatch.delenv("OPENAI_API_KEY", raising=False)
    with pytest.raises(ValueError, match="VLM_API_KEY"):
        OpenAICompatibleVLMClient()


def test_openai_vlm_falls_back_to_llm_api_key(monkeypatch):
    monkeypatch.delenv("VLM_API_KEY", raising=False)
    monkeypatch.delenv("OPENAI_API_KEY", raising=False)
    monkeypatch.setenv("LLM_API_KEY", "shared-key")
    client = OpenAICompatibleVLMClient()
    assert client.api_key == "shared-key"


def test_openai_vlm_uses_env_defaults(monkeypatch):
    monkeypatch.setenv("VLM_API_KEY", "test-key")
    monkeypatch.setenv("VLM_MODEL", "test-vlm")
    monkeypatch.setenv("VLM_BASE_URL", "https://example.com/v1")
    client = OpenAICompatibleVLMClient()
    assert client.model == "test-vlm"
    assert client.base_url == "https://example.com/v1"
