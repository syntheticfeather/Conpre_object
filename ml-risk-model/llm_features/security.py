"""文本侧 PII 脱敏。

在把材料文本发给外部 LLM 之前，先替换身份证、手机号、银行卡号。
注意：图片本身在 VLM 阶段已经上传，文本脱敏无法撤销这一点；
生产环境还需要本地化模型、图片脱敏或供应商合规评估。
"""
from __future__ import annotations

import re
from dataclasses import dataclass
from typing import Dict

_ID_CARD = re.compile(r"(?<!\d)(?:\d{17}[\dXx]|\d{15})(?!\d)")
_BANK_CARD = re.compile(r"(?<!\d)\d{16,19}(?!\d)")
_PHONE = re.compile(r"(?<!\d)1[3-9]\d{9}(?!\d)")


@dataclass
class RedactionResult:
    text: str
    counts: Dict[str, int]


def redact_pii(text: str) -> RedactionResult:
    """按身份证 → 银行卡 → 手机号的顺序替换，避免长数字被短规则截断。"""
    counts = {"id_card": 0, "phone": 0, "bank_card": 0}

    text, counts["id_card"] = _ID_CARD.subn("[身份证]", text)
    text, counts["bank_card"] = _BANK_CARD.subn("[银行卡]", text)
    text, counts["phone"] = _PHONE.subn("[手机号]", text)

    return RedactionResult(text=text, counts=counts)
