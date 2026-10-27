"""结构化特征与 LLM 特征的合并。

约定：
- 表格文件是事实来源，标签只从表格读取；
- LLM 文件缺失或失败时填默认值，并用 ``llm_feature_available=0`` 标记；
- 合并结果中不保留 LLM 输出里的任何 label 字段。
"""
from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Dict, Iterable, List

import pandas as pd

from llm_features.schema import (
    DETERMINISTIC_FEATURE_NAMES,
    LLM_FEATURE_NAMES,
    default_feature_dict,
)


def load_jsonl(path: str | Path) -> List[Dict[str, Any]]:
    """读取 JSONL，忽略空行。"""
    records = []
    with open(path, "r", encoding="utf-8") as handle:
        for line in handle:
            line = line.strip()
            if line:
                records.append(json.loads(line))
    return records


def load_records(path: str | Path) -> List[Dict[str, Any]]:
    """按扩展名读取表格数据：支持 .csv 与 .jsonl。"""
    path = Path(path)
    if path.suffix.lower() == ".csv":
        return pd.read_csv(path).to_dict("records")
    return load_jsonl(path)


def _flatten_llm_record(record: Dict[str, Any]) -> Dict[str, Any]:
    row: Dict[str, Any] = {"application_id": record.get("application_id")}
    row.update(record.get("features") or {})
    row.update(record.get("deterministic_features") or {})
    return row


def merge_records(
    tabular_records: Iterable[Dict[str, Any]],
    llm_records: Iterable[Dict[str, Any]],
) -> pd.DataFrame:
    """以 application_id 左连接表格特征与 LLM 特征。"""
    tabular = pd.DataFrame(list(tabular_records))
    if tabular.empty:
        raise ValueError("表格特征为空，无法合并")

    if "application_id" not in tabular.columns:
        raise ValueError("表格特征缺少 application_id 列")

    # 表格里如果已经带了 G/H 列，以本次 LLM 结果为准
    drop_cols = [
        col for col in LLM_FEATURE_NAMES + DETERMINISTIC_FEATURE_NAMES
        if col in tabular.columns
    ]
    if drop_cols:
        tabular = tabular.drop(columns=drop_cols)

    rows = [_flatten_llm_record(record) for record in llm_records]
    llm = pd.DataFrame(rows)
    if llm.empty:
        llm = pd.DataFrame(
            columns=["application_id"] + LLM_FEATURE_NAMES + DETERMINISTIC_FEATURE_NAMES
        )

    merged = tabular.merge(llm, on="application_id", how="left")

    unavailable = default_feature_dict(available=False)
    for name in LLM_FEATURE_NAMES:
        if name not in merged.columns:
            merged[name] = unavailable[name]
        merged[name] = merged[name].fillna(unavailable[name])

    for name in DETERMINISTIC_FEATURE_NAMES:
        if name not in merged.columns:
            merged[name] = 0
        merged[name] = merged[name].fillna(0)

    return merged


def load_and_merge(
    tabular_path: str | Path,
    llm_path: str | Path,
) -> pd.DataFrame:
    """从文件加载并合并。"""
    return merge_records(load_records(tabular_path), load_records(llm_path))
