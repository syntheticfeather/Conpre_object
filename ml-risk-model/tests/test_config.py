"""`.env` 配置加载测试。"""
import os

from llm_features.config import load_env


def test_load_env_reads_key_values_and_quotes(tmp_path):
    path = tmp_path / ".env"
    path.write_text(
        "# comment\n"
        "CFG_TEST_PLAIN=abc\n"
        "CFG_TEST_QUOTED=\"x y\"\n"
        "export CFG_TEST_EXPORTED=1\n"
        "\n",
        encoding="utf-8",
    )
    for key in ("CFG_TEST_PLAIN", "CFG_TEST_QUOTED", "CFG_TEST_EXPORTED"):
        os.environ.pop(key, None)
    try:
        loaded = load_env(path)
        assert loaded == [path]
        assert os.environ["CFG_TEST_PLAIN"] == "abc"
        assert os.environ["CFG_TEST_QUOTED"] == "x y"
        assert os.environ["CFG_TEST_EXPORTED"] == "1"
    finally:
        for key in ("CFG_TEST_PLAIN", "CFG_TEST_QUOTED", "CFG_TEST_EXPORTED"):
            os.environ.pop(key, None)


def test_shell_env_wins_by_default(monkeypatch, tmp_path):
    monkeypatch.setenv("CFG_TEST_WINS", "shell")
    path = tmp_path / ".env"
    path.write_text("CFG_TEST_WINS=file\n", encoding="utf-8")
    load_env(path)
    assert os.environ["CFG_TEST_WINS"] == "shell"


def test_override_true_replaces_shell_env(monkeypatch, tmp_path):
    monkeypatch.setenv("CFG_TEST_OVERRIDE", "shell")
    path = tmp_path / ".env"
    path.write_text("CFG_TEST_OVERRIDE=file\n", encoding="utf-8")
    load_env(path, override=True)
    assert os.environ["CFG_TEST_OVERRIDE"] == "file"


def test_missing_file_is_ignored(tmp_path):
    assert load_env(tmp_path / "missing.env") == []
