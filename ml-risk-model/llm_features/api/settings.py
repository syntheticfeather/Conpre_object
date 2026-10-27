"""在线服务配置。"""
from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path
from typing import Optional

from llm_features.config import load_env

load_env()


def _as_bool(value: str, default: bool = True) -> bool:
    if value is None:
        return default
    return value.strip().lower() in ("1", "true", "yes", "on")


@dataclass(frozen=True)
class Settings:
    api_key: str
    doc_base_dir: Optional[Path]
    request_timeout: float
    max_workers: int
    redact_pii: bool

    @classmethod
    def from_env(cls) -> "Settings":
        base_dir = os.getenv("RISK_DOC_BASE_DIR", "").strip()
        try:
            timeout = float(os.getenv("RISK_REQUEST_TIMEOUT", "30"))
        except ValueError:
            timeout = 30.0
        try:
            workers = int(os.getenv("RISK_MAX_WORKERS", "4"))
        except ValueError:
            workers = 4

        return cls(
            api_key=os.getenv("RISK_API_KEY", "").strip(),
            doc_base_dir=Path(base_dir).resolve() if base_dir else None,
            request_timeout=timeout,
            max_workers=workers,
            redact_pii=_as_bool(os.getenv("RISK_REDACT_PII", "true")),
        )
