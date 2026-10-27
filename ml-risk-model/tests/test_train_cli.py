"""训练 CLI 端到端测试。"""
import json

import train_with_llm_features as cli
from llm_features.sample_data import write_sample_files


def test_train_cli_writes_ablation_report(tmp_path):
    paths = write_sample_files(tmp_path / "data", n=180, seed=5)
    report_path = tmp_path / "report.json"

    exit_code = cli.main([
        "--tabular", str(paths["tabular"]),
        "--llm", str(paths["llm"]),
        "--out", str(report_path),
        "--model", "logreg",
    ])

    assert exit_code == 0
    report = json.loads(report_path.read_text(encoding="utf-8"))
    assert set(report["feature_sets"]) == {"baseline", "llm", "full"}
    assert report["synthetic"] is True
    assert report["model"] == "LogisticRegression"
