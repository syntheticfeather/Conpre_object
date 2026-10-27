"""表格特征 + LLM 特征的三组消融训练。

用法::

    cd ml-risk-model
    PYTHONIOENCODING=utf-8 python train_with_llm_features.py

默认读取 data/llm_features 下的样例数据。真实使用时把
--tabular / --llm 指向 ml_training_log 导出文件和批量抽取结果即可。
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path
from typing import List, Optional

from llm_features.evaluate import (
    format_report,
    infer_tabular_features,
    run_ablation,
)
from llm_features.merge import load_and_merge

BASE_DIR = Path(__file__).resolve().parent
DEFAULT_TABULAR = BASE_DIR / "data" / "llm_features" / "sample_tabular.jsonl"
DEFAULT_LLM = BASE_DIR / "data" / "llm_features" / "sample_llm_features.jsonl"
DEFAULT_OUT = BASE_DIR / "model" / "llm_feature_ablation_report.json"


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="LLM 风控特征消融实验")
    parser.add_argument("--tabular", default=str(DEFAULT_TABULAR))
    parser.add_argument("--llm", default=str(DEFAULT_LLM))
    parser.add_argument("--out", default=str(DEFAULT_OUT))
    parser.add_argument(
        "--model",
        default="auto",
        choices=["auto", "xgb", "logreg"],
        help="auto: 有 xgboost 用 xgboost，否则回退逻辑回归",
    )
    parser.add_argument("--test-size", type=float, default=0.25)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument(
        "--synthetic",
        action="store_true",
        help="标记数据为合成数据（文件名含 sample 时自动开启）",
    )
    return parser


def main(argv: Optional[List[str]] = None) -> int:
    args = build_parser().parse_args(argv)

    merged = load_and_merge(args.tabular, args.llm)
    tabular_features = infer_tabular_features(merged)

    synthetic = args.synthetic or "sample" in Path(args.tabular).name

    report = run_ablation(
        merged,
        tabular_features=tabular_features,
        model=args.model,
        test_size=args.test_size,
        seed=args.seed,
        synthetic=synthetic,
    )

    print(format_report(report))

    out_path = Path(args.out)
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(
        json.dumps(report, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    print(f"报告已写入: {out_path}")
    return 0


if __name__ == "__main__":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass
    raise SystemExit(main())
