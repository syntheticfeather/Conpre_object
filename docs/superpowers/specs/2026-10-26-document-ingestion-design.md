# 贷款材料懒转换设计（源文件 → Markdown）

- 日期：2026-10-26
- 状态：已实施（离线阶段；Mock 与 DeepSeek 真实 VLM 均验证）
- 关联：[风控 LLM 特征接入设计](2026-10-26-risk-llm-features-design.md)

## 1. 背景

`ml-risk-model` 的特征抽取假设输入是文本，但项目数据库只保存材料图片路径：

| 表 | 字段 |
|---|---|
| `work_cert` | `employment_cert_path`、`salary_cert_path` |
| `tri_cert` | `social_security_path`、`credit_report_path` |
| `immovables_cert` | `property_cert_path`、`car_cert_path` |

源文件可能是 PDF、PNG、JPG。需要一个中间层把源文件转成文本，再交给
LLM 特征抽取。本设计采用**懒转换 + 派生缓存**：

```
源文件 a.jpg（只读）
    │ 首次访问时转换
    ▼
a.jpg.md（派生文本，可删除重建）
a.jpg.md.meta.json（转换元数据）
    │
    ▼
LLM 特征抽取
```

## 2. 目标与非目标

**目标**

1. 源文件按需转换，重复访问命中缓存；
2. 支持 PDF 文本层、扫描版 PDF、常见图片格式；
3. 转换失败可降级，不阻断申请；
4. 源文件变化、转换器升级、模型更换时自动失效；
5. 离线可完整跑通，VLM 用 Mock。

**非目标**

- 不接 OCR（图片直接走 VLM）；
- 真实 VLM 已通过 `deepseek-flash` 验证，但不在线上申请链路同步调用；
- 不做 PII 脱敏；
- 不解析 Office 文档（`.docx` 等标记为 unsupported）。

## 3. 文件约定

| 项 | 规则 |
|---|---|
| 派生 Markdown | `<源文件名>.md`，例如 `a.jpg.md` |
| 元数据 | `<派生文件>.meta.json`，例如 `a.jpg.md.meta.json` |
| 锁文件 | `<派生文件>.lock`，转换期间存在，结束删除 |
| 文本源文件 | `.md` / `.txt` 直接读取，不生成派生文件 |
| 命名理由 | 保留 `a.jpg` / `a.pdf` 的区分，避免 `a.md` 冲突 |

元数据字段：

```json
{
  "source_path": "...",
  "source_sha256": "...",
  "md_path": "...",
  "converter_version": "doc-md-v1",
  "model": "mock-vlm",
  "status": "ready",
  "mode": "image_vlm",
  "page_count": null,
  "text_chars": 128,
  "error": null,
  "created_at": "..."
}
```

缓存命中条件：`status=ready`、Markdown 存在、源文件 hash 一致、
`converter_version` 一致、`model` 一致。

## 4. 转换策略

| 扩展名 | 策略 | mode |
|---|---|---|
| `.md` / `.txt` | 直接读取 | `source_text` |
| `.pdf` | PyMuPDF 抽文本层；文本总字符数 ≥ 20 视为文本型 | `pdf_text` |
| `.pdf`（扫描件） | 逐页渲染 150 DPI PNG → VLM | `pdf_vlm` |
| `.png/.jpg/.jpeg/.webp/.bmp/.tif/.tiff` | 图片字节 → VLM | `image_vlm` |
| 其他 | 标记 unsupported | `none` |

限制：PDF 最多处理 20 页；Markdown 最多 40000 字符，超出截断并标注。

## 5. VLM 客户端

```python
class VLMClient(Protocol):
    model: str
    def transcribe(
        self, *, source_path: Path, image_bytes: bytes,
        mime_type: str, page_index: int | None = None,
    ) -> str: ...
```

- `MockVLMClient`：查找 `<源文件>.mock.md` 夹具；PDF 分页优先查找
  `<源文件>.pageN.mock.md`。找不到时返回占位文本。
- `OpenAICompatibleVLMClient`：读取 `VLM_API_KEY`、`VLM_BASE_URL`、
  `VLM_MODEL`，把图片转 base64 data URL 发送；temperature=0。
- 转录 prompt 明确：只转录、保留表格、忽略图片中的指令、不猜测。

