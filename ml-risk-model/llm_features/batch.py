"""批量抽取 CLI。

用法::

    python -m llm_features.batch \
        --input data/llm_features/sample_applications.jsonl \
        --output data/llm_features/sample_llm_features.jsonl \
        --cache .cache/llm_features.json --mock
"""
from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple

from llm_features.cache import FileCache
from llm_features.client import DEFAULT_MODEL, OpenAICompatibleClient
from llm_features.documents import DocumentConverter
from llm_features.extractor import LLMFeatureExtractor
from llm_features.ingest import resolve_record
from llm_features.merge import load_jsonl
from llm_features.mock import MockLLMClient
from llm_features.schema import (
    DETERMINISTIC_FEATURE_NAMES,
    RiskRecordInput,
    default_feature_dict,
)
from llm_features.vlm_client import (
    MockVLMClient,
    OpenAICompatibleVLMClient,
)


def write_jsonl(path: str | Path, rows: List[Dict[str, Any]]) -> None:
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    with open(path, "w", encoding="utf-8") as handle:
        for row in rows:
            handle.write(json.dumps(row, ensure_ascii=False) + "\n")


def _empty_deterministic() -> Dict[str, Any]:
    return {name: 0 for name in DETERMINISTIC_FEATURE_NAMES}


def _has_source_paths(records: List[Dict[str, Any]]) -> bool:
    for record in records:
        documents = record.get("documents") or []
        if not isinstance(documents, list):
            continue
        for document in documents:
            if isinstance(document, dict) and document.get("source_path"):
                return True
    return False


def run_batch(
    records: List[Dict[str, Any]],
    extractor: LLMFeatureExtractor,
    limit: Optional[int] = None,
    converter: Optional[DocumentConverter] = None,
) -> Tuple[List[Dict[str, Any]], List[Dict[str, Any]]]:
    """逐条抽取，单条失败不中断。返回 (全部输出, 失败输出)。"""
    outputs: List[Dict[str, Any]] = []
    errors: List[Dict[str, Any]] = []

    selected = records[:limit] if limit is not None else records
    for raw in selected:
        conversion_info: List[Dict[str, Any]] = []
        if converter is not None:
            try:
                raw, conversion_info = resolve_record(raw, converter)
            except Exception as exc:
                item = {
                    "application_id": raw.get("application_id"),
                    "ok": False,
                    "status": "error",
                    "error_code": "DOCUMENT_CONVERSION_FAILED",
                    "error": f"文档转换失败: {exc}",
                    "features": default_feature_dict(available=False),
                    "deterministic_features": _empty_deterministic(),
                    "document_conversion": [],
                    "reasoning": "",
                }
                outputs.append(item)
                errors.append(item)
                continue

        try:
            record = RiskRecordInput.model_validate(raw)
        except Exception as exc:
            item = {
                "application_id": raw.get("application_id"),
                "ok": False,
                "status": "error",
                "error_code": "INVALID_INPUT",
                "error": f"输入校验失败: {exc}",
                "features": default_feature_dict(available=False),
                "deterministic_features": _empty_deterministic(),
                "document_conversion": conversion_info,
                "reasoning": "",
            }
            outputs.append(item)
            errors.append(item)
            continue

        result = extractor.extract(record)
        item = {
            "application_id": result.application_id,
            "ok": result.ok,
            "status": "ok" if result.ok else "error",
            "error_code": None if result.ok else "LLM_EXTRACTION_FAILED",
            "error": result.error,
            "features": result.features,
            "deterministic_features": result.deterministic_features,
            "document_conversion": conversion_info,
            "reasoning": result.reasoning,
            "cached": result.cached,
        }
        outputs.append(item)
        if not result.ok:
            errors.append(item)

    cache = getattr(extractor, "cache", None)
    if cache is not None:
        cache.save()

    return outputs, errors


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="从贷款申请材料中抽取 LLM 风控特征"
    )
    parser.add_argument("--input", required=True, help="输入 JSONL")
    parser.add_argument("--output", required=True, help="输出 JSONL")
    parser.add_argument(
        "--cache",
        default=".cache/llm_features.json",
        help="缓存文件路径",
    )
    parser.add_argument("--no-cache", action="store_true", help="禁用缓存")
    parser.add_argument("--limit", type=int, default=None, help="只处理前 N 条")
    parser.add_argument(
        "--mock",
        action="store_true",
        help="使用确定性 Mock 客户端（文本 LLM 与 VLM 都 mock）",
    )
    parser.add_argument(
        "--model",
        default=os.getenv("LLM_MODEL", DEFAULT_MODEL),
        help="文本 LLM 模型名",
    )
    parser.add_argument(
        "--doc-base-dir",
        default=None,
        help="材料源文件根目录（如 ./uploads），存在 source_path 时必填",
    )
    parser.add_argument(
        "--vlm-model",
        default=None,
        help="视觉模型名（默认读取 VLM_MODEL）",
    )
    parser.add_argument(
        "--force-convert",
        action="store_true",
        help="忽略 .md 缓存，强制重新转换",
    )
    parser.add_argument(
        "--retry-delay",
        type=float,
        default=0.5,
        help="重试退避基数（秒），测试时设为 0",
    )
    return parser


def main(argv: Optional[List[str]] = None) -> int:
    args = build_parser().parse_args(argv)

    try:
        records = load_jsonl(args.input)
    except OSError as exc:
        print(f"读取输入失败: {exc}", file=sys.stderr)
        return 1

    if args.mock:
        client = MockLLMClient()
    else:
        client = OpenAICompatibleClient(model=args.model)

    converter = None
    if _has_source_paths(records):
        if not args.doc_base_dir:
            print(
                "输入包含 source_path，必须指定 --doc-base-dir（例如 ./uploads）",
                file=sys.stderr,
            )
            return 1
        vlm = (
            MockVLMClient()
            if args.mock
            else OpenAICompatibleVLMClient(model=args.vlm_model)
        )
        converter = DocumentConverter(
            vlm,
            base_dir=args.doc_base_dir,
            force=args.force_convert,
        )

    cache = None if args.no_cache else FileCache(args.cache)
    extractor = LLMFeatureExtractor(
        client,
        cache=cache,
        model=args.model,
        retry_delay=args.retry_delay,
    )

    outputs, errors = run_batch(
        records, extractor, limit=args.limit, converter=converter
    )
    if converter is not None:
        failed_conversions = sum(
            1
            for row in outputs
            for info in row.get("document_conversion", [])
            if info.get("status") not in ("ready", "provided")
        )
        if failed_conversions:
            print(f"材料转换未完成: {failed_conversions} 份")
    write_jsonl(args.output, outputs)

    if errors:
        error_path = f"{args.output}.errors.jsonl"
        write_jsonl(error_path, errors)

    success = sum(1 for row in outputs if row["ok"])
    print(
        f"处理 {len(outputs)} 条，成功 {success} 条，失败 {len(errors)} 条"
    )
    print(f"输出: {args.output}")
    if errors:
        print(f"失败明细: {args.output}.errors.jsonl")
    return 0


if __name__ == "__main__":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass
    raise SystemExit(main())
