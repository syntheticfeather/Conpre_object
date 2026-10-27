"""轻量 `.env` 配置加载。

不依赖 python-dotenv：
- 支持 ``KEY=VALUE``、引号、``export`` 前缀和注释；
- 默认不覆盖已存在的环境变量，便于临时用 shell 覆盖；
- 查找顺序：显式路径 → 当前目录 ``.env`` → 项目根目录 ``.env``。
"""
from __future__ import annotations

import os
from pathlib import Path
from typing import List, Optional, Union

BASE_DIR = Path(__file__).resolve().parents[1]


def _apply(path: Path, override: bool) -> None:
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        if line.startswith("export "):
            line = line[len("export "):].strip()
        if "=" not in line:
            continue

        key, value = line.split("=", 1)
        key = key.strip()
        value = value.strip()
        if not key:
            continue

        if (
            len(value) >= 2
            and value[0] == value[-1]
            and value[0] in ("'", '"')
        ):
            value = value[1:-1]
        elif " #" in value:
            value = value.split(" #", 1)[0].strip()

        if override or not os.environ.get(key):
            os.environ[key] = value


def load_env(
    path: Optional[Union[str, Path]] = None,
    override: bool = False,
) -> List[Path]:
    """加载 `.env`，返回实际读取的文件列表。"""
    if path is not None:
        candidates = [Path(path)]
    else:
        candidates = [Path.cwd() / ".env", BASE_DIR / ".env"]

    loaded: List[Path] = []
    seen = set()
    for candidate in candidates:
        resolved = candidate.resolve()
        if resolved in seen:
            continue
        seen.add(resolved)
        if resolved.exists():
            _apply(resolved, override)
            loaded.append(resolved)
    return loaded
