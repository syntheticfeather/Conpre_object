"""生成合成贷款申请样例。

用途是验证流水线可以端到端运行，**不代表真实业务效果**。
材料文本中的风险维度与潜在违约风险相关，Mock 客户端据此抽取特征。
"""
from __future__ import annotations

import json
from pathlib import Path
from typing import Any, Dict, List, Union

import numpy as np
from PIL import Image, ImageDraw

from llm_features.batch import run_batch, write_jsonl
from llm_features.extractor import LLMFeatureExtractor
from llm_features.merge import load_jsonl
from llm_features.mock import MockLLMClient

TABULAR_FEATURES = [
    "age",
    "monthly_income",
    "dti_ratio",
    "active_loan_count",
    "has_mortgage",
    "max_overdue_days_90",
    "overdue_30_59_count",
    "overdue_60_89_count",
    "credit_card_usage_pct",
    "info_completeness",
    "application_hour",
    "is_late_night_apply",
    "total_loan_count",
]


def _clip(value: float, low: float, high: float) -> float:
    return float(min(max(value, low), high))


def _risk_profile(risk: float, rng) -> Dict[str, bool]:
    """按潜在风险概率生成各维度的材料风险标记。"""
    def flag(base: float, weight: float, cap: float = 0.9) -> bool:
        return bool(rng.random() < min(cap, base + weight * risk))

    return {
        "income_inconsistent": flag(0.10, 0.80),
        "employment_risky": flag(0.08, 0.80),
        "multi_lender": flag(0.08, 0.80),
        "suspicious_doc": flag(0.04, 0.65),
        "negative_record": flag(0.04, 0.70),
        "debt_pressure": flag(0.05, 0.75),
        "material_mismatch": flag(0.03, 0.55),
    }


def _documents(
    profile: Dict[str, bool],
    has_mortgage: int,
    rng,
) -> List[Dict[str, str]]:
    docs: List[Dict[str, str]] = []

    if profile["employment_risky"]:
        docs.append({
            "type": "employment_cert",
            "text": "在职证明：无固定工作，临时工，收入不稳定。",
        })
    else:
        docs.append({
            "type": "employment_cert",
            "text": "在职证明：就职于某科技公司，在职三年，收入稳定。",
        })

    if profile["income_inconsistent"]:
        docs.append({
            "type": "salary_cert",
            "text": "收入证明与申报收入不一致，存在虚报。",
        })
    else:
        docs.append({
            "type": "salary_cert",
            "text": "工资流水与申报收入一致，月收入稳定。",
        })

    credit_parts = []
    if profile["multi_lender"]:
        credit_parts.append("近3月频繁查询，存在多头借贷")
    if profile["negative_record"]:
        credit_parts.append("历史有逾期记录")
    if credit_parts:
        docs.append({
            "type": "credit_report",
            "text": "征信报告：" + "，".join(credit_parts) + "。",
        })
    else:
        docs.append({
            "type": "credit_report",
            "text": "征信报告：无逾期，查询次数正常。",
        })

    if profile["material_mismatch"]:
        docs.append({
            "type": "bank_statement",
            "text": "银行流水与收入证明信息矛盾，前后不符。",
        })

    if profile["suspicious_doc"]:
        docs.append({
            "type": "identity_cert",
            "text": "身份证复印件模糊无法辨认。",
        })

    if rng.random() < 0.85:
        docs.append({
            "type": "social_security",
            "text": "社保连续缴纳 24 个月。",
        })
    else:
        docs.append({
            "type": "social_security",
            "text": "未提供社保证明。",
        })

    if has_mortgage:
        docs.append({
            "type": "property_cert",
            "text": "房产证明：名下有一套房产。",
        })

    if rng.random() < 0.3:
        docs.append({
            "type": "car_cert",
            "text": "车辆登记证：名下有一辆车。",
        })

    return docs


