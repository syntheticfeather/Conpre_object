"""单条申请演示：输入材料 → LLM 抽取 → 合并后的特征行。

用法::

    cd ml-risk-model
    # 取第一条 label=1（高风险）的样例，使用 Mock 客户端
    PYTHONIOENCODING=utf-8 python scripts/demo_single_application.py --label 1

    # 取指定 application_id，并打印完整 prompt
    PYTHONIOENCODING=utf-8 python scripts/demo_single_application.py \
        --application-id 7 --show-prompt

    # 使用真实 LLM（需要 LLM_API_KEY）
    PYTHONIOENCODING=utf-8 python scripts/demo_single_application.py --real
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parents[1]
if str(BASE_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_DIR))

from llm_features.client import OpenAICompatibleClient  # noqa: E402
from llm_features.extractor import LLMFeatureExtractor  # noqa: E402
from llm_features.merge import load_records, merge_records  # noqa: E402
from llm_features.mock import MockLLMClient  # noqa: E402
from llm_features.prompt import build_user_prompt  # noqa: E402
from llm_features.schema import RiskRecordInput  # noqa: E402

DEFAULT_APPLICATIONS = BASE_DIR / "data" / "llm_features" / "sample_applications.jsonl"
DEFAULT_TABULAR = BASE_DIR / "data" / "llm_features" / "sample_tabular.jsonl"


def _pick(records, application_id, label):
    if application_id is not None:
        for record in records:
            if record["application_id"] == application_id:
                return record
        raise SystemExit(f"未找到 application_id={application_id}")
    if label is not None:
        for record in records:
            if record.get("label") == label:
                return record
        raise SystemExit(f"未找到 label={label} 的申请")
    return records[0]


def main() -> int:
    parser = argparse.ArgumentParser(description="单条申请 LLM 特征抽取演示")
    parser.add_argument("--input", default=str(DEFAULT_APPLICATIONS))
    parser.add_argument("--tabular", default=str(DEFAULT_TABULAR))
    parser.add_argument("--application-id", type=int, default=None)
    parser.add_argument("--label", type=int, choices=[0, 1], default=None)
    parser.add_argument("--real", action="store_true", help="使用真实 LLM")
    parser.add_argument("--show-prompt", action="store_true")
    args = parser.parse_args()

    records = load_records(args.input)
    raw = _pick(records, args.application_id, args.label)
    record = RiskRecordInput.model_validate(raw)

    print("=" * 72)
    print(f"申请 application_id={record.application_id}  label={record.label}")
    print("=" * 72)
    print("申请上下文:")
    print(json.dumps(record.application.model_dump(), ensure_ascii=False, indent=2))
    print("\n材料:")
    for doc in record.documents:
        print(f"  [{doc.type}] {doc.text}")
    print(f"\n申请备注: {record.application_note}")

    prompt = build_user_prompt(record)
    if args.show_prompt:
        print("\n" + "-" * 72)
        print("发送给 LLM 的 user prompt:")
        print("-" * 72)
        print(prompt)

    client = OpenAICompatibleClient() if args.real else MockLLMClient()
    extractor = LLMFeatureExtractor(client, retry_delay=0)
    result = extractor.extract(record)

    print("\n" + "-" * 72)
    print("LLM 抽取结果 (ok=%s, cached=%s)" % (result.ok, result.cached))
    print("-" * 72)
    print("reasoning:", result.reasoning or "(无)")
    print("G 组特征:")
    print(json.dumps(result.features, ensure_ascii=False, indent=2))
    print("H 组特征:")
    print(json.dumps(result.deterministic_features, ensure_ascii=False, indent=2))

    tabular = [row for row in load_records(args.tabular)
               if row["application_id"] == record.application_id]
    merged = merge_records(tabular, [{
        "application_id": record.application_id,
        "features": result.features,
        "deterministic_features": result.deterministic_features,
    }])
    print("\n" + "-" * 72)
    print("合并后进入模型的一行特征:")
    print("-" * 72)
    print(json.dumps(merged.iloc[0].to_dict(), ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass
    raise SystemExit(main())
