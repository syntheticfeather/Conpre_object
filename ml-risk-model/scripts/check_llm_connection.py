"""检查 .env 配置与真实 LLM / VLM 连通性。

用法::

    cd ml-risk-model
    # 先编辑 .env 填入 LLM_API_KEY
    PYTHONIOENCODING=utf-8 python scripts/check_llm_connection.py
    PYTHONIOENCODING=utf-8 python scripts/check_llm_connection.py --skip-vlm
"""
from __future__ import annotations

import argparse
import os
import sys
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parents[1]
if str(BASE_DIR) not in sys.path:
    sys.path.insert(0, str(BASE_DIR))

from llm_features.client import OpenAICompatibleClient  # noqa: E402
from llm_features.config import load_env  # noqa: E402
from llm_features.vlm_client import OpenAICompatibleVLMClient  # noqa: E402

DEFAULT_IMAGE = BASE_DIR / "data" / "llm_features" / "source_docs" / "93" / "00_employment_cert.png"


def main() -> int:
    parser = argparse.ArgumentParser(description="检查 LLM/VLM API 连通性")
    parser.add_argument("--skip-vlm", action="store_true")
    parser.add_argument("--image", default=str(DEFAULT_IMAGE))
    args = parser.parse_args()

    loaded = load_env()
    print("读取配置:", ", ".join(str(path) for path in loaded) or "(未找到 .env)")
    print("LLM_BASE_URL:", os.getenv("LLM_BASE_URL", "(默认)"))
    print("LLM_MODEL:   ", os.getenv("LLM_MODEL", "(默认)"))
    print("VLM_MODEL:   ", os.getenv("VLM_MODEL", "(默认)"))
    print("API Key:     ", "已配置" if (
        os.getenv("LLM_API_KEY") or os.getenv("OPENAI_API_KEY")
    ) else "未配置")

    print("\n[1/2] 文本 LLM ...")
    try:
        client = OpenAICompatibleClient()
        answer = client.complete(
            "你是一个严格的测试助手。",
            "只回复两个字：成功",
        )
        print("  模型:", client.model)
        print("  回复:", answer.strip()[:200])
    except Exception as exc:
        print("  失败:", exc)
        print("  请检查 .env 中的 LLM_API_KEY / LLM_BASE_URL / LLM_MODEL")
        return 1

    if args.skip_vlm:
        print("\n[2/2] 跳过 VLM 检查")
        return 0

    print("\n[2/2] 视觉模型 ...")
    image = Path(args.image)
    if not image.exists():
        print(f"  跳过：样例图片不存在 {image}")
        print("  可先运行 python scripts/generate_source_docs.py --limit 5")
        return 0

    try:
        vlm = OpenAICompatibleVLMClient()
        text = vlm.transcribe(
            source_path=image,
            image_bytes=image.read_bytes(),
            mime_type="image/png",
        )
        print("  模型:", vlm.model)
        print("  转录:", text.strip()[:200])
    except Exception as exc:
        print("  失败:", exc)
        print("  请检查 .env 中的 VLM_* 配置，或确认该模型支持图片输入")
        return 2

    return 0


if __name__ == "__main__":
    try:
        sys.stdout.reconfigure(encoding="utf-8")
    except Exception:
        pass
    raise SystemExit(main())