def generate_records(n: int = 1200, seed: int = 42) -> List[Dict[str, Any]]:
    """生成 n 条带潜在风险的合成申请。"""
    rng = np.random.default_rng(seed)
    records: List[Dict[str, Any]] = []

    for index in range(n):
        age = int(_clip(rng.normal(35, 8), 22, 60))
        income = int(_clip(np.exp(rng.normal(np.log(8000), 0.45)), 3000, 50000))
        dti = _clip(rng.normal(35, 15), 5, 95)
        active_loans = int(rng.poisson(1.2))
        has_mortgage = int(rng.random() < 0.35)
        overdue_90 = int(rng.poisson(0.2))
        overdue_30 = int(rng.poisson(0.35))
        overdue_60 = int(rng.poisson(0.15))
        credit_usage = _clip(rng.beta(2, 5) * 100, 0, 100)
        completeness = int(_clip(rng.normal(80, 12), 40, 100))
        hour = int(rng.integers(0, 24))

        risk = float(rng.beta(2, 5))
        profile = _risk_profile(risk, rng)
        bad_count = sum(profile.values())

        logit = (
            -2.2
            + 0.60 * bad_count
            + 0.5 * (overdue_90 > 0)
            + 0.010 * (dti - 35)
            - 0.25 * np.log(income / 8000)
            + rng.normal(0, 0.35)
        )
        label = int(rng.random() < 1.0 / (1.0 + np.exp(-logit)))

        note = (
            "客户表示近期资金紧张，负债较高。"
            if profile["debt_pressure"]
            else "正常申请，用途为装修。"
        )

        record = {
            "application_id": index + 1,
            "user_id": 1000 + index,
            "label": label,
            "application": {
                "amount": float(rng.choice([10000, 20000, 50000, 80000, 120000])),
                "term": int(rng.choice([6, 12, 24, 36])),
                "income": income,
            },
            "documents": _documents(profile, has_mortgage, rng),
            "application_note": note,
            "age": age,
            "monthly_income": income,
            "dti_ratio": round(float(dti), 2),
            "active_loan_count": active_loans,
            "has_mortgage": has_mortgage,
            "max_overdue_days_90": overdue_90,
            "overdue_30_59_count": overdue_30,
            "overdue_60_89_count": overdue_60,
            "credit_card_usage_pct": round(float(credit_usage), 2),
            "info_completeness": completeness,
            "application_hour": hour,
            "is_late_night_apply": int(hour <= 5),
            "total_loan_count": int(rng.poisson(1.5)),
        }
        records.append(record)

    return records


def write_sample_files(
    out_dir: Union[str, Path],
    n: int = 1200,
    seed: int = 42,
) -> Dict[str, Path]:
    """生成并写出三份样例文件，返回路径字典。"""
    out = Path(out_dir)
    out.mkdir(parents=True, exist_ok=True)

    records = generate_records(n=n, seed=seed)

    applications_path = out / "sample_applications.jsonl"
    tabular_path = out / "sample_tabular.jsonl"
    llm_path = out / "sample_llm_features.jsonl"

    write_jsonl(applications_path, records)

    tabular_rows = [
        {
            "application_id": record["application_id"],
            "label": record["label"],
            **{name: record[name] for name in TABULAR_FEATURES},
        }
        for record in records
    ]
    write_jsonl(tabular_path, tabular_rows)

    extractor = LLMFeatureExtractor(MockLLMClient(), retry_delay=0)
    outputs, _ = run_batch(records, extractor)
    write_jsonl(llm_path, outputs)

    return {
        "applications": applications_path,
        "tabular": tabular_path,
        "llm": llm_path,
    }


def write_source_docs(
    out_dir: Union[str, Path],
    applications_path: Union[str, Path],
    application_ids: List[int] | None = None,
    limit: int = 10,
) -> Dict[str, Any]:
    """为样例申请生成假的源文件（PNG）和 Mock VLM 夹具。

    图片内容只是占位，真实转录文本写在 ``<文件名>.mock.md`` 里，
    供 MockVLMClient 读取，从而在没有真实 VLM 时跑通懒转换链路。
    """
    out = Path(out_dir)
    out.mkdir(parents=True, exist_ok=True)

    records = load_jsonl(applications_path)
    if application_ids is not None:
        wanted = set(application_ids)
        selected = [r for r in records if r["application_id"] in wanted]
    else:
        selected = records[:limit]

    manifest_rows: List[Dict[str, Any]] = []
    for record in selected:
        app_dir = out / str(record["application_id"])
        app_dir.mkdir(parents=True, exist_ok=True)

        documents = []
        for index, document in enumerate(record.get("documents") or []):
            filename = f"{index:02d}_{document['type']}.png"
            image_path = app_dir / filename
            image = Image.new("RGB", (640, 480), "white")
            ImageDraw.Draw(image).text(
                (20, 20),
                f"application {record['application_id']} {document['type']}",
                fill="black",
            )
            image.save(image_path)
            Path(str(image_path) + ".mock.md").write_text(
                document.get("text", ""), encoding="utf-8"
            )
            documents.append({
                "type": document["type"],
                "source_path": f"{record['application_id']}/{filename}",
            })

        item = {key: value for key, value in record.items() if key != "documents"}
        item["documents"] = documents
        manifest_rows.append(item)

    manifest_path = out.parent / "sample_applications_with_paths.jsonl"
    write_jsonl(manifest_path, manifest_rows)

    return {
        "base_dir": out,
        "applications": manifest_path,
        "count": len(manifest_rows),
    }


if __name__ == "__main__":
    paths = write_sample_files(
        Path(__file__).resolve().parents[1] / "data" / "llm_features"
    )
    print(json.dumps(
        {key: str(value) for key, value in paths.items()},
        ensure_ascii=False,
        indent=2,
    ))
