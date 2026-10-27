"""生成 LLM 风控特征流水线的合成样例。

用法::

    cd ml-risk-model
    PYTHONIOENCODING=utf-8 python scripts/generate_sample_data.py
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parents[1]
if str(BASE_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_DIR))

from llm_features.sample_data import write_sample_files  # noqa: E402


def main() -> int:
    parser = argparse.ArgumentParser(description="生成合成申请样例")
    parser.add_argument("--n", type=int, default=1200)
    parser.add_argument("--seed", type=int, default=42)
    parser.add_argument(
        "--out",
        default=str(BASE_DIR / "data" / "llm_features"),
    )
    args = parser.parse_args()

    paths = write_sample_files(args.out, n=args.n, seed=args.seed)
    for name, path in paths.items():
        print(f"{name}: {path}")
    return 0


if __name__ == "__main__":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass
    raise SystemExit(main())
