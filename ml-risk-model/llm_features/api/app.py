"""FastAPI 在线服务。

启动::

    uvicorn llm_features.api.app:app --host 127.0.0.1 --port 8000

请求必须带 ``X-API-Key``，值为 .env 中的 ``RISK_API_KEY``。
"""
from __future__ import annotations

import logging
import os
import secrets
import time
import uuid
from contextlib import asynccontextmanager
from typing import Optional

from fastapi import Depends, FastAPI, Header, HTTPException, Request
from fastapi.responses import JSONResponse

from llm_features.api.schemas import RiskFeatureRequest, RiskFeatureResponse
from llm_features.api.service import RiskFeatureService
from llm_features.api.settings import Settings
from llm_features.cache import FileCache
from llm_features.client import OpenAICompatibleClient
from llm_features.documents import DocumentConverter
from llm_features.extractor import LLMFeatureExtractor
from llm_features.vlm_client import OpenAICompatibleVLMClient

logger = logging.getLogger("risk_api")

MAX_BODY_BYTES = 1_000_000


def build_service(settings: Settings) -> RiskFeatureService:
    llm = OpenAICompatibleClient()

    cache_path = os.getenv(
        "RISK_FEATURE_CACHE_PATH", ".cache/risk_features.json"
    ).strip()
    cache = FileCache(cache_path) if cache_path else None
    extractor = LLMFeatureExtractor(llm, cache=cache, model=llm.model)

    converter = None
    if settings.doc_base_dir is not None:
        vlm = OpenAICompatibleVLMClient()
        converter = DocumentConverter(vlm, base_dir=settings.doc_base_dir)

    return RiskFeatureService(
        extractor,
        converter=converter,
        redact=settings.redact_pii,
        timeout=settings.request_timeout,
        max_workers=settings.max_workers,
        model=llm.model,
    )


def create_app(
    service: Optional[RiskFeatureService] = None,
    api_key: Optional[str] = None,
) -> FastAPI:
    settings = Settings.from_env()
    expected_key = settings.api_key if api_key is None else api_key
    state = {"service": service}

    def get_service() -> RiskFeatureService:
        if state["service"] is None:
            try:
                state["service"] = build_service(settings)
            except Exception:
                logger.exception("failed to build risk feature service")
                raise HTTPException(503, "service unavailable")
        return state["service"]

    def require_api_key(
        x_api_key: Optional[str] = Header(default=None, alias="X-API-Key"),
    ) -> None:
        if not expected_key:
            raise HTTPException(503, "service not configured")
        if not x_api_key or not secrets.compare_digest(x_api_key, expected_key):
            raise HTTPException(401, "invalid api key")

    @asynccontextmanager
    async def lifespan(app: FastAPI):
        yield
        active = state["service"]
        if active is not None:
            active.close()

    app = FastAPI(
        title="Risk LLM Feature Service",
        version="1.0.0",
        lifespan=lifespan,
    )

    @app.middleware("http")
    async def limit_body(request: Request, call_next):
        length = request.headers.get("content-length")
        if length:
            try:
                size = int(length)
            except ValueError:
                size = 0
            if size > MAX_BODY_BYTES:
                return JSONResponse(
                    status_code=413,
                    content={"detail": "request body too large"},
                )
        return await call_next(request)

    @app.middleware("http")
    async def add_request_id(request: Request, call_next):
        request.state.request_id = uuid.uuid4().hex
        response = await call_next(request)
        response.headers["X-Request-ID"] = request.state.request_id
        return response

    @app.get("/health")
    def health() -> dict:
        return {
            "status": "ok",
            "model": os.getenv("LLM_MODEL", "deepseek-flash"),
            "auth_configured": bool(expected_key),
            "doc_base_dir_configured": settings.doc_base_dir is not None,
        }

    @app.post("/v1/risk/features", response_model=RiskFeatureResponse)
    def extract_features(
        payload: RiskFeatureRequest,
        request: Request,
        _: None = Depends(require_api_key),
    ):
        started = time.perf_counter()
        result = get_service().process(payload.model_dump())
        elapsed_ms = (time.perf_counter() - started) * 1000

        logger.info(
            "risk_features request_id=%s application_id=%s status=%s "
            "error_code=%s elapsed_ms=%.1f",
            request.state.request_id,
            result.get("application_id"),
            result.get("status"),
            result.get("error_code"),
            elapsed_ms,
        )

        status_code = 400 if result.get("error_code") in (
            "INVALID_PATH", "INVALID_INPUT"
        ) else 200
        if status_code != 200:
            return JSONResponse(status_code=status_code, content=result)
        return result

    return app


app = create_app()
