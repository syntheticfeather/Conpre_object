"""基于 sha256 的 JSON 文件缓存。

缓存键由「模型 + prompt 版本 + 输入内容」共同决定，任一项变化都会
产生新键，避免旧 prompt 的结果被复用。
"""
from __future__ import annotations

import hashlib
import json
import os
import tempfile
import threading
from pathlib import Path
from typing import Any, Dict, Optional


def make_cache_key(model: str, prompt_version: str, payload: Any) -> str:
    """生成稳定缓存键，与字典键顺序无关。"""
    canonical = json.dumps(
        {"model": model, "prompt_version": prompt_version, "payload": payload},
        sort_keys=True,
        ensure_ascii=False,
        default=str,
    )
    return hashlib.sha256(canonical.encode("utf-8")).hexdigest()


class FileCache:
    """简单的进程内 + 文件持久化缓存。"""

    def __init__(self, path: str | Path):
        self.path = Path(path)
        self._lock = threading.RLock()
        self._data: Dict[str, Any] = self._load()

    def _load(self) -> Dict[str, Any]:
        if not self.path.exists():
            return {}
        try:
            raw = json.loads(self.path.read_text(encoding="utf-8"))
            return raw if isinstance(raw, dict) else {}
        except (OSError, ValueError):
            # 缓存损坏不应阻断批处理，直接当作空缓存
            return {}

    def __len__(self) -> int:
        with self._lock:
            return len(self._data)

    def get(self, key: str) -> Optional[Any]:
        with self._lock:
            return self._data.get(key)

    def set(self, key: str, value: Any, persist: bool = True) -> None:
        """写入缓存。

        批量抽取时用 ``persist=False`` 避免每条记录都重写整个文件，
        批处理结束后统一调用 ``save()``。
        """
        with self._lock:
            self._data[key] = value
            if persist:
                self.save()

    def save(self) -> None:
        with self._lock:
            self.path.parent.mkdir(parents=True, exist_ok=True)
            fd, tmp_name = tempfile.mkstemp(
                dir=str(self.path.parent), prefix=".cache-", suffix=".tmp"
            )
            try:
                with os.fdopen(fd, "w", encoding="utf-8") as handle:
                    json.dump(self._data, handle, ensure_ascii=False)
                os.replace(tmp_name, self.path)
            except Exception:
                if os.path.exists(tmp_name):
                    os.remove(tmp_name)
                raise
