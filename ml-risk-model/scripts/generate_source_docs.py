"""为样例申请生成源文件（PNG）与 Mock VLM 夹具。

用法::

    cd ml-risk-model
    PYTHONIOENCODING=utf-8 python scripts/generate_source_docs.py --limit 10
"""
from __future__ import annotations

import argparse
import sys
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parents[1]
if str(BASE_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_DIR))

from llm_features.sample_data import write_source_docs  # noqa: E402


def main() -> int:
    parser = argparse.ArgumentParser(description="生成样例材料源文件")
    parser.add_argument(
        "--applications",
        default=str(BASE_DIR / "data" / "llm_features" / "sample_applications.jsonl"),
    )
    parser.add_argument(
        "--out",
        default=str(BASE_DIR / "data" / "llm_features" / "source_docs"),
    )
    parser.add_argument("--limit", type=int, default=10)
    args = parser.parse_args()

    result = write_source_docs(args.out, args.applications, limit=args.limit)
    print(f"申请数: {result['count']}")
    print(f"源文件目录: {result['base_dir']}")
    print(f"带路径的申请清单: {result['applications']}")
    return 0


if __name__ == "__main__":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass
    raise SystemExit(main())
