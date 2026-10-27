"""LLM 特征抽取器：调用、解析、修复重试、降级、缓存。"""
from __future__ import annotations

import json
import re
import time
from dataclasses import dataclass
from typing import Any, Dict, Optional, Tuple

from llm_features.cache import FileCache, make_cache_key
from llm_features.client import LLMClient
from llm_features.deterministic import compute_deterministic_features
from llm_features.prompt import PROMPT_VERSION, SYSTEM_PROMPT, build_user_prompt
from llm_features.schema import (
    LLMRiskFeatures,
    RiskRecordInput,
    default_feature_dict,
)

_CODE_FENCE = re.compile(r"```(?:json)?\s*(.*?)```", re.DOTALL | re.IGNORECASE)
_MAX_REPAIR_INPUT = 2000


@dataclass
class ExtractionResult:
    application_id: int
    features: Dict[str, Any]
    deterministic_features: Dict[str, Any]
    ok: bool
    cached: bool = False
    error: Optional[str] = None
    reasoning: str = ""


class LLMFeatureExtractor:
    """把申请材料转成模型可用的风险特征。"""

    def __init__(
        self,
        client: LLMClient,
        cache: Optional[FileCache] = None,
        model: str = "deepseek-flash",
        retries: int = 2,
        retry_delay: float = 0.5,
    ):
        self.client = client
        self.cache = cache
        self.model = model
        self.retries = max(1, retries)
        self.retry_delay = retry_delay

    def extract(self, record: RiskRecordInput) -> ExtractionResult:
        deterministic = compute_deterministic_features(record)
        user_prompt = build_user_prompt(record)

        cache_key = make_cache_key(
            self.model,
            PROMPT_VERSION,
            {"system": SYSTEM_PROMPT, "user": user_prompt},
        )
        cached = self._read_cache(cache_key)
        if cached is not None:
            return self._success(record, cached, deterministic, cached=True)

        try:
            raw = self._complete_with_retry(user_prompt)
        except Exception as exc:
            return self._failure(record, deterministic, f"LLM 调用失败: {exc}")

        parsed, error = self._parse(raw)
        if parsed is None:
            try:
                raw = self._complete_with_retry(self._repair_prompt(raw, error))
                parsed, error = self._parse(raw)
            except Exception as exc:
                return self._failure(record, deterministic, f"修复调用失败: {exc}")

        if parsed is None:
            return self._failure(record, deterministic, error or "解析失败")

        if self.cache is not None:
            # 批量抽取时由调用方在结束时统一落盘，避免 O(n²) 写放大
            self.cache.set(cache_key, parsed.model_dump(), persist=False)
        return self._success(record, parsed, deterministic, cached=False)

    def flush_cache(self) -> None:
        """把延迟写入的缓存落盘；在线服务每次请求后调用。"""
        if self.cache is not None:
            self.cache.save()

    # ── 内部 ──

    def _complete_with_retry(self, user_prompt: str) -> str:
        last_error: Optional[Exception] = None
        for attempt in range(self.retries):
            try:
                return self.client.complete(SYSTEM_PROMPT, user_prompt)
            except Exception as exc:
                last_error = exc
                if attempt < self.retries - 1 and self.retry_delay > 0:
                    time.sleep(self.retry_delay * (2 ** attempt))
        raise last_error if last_error else RuntimeError("LLM 调用失败")

    def _read_cache(self, key: str) -> Optional[LLMRiskFeatures]:
        if self.cache is None:
            return None
        cached = self.cache.get(key)
        if cached is None:
            return None
        try:
            return LLMRiskFeatures.model_validate(cached)
        except Exception:
            # 旧版本或损坏的缓存条目直接忽略
            return None

    @staticmethod
    def _parse(raw: str) -> Tuple[Optional[LLMRiskFeatures], Optional[str]]:
        text = raw.strip()
        fence = _CODE_FENCE.search(text)
        if fence:
            text = fence.group(1).strip()

        start, end = text.find("{"), text.rfind("}")
        if start == -1 or end == -1 or end <= start:
            return None, "输出中没有找到 JSON 对象"
        candidate = text[start:end + 1]

        try:
            payload = json.loads(candidate)
        except json.JSONDecodeError as exc:
            return None, f"JSON 解析失败: {exc}"

        try:
            return LLMRiskFeatures.model_validate(payload), None
        except Exception as exc:
            return None, f"字段校验失败: {exc}"[:500]

    @staticmethod
    def _repair_prompt(raw: str, error: Optional[str]) -> str:
        safe = raw[:_MAX_REPAIR_INPUT].replace("<", "＜").replace(">", "＞")
        return f"""\
你上一次输出无法解析为要求的 JSON。
错误：{error or "未知错误"}

原始输出：
<invalid_output>
{safe}
</invalid_output>

请只输出修正后的 JSON 对象，不要输出 Markdown 代码块或解释，
且必须包含全部要求的字段。"""

    def _success(
        self,
        record: RiskRecordInput,
        parsed: LLMRiskFeatures,
        deterministic: Dict[str, Any],
        cached: bool,
    ) -> ExtractionResult:
        return ExtractionResult(
            application_id=record.application_id,
            features=parsed.to_feature_dict(),
            deterministic_features=deterministic,
            ok=True,
            cached=cached,
            reasoning=parsed.reasoning,
        )

    def _failure(
        self,
        record: RiskRecordInput,
        deterministic: Dict[str, Any],
        error: str,
    ) -> ExtractionResult:
        return ExtractionResult(
            application_id=record.application_id,
            features=default_feature_dict(available=False),
            deterministic_features=deterministic,
            ok=False,
            error=error,
        )
