"""把申请记录里的源文件路径解析为文本。

输入既可以直接带 ``documents[].text``，也可以带
``documents[].source_path``；后者走懒转换，结果写回同名 ``.md``。
"""
from __future__ import annotations

from typing import Any, Dict, List, Tuple

from llm_features.documents import DocumentConverter


def resolve_record(
    raw: Dict[str, Any],
    converter: DocumentConverter,
) -> Tuple[Dict[str, Any], List[Dict[str, Any]]]:
    """返回 (解析后的记录, 每份材料的转换信息)。不修改原记录。"""
    resolved_documents: List[Dict[str, Any]] = []
    conversion_info: List[Dict[str, Any]] = []

    for document in raw.get("documents") or []:
        doc_type = document.get("type", "unknown")
        text = document.get("text")

        if text:
            resolved_documents.append({"type": doc_type, "text": str(text)})
            conversion_info.append({
                "type": doc_type,
                "status": "provided",
                "cached": False,
                "md_path": None,
                "error": None,
            })
            continue

        source_path = document.get("source_path")
        if not source_path:
            resolved_documents.append({"type": doc_type, "text": ""})
            conversion_info.append({
                "type": doc_type,
                "status": "missing_source",
                "cached": False,
                "md_path": None,
                "error": "材料既没有 text，也没有 source_path",
            })
            continue

        result = converter.resolve(source_path)
        resolved_documents.append({"type": doc_type, "text": result.text})
        conversion_info.append({
            "type": doc_type,
            "status": result.status,
            "cached": result.cached,
            "md_path": str(result.md_path) if result.md_path else None,
            "error": result.error,
        })

    resolved = dict(raw)
    resolved["documents"] = resolved_documents
    resolved["document_conversion"] = conversion_info
    return resolved, conversion_info


def resolve_records(
    records: List[Dict[str, Any]],
    converter: DocumentConverter,
) -> Tuple[List[Dict[str, Any]], List[Dict[str, Any]]]:
    """批量解析，返回 (记录列表, 全部转换信息)。"""
    resolved = []
    infos = []
    for record in records:
        item, item_infos = resolve_record(record, converter)
        resolved.append(item)
        infos.extend(item_infos)
    return resolved, infos
