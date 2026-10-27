"""在线特征服务：解析材料、脱敏、超时控制、失败降级。"""
from __future__ import annotations

import logging
from concurrent.futures import ThreadPoolExecutor
from typing import Any, Dict, List, Optional

from llm_features.deterministic import compute_deterministic_features
from llm_features.documents import DocumentConverter
from llm_features.ingest import resolve_record
from llm_features.schema import (
    DETERMINISTIC_FEATURE_NAMES,
    RiskRecordInput,
    default_feature_dict,
)
from llm_features.security import redact_pii

logger = logging.getLogger("risk_api")

_ERROR_HINT = {
    "INVALID_PATH": 400,
    "INVALID_INPUT": 400,
}


def _has_source_paths(raw: Dict[str, Any]) -> bool:
    documents = raw.get("documents") or []
    if not isinstance(documents, list):
        return False
    return any(
        isinstance(document, dict) and document.get("source_path")
        for document in documents
    )


def _empty_deterministic() -> Dict[str, float]:
    return {name: 0 for name in DETERMINISTIC_FEATURE_NAMES}


def _safe_conversion(infos: Optional[List[Dict[str, Any]]]) -> List[Dict[str, Any]]:
    """不向客户端暴露服务器路径和内部错误。"""
    result = []
    for info in infos or []:
        status = info.get("status", "unknown")
        result.append({
            "type": info.get("type", "unknown"),
            "status": status,
            "cached": bool(info.get("cached")),
            "error": None if status in ("ready", "provided") else "材料转换失败",
        })
    return result


class RiskFeatureService:
    """把一次在线请求转成风险特征响应。"""

    def __init__(
        self,
        extractor: Any,
        converter: Optional[DocumentConverter] = None,
        redact: bool = True,
        timeout: float = 30.0,
        max_workers: int = 4,
        model: Optional[str] = None,
    ):
        self.extractor = extractor
        self.converter = converter
        self.redact = redact
        self.timeout = timeout
        self.executor = ThreadPoolExecutor(max_workers=max(1, max_workers))
        self.model = model or getattr(extractor, "model", "unknown")

    def process(self, raw: Dict[str, Any]) -> Dict[str, Any]:
        application_id = raw.get("application_id", 0)
        conversion_info: List[Dict[str, Any]] = []

        if self.converter is not None:
            try:
                raw, conversion_info = resolve_record(raw, self.converter)
            except ValueError:
                logger.warning("invalid document path application_id=%s", application_id)
                return self._error(application_id, "INVALID_PATH", "材料路径不合法")
            except Exception:
                logger.exception("document conversion failed application_id=%s", application_id)
                return self._error(
                    application_id, "DOCUMENT_CONVERSION_FAILED", "材料转换失败"
                )
        elif _has_source_paths(raw):
            return self._error(
                application_id, "DOCUMENT_CONVERSION_FAILED", "服务未配置文件目录"
            )

        if self.redact:
            raw = self._redact_record(raw)

        try:
            record = RiskRecordInput.model_validate(raw)
        except Exception:
            logger.warning("invalid input application_id=%s", application_id)
            return self._error(application_id, "INVALID_INPUT", "输入不合法")

        deterministic = compute_deterministic_features(record)
        future = self.executor.submit(self.extractor.extract, record)
        try:
            result = future.result(timeout=self.timeout)
        except TimeoutError:
            future.cancel()
            logger.warning("feature extraction timeout application_id=%s", application_id)
            return self._error(
                application_id,
                "TIMEOUT",
                "特征抽取超时",
                deterministic=deterministic,
                conversion_info=conversion_info,
            )
        except Exception:
            logger.exception("feature extraction failed application_id=%s", application_id)
            return self._error(
                application_id,
                "LLM_EXTRACTION_FAILED",
                "特征抽取失败",
                deterministic=deterministic,
                conversion_info=conversion_info,
            )
        finally:
            self._flush_cache()

        return {
            "application_id": result.application_id,
            "status": "ok" if result.ok else "error",
            "error_code": None if result.ok else "LLM_EXTRACTION_FAILED",
            "error": None if result.ok else "特征抽取失败",
            "features": result.features,
            "deterministic_features": result.deterministic_features or deterministic,
            "document_conversion": _safe_conversion(conversion_info),
            "reasoning": result.reasoning or "",
            "model": self.model,
            "cached": bool(result.cached),
        }

    def _flush_cache(self) -> None:
        flush = getattr(self.extractor, "flush_cache", None)
        if callable(flush):
            flush()

    def _redact_record(self, raw: Dict[str, Any]) -> Dict[str, Any]:
        safe = dict(raw)
        documents = []
        for document in raw.get("documents") or []:
            item = dict(document)
            text = item.get("text")
            if text:
                item["text"] = redact_pii(str(text)).text
            documents.append(item)
        safe["documents"] = documents
        safe["application_note"] = redact_pii(
            str(raw.get("application_note") or "")
        ).text
        return safe

    def _error(
        self,
        application_id: Any,
        error_code: str,
        message: str,
        deterministic: Optional[Dict[str, float]] = None,
        conversion_info: Optional[List[Dict[str, Any]]] = None,
    ) -> Dict[str, Any]:
        return {
            "application_id": application_id if isinstance(application_id, int) else 0,
            "status": "error",
            "error_code": error_code,
            "error": message,
            "features": default_feature_dict(available=False),
            "deterministic_features": deterministic or _empty_deterministic(),
            "document_conversion": _safe_conversion(conversion_info),
            "reasoning": "",
            "model": self.model,
            "cached": False,
        }

    def close(self) -> None:
        self.executor.shutdown(wait=False, cancel_futures=True)
