"""消融评估测试。"""
import numpy as np
import pandas as pd
import pytest

from llm_features.evaluate import (
    FEATURE_SET_NAMES,
    format_report,
    infer_tabular_features,
    run_ablation,
)
from llm_features.schema import (
    DETERMINISTIC_FEATURE_NAMES,
    LLM_FEATURE_NAMES,
    default_feature_dict,
)


def _dataset(n=300, seed=7) -> pd.DataFrame:
    rng = np.random.default_rng(seed)
    label = rng.integers(0, 2, n)
    rows = []
    for i in range(n):
        row = {
            "application_id": i,
            "label": int(label[i]),
            "noise_feature": float(rng.normal()),
        }
        features = default_feature_dict(available=True)
        # LLM 特征完美携带标签信号
        features["llm_verdict"] = int(label[i])
        features["llm_confidence"] = 0.9 if label[i] else 0.2
        features["doc_negative_public_record"] = int(label[i])
        row.update(features)
        for name in DETERMINISTIC_FEATURE_NAMES:
            row[name] = float(rng.integers(0, 10))
        rows.append(row)
    return pd.DataFrame(rows)


def test_infer_tabular_features_excludes_labels_and_llm_columns():
    df = _dataset(10)
    cols = infer_tabular_features(df)
    assert "noise_feature" in cols
    assert "application_id" not in cols
    assert "label" not in cols
    assert not set(cols).intersection(LLM_FEATURE_NAMES)
    assert not set(cols).intersection(DETERMINISTIC_FEATURE_NAMES)


def test_run_ablation_returns_three_feature_sets():
    report = run_ablation(_dataset(), model="logreg", synthetic=True)
    assert set(report["feature_sets"]) == set(FEATURE_SET_NAMES)
    assert report["synthetic"] is True
    assert report["n_samples"] == 300


def test_metrics_are_in_valid_ranges():
    report = run_ablation(_dataset(), model="logreg")
    for metrics in report["feature_sets"].values():
        assert 0.0 <= metrics["auc"] <= 1.0
        assert 0.0 <= metrics["brier"] <= 1.0
        assert 0.0 <= metrics["ks"] <= 1.0
        assert 0.0 <= metrics["f1"] <= 1.0
        assert 0.0 <= metrics["precision"] <= 1.0
        assert 0.0 <= metrics["recall"] <= 1.0


def test_llm_features_improve_auc_when_signal_exists():
    report = run_ablation(_dataset(), model="logreg")
    baseline = report["feature_sets"]["baseline"]["auc"]
    llm = report["feature_sets"]["llm"]["auc"]
    assert llm > baseline + 0.1


def test_full_set_has_llm_and_deterministic_features():
    report = run_ablation(_dataset(), model="logreg")
    baseline_n = report["feature_sets"]["baseline"]["n_features"]
    llm_n = report["feature_sets"]["llm"]["n_features"]
    full_n = report["feature_sets"]["full"]["n_features"]
    assert llm_n == baseline_n + len(LLM_FEATURE_NAMES)
    assert full_n == llm_n + len(DETERMINISTIC_FEATURE_NAMES)


def test_model_name_is_reported():
    report = run_ablation(_dataset(), model="logreg")
    assert report["model"] == "LogisticRegression"


def test_single_class_label_is_rejected():
    df = _dataset(20)
    df["label"] = 0
    with pytest.raises(ValueError, match="两个类别"):
        run_ablation(df, model="logreg")


def test_format_report_contains_all_sets():
    text = format_report(run_ablation(_dataset(), model="logreg", synthetic=True))
    for name in FEATURE_SET_NAMES:
        assert name in text
    assert "AUC" in text
