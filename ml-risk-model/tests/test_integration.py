"""端到端：样例生成 → 抽取 → 合并 → 消融。"""
import math

from llm_features.batch import run_batch
from llm_features.documents import DocumentConverter
from llm_features.evaluate import FEATURE_SET_NAMES, run_ablation
from llm_features.extractor import LLMFeatureExtractor
from llm_features.merge import load_and_merge, load_jsonl
from llm_features.mock import MockLLMClient
from llm_features.sample_data import write_sample_files, write_source_docs
from llm_features.schema import RiskRecordInput
from llm_features.vlm_client import MockVLMClient


def test_sample_pipeline_end_to_end(tmp_path):
    paths = write_sample_files(tmp_path, n=240, seed=1)

    merged = load_and_merge(paths["tabular"], paths["llm"])
    assert len(merged) == 240
    assert merged["label"].nunique() == 2
    assert merged["llm_feature_available"].mean() > 0.9

    report = run_ablation(merged, model="logreg", synthetic=True)

    assert set(report["feature_sets"]) == set(FEATURE_SET_NAMES)
    for metrics in report["feature_sets"].values():
        assert not any(math.isnan(value) for value in metrics.values())

    assert report["feature_sets"]["llm"]["auc"] > 0.6


def test_sample_generation_is_reproducible(tmp_path):
    a = write_sample_files(tmp_path / "a", n=40, seed=3)
    b = write_sample_files(tmp_path / "b", n=40, seed=3)
    assert a["applications"].read_text(encoding="utf-8") == \
        b["applications"].read_text(encoding="utf-8")


def test_source_path_pipeline_matches_text_pipeline(tmp_path):
    paths = write_sample_files(tmp_path / "data", n=60, seed=11)
    docs = write_source_docs(
        tmp_path / "data" / "source_docs",
        paths["applications"],
        limit=5,
    )

    records = load_jsonl(docs["applications"])
    converter = DocumentConverter(MockVLMClient(), base_dir=docs["base_dir"])
    extractor = LLMFeatureExtractor(MockLLMClient(), retry_delay=0)

    outputs, errors = run_batch(records, extractor, converter=converter)
    assert errors == []
    assert len(outputs) == 5

    text_records = {
        record["application_id"]: record
        for record in load_jsonl(paths["applications"])
    }
    for output in outputs:
        baseline = extractor.extract(
            RiskRecordInput.model_validate(
                text_records[output["application_id"]]
            )
        )
        assert output["features"] == baseline.features
        assert all(
            info["status"] == "ready"
            for info in output["document_conversion"]
        )
        assert all(
            info["cached"] is False
            for info in output["document_conversion"]
        )

    # 第二次运行命中 .md 缓存
    second, _ = run_batch(records, extractor, converter=converter)
    assert all(
        info["cached"] is True
        for output in second
        for info in output["document_conversion"]
    )


def test_sample_contains_both_risk_profiles(tmp_path):
    paths = write_sample_files(tmp_path, n=120, seed=2)
    text = paths["llm"].read_text(encoding="utf-8")
    assert '"llm_feature_available": 1' in text
    assert '"llm_verdict": 1' in text
    assert '"llm_verdict": 0' in text
