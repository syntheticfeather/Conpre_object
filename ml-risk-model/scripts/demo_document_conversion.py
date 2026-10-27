"""源文件 → 同名 Markdown 懒转换演示。

用法::

    cd ml-risk-model
    PYTHONIOENCODING=utf-8 python scripts/generate_source_docs.py --limit 5
    PYTHONIOENCODING=utf-8 python scripts/demo_document_conversion.py --application-id 93
"""
from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parents[1]
if str(BASE_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_DIR))

from llm_features.documents import DocumentConverter  # noqa: E402
from llm_features.extractor import LLMFeatureExtractor  # noqa: E402
from llm_features.ingest import resolve_record  # noqa: E402
from llm_features.merge import load_jsonl  # noqa: E402
from llm_features.mock import MockLLMClient  # noqa: E402
from llm_features.schema import RiskRecordInput  # noqa: E402
from llm_features.vlm_client import (  # noqa: E402
    MockVLMClient,
    OpenAICompatibleVLMClient,
)

DEFAULT_MANIFEST = BASE_DIR / "data" / "llm_features" / "sample_applications_with_paths.jsonl"
DEFAULT_BASE_DIR = BASE_DIR / "data" / "llm_features" / "source_docs"


def main() -> int:
    parser = argparse.ArgumentParser(description="源文件懒转换演示")
    parser.add_argument("--manifest", default=str(DEFAULT_MANIFEST))
    parser.add_argument("--base-dir", default=str(DEFAULT_BASE_DIR))
    parser.add_argument("--application-id", type=int, default=None)
    parser.add_argument("--force", action="store_true")
    parser.add_argument("--real", action="store_true", help="使用真实 VLM")
    parser.add_argument("--vlm-model", default=None)
    args = parser.parse_args()

    records = load_jsonl(args.manifest)
    if args.application_id is not None:
        records = [
            record for record in records
            if record["application_id"] == args.application_id
        ]
    if not records:
        print("没有匹配的申请，请先运行 scripts/generate_source_docs.py")
        return 1

    record = records[0]
    base_dir = Path(args.base_dir)
    vlm = (
        OpenAICompatibleVLMClient(model=args.vlm_model)
        if args.real
        else MockVLMClient()
    )
    converter = DocumentConverter(vlm, base_dir=base_dir, force=args.force)

    print("=" * 72)
    print(f"申请 application_id={record['application_id']}")
    print(f"VLM: {vlm.model} ({'真实调用' if args.real else 'Mock'})")
    print("=" * 72)

    for document in record["documents"]:
        source = base_dir / document["source_path"]
        print(f"\n[{document['type']}]")
        print(f"  源文件: {source}  ({source.stat().st_size} bytes)")

        result = converter.resolve(source, force=args.force)

        print(f"  状态: {result.status}   命中缓存: {result.cached}   mode: {result.mode}")
        print(f"  Markdown: {result.md_path}")
        print(f"  源文件 hash: {result.source_sha256[:16]}...")
        if result.text:
            preview = result.text.replace("\n", " ")
            print(f"  文本预览: {preview[:120]}")
        else:
            print(f"  错误: {result.error}")

    resolved, infos = resolve_record(record, converter)
    extractor = LLMFeatureExtractor(MockLLMClient(), retry_delay=0)
    features = extractor.extract(RiskRecordInput.model_validate(resolved))

    print("\n" + "-" * 72)
    print("转换后抽取的 G 组特征")
    print("-" * 72)
    print(json.dumps(features.features, ensure_ascii=False, indent=2))
    print("\n第二次 resolve 应全部命中缓存:")
    for document in record["documents"]:
        again = converter.resolve(base_dir / document["source_path"])
        print(f"  {document['type']}: cached={again.cached}")
    return 0


if __name__ == "__main__":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass
    raise SystemExit(main())
