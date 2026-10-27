"""确定性 Mock 客户端。

用途：
- 单元测试不联网；
- 没有 API Key 时演示完整流水线。

它只做关键词启发式，**不代表真实 LLM 效果**。
"""
from __future__ import annotations

import json
import re

_DOC_RE = re.compile(r"<document[^>]*>(.*?)</document>", re.DOTALL)
_NOTE_RE = re.compile(r"<application_note>(.*?)</application_note>", re.DOTALL)
# 只对负面记录做否定消解，避免“无逾期”被误判为“逾期”
_NEGATED_NEGATIVE = re.compile(
    r"(无|没有|不存在|未出现)(逾期|诉讼|失信|被执行)"
)
_PATTERNS = {
    "doc_income_consistency": ("不一致", "不符", "虚报"),
    "doc_income_stability": ("不稳定", "频繁换工作", "无固定工作", "临时工"),
    "doc_employment_risk": ("无固定工作", "临时工", "失业", "刚离职"),
    "doc_multi_lender_signal": ("频繁查询", "多头借贷", "多家小额贷款", "多次申请"),
    "doc_material_mismatch": ("材料不一致", "信息矛盾", "前后不符"),
    "doc_suspicious_document": ("模糊", "无法辨认", "涂改", "疑似伪造", "ps"),
    "doc_debt_pressure": ("负债较高", "还款压力大", "月供占比高", "多头负债"),
    "doc_negative_public_record": ("逾期", "诉讼", "失信", "被执行"),
}


class MockLLMClient:
    """基于关键词的确定性特征抽取。"""

    def __init__(self):
        self.calls = 0

    @staticmethod
    def _data_sections(prompt: str) -> str:
        """只分析材料与备注，避免把 prompt 里的字段说明当成材料。"""
        parts = _DOC_RE.findall(prompt) + _NOTE_RE.findall(prompt)
        # 直接传入裸文本（如单测）时，没有标签就分析全文
        return "\n".join(parts) if parts else prompt

    def complete(self, system: str, user: str) -> str:
        self.calls += 1
        text = _NEGATED_NEGATIVE.sub("", self._data_sections(user)).lower()

        payload = {"reasoning": "Mock 关键词启发式判断。"}
        risk_count = 0

        for name, markers in _PATTERNS.items():
            hit = any(marker in text for marker in markers)
            if name in ("doc_income_consistency", "doc_income_stability"):
                value = not hit
                # 这两个特征为 True 表示低风险，不计入风险维度
                if not value:
                    risk_count += 1
            else:
                value = hit
                if value:
                    risk_count += 1
            payload[name] = value

        missing = (
            "(无材料)" in user
            or any(marker in text for marker in ("缺少", "未提供", "缺失"))
        )
        payload["doc_missing_key_info"] = missing
        if missing:
            risk_count += 1

        payload["llm_verdict"] = risk_count >= 2
        payload["llm_confidence"] = min(0.95, 0.6 + 0.05 * risk_count)
        payload["reasoning"] = (
            f"Mock 关键词启发式：命中 {risk_count} 个风险维度。"
        )
        return json.dumps(payload, ensure_ascii=False)
