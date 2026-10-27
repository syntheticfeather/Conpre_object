"""源文件 → 同名 Markdown 的懒转换。

约定：
- ``a.jpg`` 的派生文件是 ``a.jpg.md``，避免 ``a.jpg`` / ``a.pdf`` 冲突；
- ``a.jpg.md.meta.json`` 记录源文件 hash、转换器版本、模型和状态；
- 源文件 hash / 版本 / 模型任一变化都会触发重新转换；
- 转换失败不写半成品 Markdown，状态写入 meta，调用方降级。
"""
from __future__ import annotations

import hashlib
import json
import mimetypes
import os
import tempfile
import time
from contextlib import contextmanager
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict, Iterator, Optional, Tuple

from llm_features.prompt import TRUNCATION_MARKER
from llm_features.vlm_client import VLMClient

CONVERTER_VERSION = "doc-md-v1"
MIN_PDF_TEXT_CHARS = 20
MAX_PAGES = 20
MAX_MD_CHARS = 40000

TEXT_EXTS = {".md", ".txt"}
IMAGE_EXTS = {".png", ".jpg", ".jpeg", ".webp", ".bmp", ".tif", ".tiff"}
PDF_EXT = ".pdf"


@dataclass
class DocumentText:
    source_path: Path
    md_path: Optional[Path]
    text: str
    ok: bool
    cached: bool
    status: str
    mode: str
    error: Optional[str] = None
    page_count: Optional[int] = None
    source_sha256: str = ""


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with open(path, "rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


@contextmanager
def _file_lock(path: Path, timeout: float = 60.0) -> Iterator[None]:
    """跨进程的简单文件锁，避免同一份材料被并发重复转换。"""
    path.parent.mkdir(parents=True, exist_ok=True)
    deadline = time.monotonic() + timeout
    fd: Optional[int] = None
    while True:
        try:
            fd = os.open(path, os.O_CREAT | os.O_EXCL | os.O_WRONLY)
            os.write(fd, str(os.getpid()).encode("ascii"))
            os.close(fd)
            fd = None
            break
        except FileExistsError:
            if time.monotonic() > deadline:
                raise TimeoutError(f"等待文件锁超时: {path}")
            time.sleep(0.05)

    try:
        yield
    finally:
        if fd is not None:
            os.close(fd)
        try:
            path.unlink()
        except FileNotFoundError:
            pass


def _atomic_write_text(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    fd, tmp_name = tempfile.mkstemp(
        dir=str(path.parent), prefix=".md-", suffix=".tmp"
    )
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as handle:
            handle.write(text)
        os.replace(tmp_name, path)
    except Exception:
        if os.path.exists(tmp_name):
            os.remove(tmp_name)
        raise


def _atomic_write_json(path: Path, payload: Dict[str, Any]) -> None:
    _atomic_write_text(path, json.dumps(payload, ensure_ascii=False, indent=2))


class DocumentConverter:
    """把申请材料源文件懒转换为 Markdown 文本。"""

    def __init__(
        self,
        vlm_client: VLMClient,
        base_dir: Optional[str | Path] = None,
        converter_version: str = CONVERTER_VERSION,
        max_pages: int = MAX_PAGES,
        max_chars: int = MAX_MD_CHARS,
        lock_timeout: float = 60.0,
        force: bool = False,
    ):
        self.vlm_client = vlm_client
        self.base_dir = Path(base_dir).resolve() if base_dir is not None else None
        self.converter_version = converter_version
        self.max_pages = max_pages
        self.max_chars = max_chars
        self.lock_timeout = lock_timeout
        self.force = force

    @property
    def model_name(self) -> str:
        return getattr(
            self.vlm_client, "model", self.vlm_client.__class__.__name__
        )

    def resolve(self, source_path: str | Path, force: bool = False) -> DocumentText:
        force = force or self.force
        source = self._resolve_source(source_path)
        if not source.exists():
            return self._failure(source, None, "missing", f"源文件不存在: {source}")

        ext = source.suffix.lower()
        source_hash = sha256_file(source)

        if ext in TEXT_EXTS:
            text = source.read_text(encoding="utf-8", errors="replace")
            return DocumentText(
                source_path=source,
                md_path=source,
                text=text,
                ok=True,
                cached=True,
                status="ready",
                mode="source_text",
                source_sha256=source_hash,
            )

        if ext != PDF_EXT and ext not in IMAGE_EXTS:
            return self._failure(
                source, None, "unsupported", f"不支持的文件类型: {ext}"
            )

        md_path = Path(str(source) + ".md")
        meta_path = Path(str(md_path) + ".meta.json")

        if not force:
            cached = self._read_cached(md_path, meta_path, source_hash)
            if cached is not None:
                return cached

        try:
            with _file_lock(
                Path(str(md_path) + ".lock"), timeout=self.lock_timeout
            ):
                # 等锁期间可能已被其他进程转换完成
                if not force:
                    cached = self._read_cached(md_path, meta_path, source_hash)
                    if cached is not None:
                        return cached
                return self._convert_and_store(
                    source, md_path, meta_path, source_hash, ext
                )
        except TimeoutError as exc:
            return self._failure(source, md_path, "locked", str(exc))

    # ── 转换 ──

    def _convert_and_store(
        self,
        source: Path,
        md_path: Path,
        meta_path: Path,
        source_hash: str,
        ext: str,
    ) -> DocumentText:
        try:
            if ext == PDF_EXT:
                text, page_count, mode = self._extract_pdf(source)
            else:
                text, page_count, mode = self._extract_image(source)

            text = self._normalize(text)
            if not text:
                self._write_meta(
                    meta_path, source, md_path, source_hash, "empty",
                    "none", page_count, 0, "转换结果为空",
                )
                return self._failure(
                    source, md_path, "empty", "转换结果为空",
                    page_count=page_count, source_hash=source_hash,
                )

            _atomic_write_text(md_path, text)
            self._write_meta(
                meta_path, source, md_path, source_hash, "ready",
                mode, page_count, len(text), None,
            )
            return DocumentText(
                source_path=source,
                md_path=md_path,
                text=text,
                ok=True,
                cached=False,
                status="ready",
                mode=mode,
                page_count=page_count,
                source_sha256=source_hash,
            )
        except Exception as exc:
            self._write_meta(
                meta_path, source, md_path, source_hash, "failed",
                "none", None, 0, str(exc),
            )
            return self._failure(
                source, md_path, "failed", str(exc), source_hash=source_hash
            )

    def _extract_image(self, source: Path) -> Tuple[str, Optional[int], str]:
        mime_type = mimetypes.guess_type(str(source))[0] or "application/octet-stream"
        text = self.vlm_client.transcribe(
            source_path=source,
            image_bytes=source.read_bytes(),
            mime_type=mime_type,
        )
        return text, None, "image_vlm"

    def _extract_pdf(self, source: Path) -> Tuple[str, int, str]:
        import pymupdf

        document = pymupdf.open(str(source))
        try:
            page_count = document.page_count
            if page_count == 0:
                return "", 0, "pdf_text"

            page_limit = min(page_count, self.max_pages)
            texts = [
                document[index].get_text("text") or ""
                for index in range(page_limit)
            ]
            if sum(len(text.strip()) for text in texts) >= MIN_PDF_TEXT_CHARS:
                body = "\n\n".join(
                    f"<!-- page {index + 1} -->\n{text.strip()}"
                    for index, text in enumerate(texts)
                )
                if page_count > self.max_pages:
                    body += (
                        f"\n\n<!-- 已截断：共 {page_count} 页，"
                        f"仅处理前 {self.max_pages} 页 -->"
                    )
                return body, page_count, "pdf_text"

            parts = []
            for index in range(min(page_count, self.max_pages)):
                pixmap = document[index].get_pixmap(dpi=150)
                parts.append(
                    f"<!-- page {index + 1} -->\n"
                    + self.vlm_client.transcribe(
                        source_path=source,
                        image_bytes=pixmap.tobytes("png"),
                        mime_type="image/png",
                        page_index=index,
                    ).strip()
                )
            if page_count > self.max_pages:
                parts.append(
                    f"<!-- 已截断：共 {page_count} 页，仅处理前 {self.max_pages} 页 -->"
                )
            return "\n\n".join(parts), page_count, "pdf_vlm"
        finally:
            document.close()

    # ── 缓存与工具 ──

    def _read_cached(
        self, md_path: Path, meta_path: Path, source_hash: str
    ) -> Optional[DocumentText]:
        if not md_path.exists():
            return None
        meta = self._read_meta(meta_path)
        if meta is None:
            return None
        if meta.get("status") != "ready":
            return None
        if meta.get("source_sha256") != source_hash:
            return None
        if meta.get("converter_version") != self.converter_version:
            return None
        if meta.get("model") != self.model_name:
            return None

        text = md_path.read_text(encoding="utf-8", errors="replace")
        return DocumentText(
            source_path=Path(meta.get("source_path", "")),
            md_path=md_path,
            text=text,
            ok=True,
            cached=True,
            status="ready",
            mode=meta.get("mode", "cached"),
            page_count=meta.get("page_count"),
            source_sha256=source_hash,
        )

    def _normalize(self, text: str) -> str:
        text = (text or "").strip()
        if len(text) > self.max_chars:
            text = text[: self.max_chars] + TRUNCATION_MARKER
        return text

    def _resolve_source(self, source_path: str | Path) -> Path:
        path = Path(source_path)
        if not path.is_absolute() and self.base_dir is not None:
            path = self.base_dir / path
        path = path.resolve()
        if self.base_dir is not None and not path.is_relative_to(self.base_dir):
            raise ValueError(f"源文件不在允许的目录 {self.base_dir} 内: {path}")
        return path

    @staticmethod
    def _read_meta(meta_path: Path) -> Optional[Dict[str, Any]]:
        if not meta_path.exists():
            return None
        try:
            payload = json.loads(meta_path.read_text(encoding="utf-8"))
            return payload if isinstance(payload, dict) else None
        except (OSError, ValueError):
            return None

    def _write_meta(
        self,
        meta_path: Path,
        source: Path,
        md_path: Path,
        source_hash: str,
        status: str,
        mode: str,
        page_count: Optional[int],
        text_chars: int,
        error: Optional[str],
    ) -> None:
        _atomic_write_json(meta_path, {
            "source_path": str(source),
            "source_sha256": source_hash,
            "md_path": str(md_path),
            "converter_version": self.converter_version,
            "model": self.model_name,
            "status": status,
            "mode": mode,
            "page_count": page_count,
            "text_chars": text_chars,
            "error": error,
            "created_at": datetime.now(timezone.utc).isoformat(),
        })

    @staticmethod
    def _failure(
        source: Path,
        md_path: Optional[Path],
        status: str,
        error: str,
        page_count: Optional[int] = None,
        source_hash: str = "",
    ) -> DocumentText:
        return DocumentText(
            source_path=source,
            md_path=md_path,
            text="",
            ok=False,
            cached=False,
            status=status,
            mode="none",
            error=error,
            page_count=page_count,
            source_sha256=source_hash,
        )