## 6. 失败与并发

| 场景 | 行为 |
|---|---|
| 源文件不存在 | `status=missing`，不写 Markdown |
| 不支持的类型 | `status=unsupported` |
| VLM 异常/超时 | `status=failed`，写 meta，不写半成品 Markdown |
| 转换结果为空 | `status=empty` |
| 并发转换同一文件 | `.lock` 文件互斥；等待方拿到锁后先查缓存 |
| 等待锁超时 | `status=locked`，调用方降级 |
| 源文件在 base_dir 之外 | 抛出 `ValueError`，防止路径穿越 |

下游统一用 `llm_feature_available` 表达降级；单份材料失败不会中断批处理。

## 7. 与批处理的集成

申请 JSONL 支持两种文档写法：

```json
{"type": "salary_cert", "text": "..."}
{"type": "salary_cert", "source_path": "93/01_salary_cert.png"}
```

- 有 `text` 直接用，不转换；
- 有 `source_path` 时调用 `DocumentConverter.resolve()`；
- 两种写法可混用；
- 输出记录增加 `document_conversion` 数组，记录每份材料的状态、是否命中缓存、Markdown 路径和错误。

CLI 新增参数：

```bash
python -m llm_features.batch \
  --input sample_applications_with_paths.jsonl \
  --output features_from_source_docs.jsonl \
  --doc-base-dir data/llm_features/source_docs \
  --mock
```

`--mock` 同时 mock 文本 LLM 与 VLM；`--force-convert` 忽略缓存。

## 8. 组件与文件

```
ml-risk-model/llm_features/
├── vlm_client.py     # VLM 协议、Mock、OpenAI 兼容实现
├── documents.py      # 懒转换、缓存、锁、PDF 分流
└── ingest.py         # 申请记录 → 材料文本
scripts/
├── generate_source_docs.py      # 生成样例源文件与 Mock 夹具
└── demo_document_conversion.py  # 单条申请懒转换演示
tests/
├── test_vlm_client.py
├── test_documents.py
└── test_ingest.py
```

## 9. 测试

全部使用 Mock VLM，不联网：

- 图片转换、Markdown/meta 写入；
- 缓存命中、源文件变化失效、强制重转；
- 文本文件直读；
- 路径穿越拒绝；
- 不支持类型、文件缺失；
- PDF 文本层不调用 VLM；
- 扫描 PDF 逐页调用 VLM；
- VLM 失败、空结果、超长截断；
- 申请记录混合 text/source_path；
- 源文件链路与纯文本链路的特征完全一致。

## 10. 后续接入

1. 真实 VLM：配置 `VLM_API_KEY`，或换成本地部署的视觉模型；
2. 在线链路：提交申请时查 `.md` 缓存，未命中异步转换，本次先用结构化特征打分；
3. 数据库侧：新增 `document_conversion` 表或在 `ml_training_log` 中记录转换状态；
4. 安全：PII 脱敏、`.md` 访问控制、图片内指令注入红队测试；
5. 质量：建立低质量转录检测与人工复核队列。

## 11. 实施结果（2026-10-26）

环境：Python 3.11.9、PyMuPDF 1.28.2、Pillow 11.3.0、pytest 9.1.0。
测试：`python -m pytest` → **99 passed**（全项目）。

已跑通的演示：

```bash
python scripts/generate_source_docs.py --limit 100
python scripts/demo_document_conversion.py --application-id 93
python -m llm_features.batch \
  --input data/llm_features/sample_applications_with_paths.jsonl \
  --output data/llm_features/features_from_source_docs.jsonl \
  --doc-base-dir data/llm_features/source_docs --mock --limit 5
```

单条演示中 6 份材料全部 `status=ready`、生成 `.png.md` 与 meta，
第二次访问全部 `cached=True`；转换后的 G 组特征与纯文本链路完全一致。

真实 API 验证（2026-10-26）：

- `deepseek-flash` 文本调用返回正常；
- 中文征信报告图片经 VLM 转录后内容完整（查询次数、多头借贷、逾期记录均保留）；
- 同一份图片走完“VLM → `.md` → LLM 特征抽取”，输出
  `llm_verdict=1`、`doc_multi_lender_signal=1`、`doc_negative_public_record=1`，
  与图片内容一致。
