"""三组嵌套消融：baseline / +LLM / +LLM+确定性特征。"""
from __future__ import annotations

from typing import Any, Dict, List, Optional

import numpy as np
import pandas as pd
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (
    brier_score_loss,
    f1_score,
    precision_score,
    recall_score,
    roc_auc_score,
    roc_curve,
)
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler

from llm_features.schema import (
    DETERMINISTIC_FEATURE_NAMES,
    LLM_FEATURE_NAMES,
)

FEATURE_SET_NAMES = ["baseline", "llm", "full"]

_EXCLUDED_FROM_TABULAR = {
    "application_id",
    "user_id",
    "label",
    "is_default",
}


def infer_tabular_features(df: pd.DataFrame) -> List[str]:
    """推断表格特征列：排除标识、标签与 LLM 特征。"""
    excluded = _EXCLUDED_FROM_TABULAR.union(LLM_FEATURE_NAMES).union(
        DETERMINISTIC_FEATURE_NAMES
    )
    numeric = df.select_dtypes(include=[np.number]).columns.tolist()
    return [col for col in numeric if col not in excluded]


def _build_estimator(kind: str, y_train: np.ndarray, seed: int):
    if kind not in ("auto", "xgb", "logreg"):
        raise ValueError(f"未知模型类型: {kind}")

    if kind in ("auto", "xgb"):
        try:
            from xgboost import XGBClassifier
        except ImportError:
            if kind == "xgb":
                raise
            kind = "logreg"
        else:
            positives = int(np.sum(y_train == 1))
            negatives = int(np.sum(y_train == 0))
            scale = negatives / positives if positives > 0 else 1.0
            return XGBClassifier(
                n_estimators=200,
                max_depth=3,
                learning_rate=0.05,
                subsample=0.8,
                colsample_bytree=0.8,
                eval_metric="logloss",
                scale_pos_weight=scale,
                random_state=seed,
                n_jobs=2,
                verbosity=0,
            ), "XGBoost"

    return Pipeline([
        ("scaler", StandardScaler()),
        ("clf", LogisticRegression(max_iter=1000, random_state=seed)),
    ]), "LogisticRegression"


def _metrics(y_true: np.ndarray, prob: np.ndarray) -> Dict[str, float]:
    pred = (prob >= 0.5).astype(int)
    fpr, tpr, _ = roc_curve(y_true, prob)
    return {
        "auc": float(roc_auc_score(y_true, prob)),
        "ks": float(np.max(tpr - fpr)),
        "brier": float(brier_score_loss(y_true, prob)),
        "f1": float(f1_score(y_true, pred, zero_division=0)),
        "precision": float(precision_score(y_true, pred, zero_division=0)),
        "recall": float(recall_score(y_true, pred, zero_division=0)),
    }


def run_ablation(
    df: pd.DataFrame,
    tabular_features: Optional[List[str]] = None,
    label_col: str = "label",
    model: str = "auto",
    test_size: float = 0.25,
    seed: int = 42,
    synthetic: bool = False,
) -> Dict[str, Any]:
    """运行三组嵌套实验并返回指标报告。"""
    if label_col not in df.columns:
        raise ValueError(f"缺少标签列: {label_col}")

    y = df[label_col].astype(int).to_numpy()
    if len(np.unique(y)) < 2:
        raise ValueError("标签只有一个类别，需要两个类别才能训练")

    if tabular_features is None:
        tabular_features = infer_tabular_features(df)

    missing = [col for col in tabular_features if col not in df.columns]
    if missing:
        raise ValueError(f"表格特征列不存在: {missing}")

    feature_sets = {
        "baseline": list(tabular_features),
        "llm": list(tabular_features) + LLM_FEATURE_NAMES,
        "full": list(tabular_features) + LLM_FEATURE_NAMES + DETERMINISTIC_FEATURE_NAMES,
    }

    idx_train, idx_test = train_test_split(
        np.arange(len(df)),
        test_size=test_size,
        random_state=seed,
        stratify=y,
    )
    y_train, y_test = y[idx_train], y[idx_test]

    results: Dict[str, Dict[str, float]] = {}
    model_name = ""
    for name in FEATURE_SET_NAMES:
        columns = feature_sets[name]
        X = df[columns].astype(float).to_numpy()
        estimator, model_name = _build_estimator(model, y_train, seed)
        estimator.fit(X[idx_train], y_train)
        prob = estimator.predict_proba(X[idx_test])[:, 1]

        metrics = _metrics(y_test, prob)
        metrics["n_features"] = len(columns)
        results[name] = metrics

    return {
        "synthetic": synthetic,
        "model": model_name,
        "n_samples": int(len(df)),
        "test_size": int(len(idx_test)),
        "positive_rate": float(y.mean()),
        "feature_sets": results,
    }


def format_report(report: Dict[str, Any]) -> str:
    """把报告格式化为终端表格。"""
    lines = [
        "=" * 72,
        "  风控 LLM 特征消融实验",
        "=" * 72,
        f"  模型: {report['model']}    样本: {report['n_samples']}    "
        f"测试集: {report['test_size']}    违约率: {report['positive_rate']:.2%}",
        f"  数据来源: {'合成数据（仅验证流水线）' if report.get('synthetic') else '用户数据'}",
        "-" * 72,
        f"  {'特征组':<10}{'特征数':>8}{'AUC':>9}{'KS':>9}{'Brier':>9}"
        f"{'F1':>9}{'Prec':>9}{'Recall':>9}",
        "-" * 72,
    ]
    for name in FEATURE_SET_NAMES:
        m = report["feature_sets"][name]
        lines.append(
            f"  {name:<10}{m['n_features']:>8}{m['auc']:>9.4f}{m['ks']:>9.4f}"
            f"{m['brier']:>9.4f}{m['f1']:>9.4f}{m['precision']:>9.4f}"
            f"{m['recall']:>9.4f}"
        )
    lines.append("=" * 72)
    return "\n".join(lines)
