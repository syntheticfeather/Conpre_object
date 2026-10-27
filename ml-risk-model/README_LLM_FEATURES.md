# 风控 LLM 特征工程（离线）

参考 *LLM Classification Is Feature Engineering*：让 LLM 从申请材料中抽取风险特征，
再由 XGBoost 做决策。LLM 不直接输出审批结论。

设计文档：[docs/superpowers/specs/2026-10-26-risk-llm-features-design.md](../docs/superpowers/specs/2026-10-26-risk-llm-features-design.md)

## 快速开始

```bash
cd ml-risk-model
python -m pip install -r requirements.txt

# 1. 生成合成样例（1200 条，可重复）
PYTHONIOENCODING=utf-8 python scripts/generate_sample_data.py

# 2. 无 API Key 跑通批量抽取（Mock 客户端）
PYTHONIOENCODING=utf-8 python -m llm_features.batch \
  --input data/llm_features/sample_applications.jsonl \
  --output data/llm_features/sample_llm_features.jsonl \
  --cache .cache/llm_features.json --mock

# 3. 三组消融实验
PYTHONIOENCODING=utf-8 python train_with_llm_features.py --model auto

# 4. 单条申请演示（看输入 → Prompt → 抽取结果 → 合并后的特征行）
PYTHONIOENCODING=utf-8 python scripts/demo_single_application.py --application-id 93
PYTHONIOENCODING=utf-8 python scripts/demo_single_application.py --label 0 --show-prompt

# 5. 测试
PYTHONIOENCODING=utf-8 python -m pytest
```

## 源文件懒转换（离线 Mock）

项目数据库存的是材料图片路径，不是文本。本模块提供“源文件 → 同名 `.md`”的懒转换：

```bash
# 1. 生成样例源文件（PNG）和 Mock VLM 夹具
PYTHONIOENCODING=utf-8 python scripts/generate_source_docs.py --limit 10

# 2. 查看单条申请的转换过程：源文件 → .md → 特征
PYTHONIOENCODING=utf-8 python scripts/demo_document_conversion.py --application-id 93

# 3. 批处理直接读取 source_path
PYTHONIOENCODING=utf-8 python -m llm_features.batch \
  --input data/llm_features/sample_applications_with_paths.jsonl \
  --output data/llm_features/features_from_source_docs.jsonl \
  --doc-base-dir data/llm_features/source_docs --mock
```

- `a.jpg` → `a.jpg.md` + `a.jpg.md.meta.json`
- 元数据记录源文件 hash、转换器版本、模型；任一变化自动重转
- PDF 先抽文本层，扫描件逐页渲染后送 VLM
- 失败写 `status=failed`，下游用 `llm_feature_available=0` 降级
- Mock VLM 读取 `<源文件>.mock.md` 夹具，不需要 API Key

## 配置真实 API Key

编辑 `ml-risk-model/.env`（已被 gitignore，模板见 `.env.example`）：

```dotenv
LLM_API_KEY=sk-your-key
LLM_BASE_URL=https://api.deepseek.com
LLM_MODEL=deepseek-flash

# 可选：DeepSeek 思考模式
LLM_THINKING=enabled
LLM_REASONING_EFFORT=high

# 与文本共用 Key 时，VLM_API_KEY 留空即可
VLM_API_KEY=
VLM_BASE_URL=https://api.deepseek.com
VLM_MODEL=deepseek-flash
```

先做连通性自检：

```bash
PYTHONIOENCODING=utf-8 python scripts/check_llm_connection.py
# 只测文本：加 --skip-vlm
```

文本与视觉模型默认都配成 `deepseek-flash`。已实测该模型支持中文图片输入，
可以同时完成文本特征抽取和材料图片转录。

去掉 `--mock` 即可跑真实调用：

```bash
PYTHONIOENCODING=utf-8 python -m llm_features.batch \
  --input data/llm_features/sample_applications.jsonl \
  --output data/llm_features/sample_llm_features.jsonl

# 真实 VLM 读源文件
PYTHONIOENCODING=utf-8 python scripts/demo_document_conversion.py \
  --application-id 93 --real --force
```

## 在线服务（FastAPI）

```bash
cd ml-risk-model
PYTHONIOENCODING=utf-8 python -m uvicorn llm_features.api.app:app \
  --host 127.0.0.1 --port 8000
```

健康检查：

```bash
curl http://127.0.0.1:8000/health
```

特征抽取（必须带 `X-API-Key`）：

```bash
curl -X POST http://127.0.0.1:8000/v1/risk/features \
  -H "Content-Type: application/json" \
  -H "X-API-Key: classroom-demo-key" \
  -d '{"application_id":1,
       "documents":[{"type":"salary_cert","text":"收入稳定"}]}'
```

安全配置在 `.env`：

```dotenv
RISK_API_KEY=classroom-demo-key
RISK_DOC_BASE_DIR=data/llm_features/source_docs
RISK_REQUEST_TIMEOUT=30
RISK_MAX_WORKERS=4
RISK_REDACT_PII=true
RISK_FEATURE_CACHE_PATH=.cache/risk_features.json
```

已实现：API Key 鉴权、文本侧 PII 脱敏、请求超时降级、路径穿越防护、
1 MB 请求体限制、`X-Request-ID` 审计日志。

同一份材料 + 相同申请上下文重复请求时，特征值缓存会命中：
响应 `cached=true`，不再调用 LLM。实测首次约 10.6 秒，第二次约 0.016 秒。
`RISK_FEATURE_CACHE_PATH` 设为空字符串可关闭缓存。

生产环境请替换 `RISK_API_KEY`，并把 `RISK_DOC_BASE_DIR` 指到真实的
`uploads` 目录。

Java 申请流接入、异步转换队列和图片 PII 脱敏仍未做，见
[在线服务与安全设计](../docs/superpowers/specs/2026-10-27-online-service-security-design.md)。

## 真实数据接入

**表格特征**：从 `ml_training_log` 导出，至少包含 `application_id`、`label`
和模型使用的结构化特征。支持 `.jsonl` 或 `.csv`。

**材料文本**：每条申请一行 JSONL，格式见设计文档第 3 节：

```json
{
  "application_id": 1001,
  "label": 0,
  "application": {"amount": 50000, "term": 12, "income": 8000},
  "documents": [
    {"type": "salary_cert", "text": "..."},
    {"type": "credit_report", "text": "..."}
  ],
  "application_note": "..."
}
```

然后：

```bash
python -m llm_features.batch --input applications.jsonl --output llm_features.jsonl
python train_with_llm_features.py \
  --tabular ml_training_log.jsonl \
  --llm llm_features.jsonl \
  --out model/llm_feature_ablation_report.json
```

## 产物说明

| 产物 | 说明 |
|---|---|
| G 组（12 维） | LLM 抽取的定性风险特征 + 可用性标志 |
| H 组（5 维） | 材料长度、份数、类型覆盖率等确定性特征 |
| `*.errors.jsonl` | 抽取失败的记录，主输出中保留降级特征 |
| 消融报告 JSON | 三组特征集的 AUC / KS / Brier / F1 |

## 限制

- 合成样例只验证流水线，**指标不代表真实效果**；报告会标注 `synthetic=true`。
- 本阶段只做离线抽取与训练，不含在线服务、Java 接入、SHAP/PSI。
- 材料文本由 OCR/录入侧提供，本模块不解析 PDF 或图片。
- 真实上线前必须处理 PII 脱敏、提示注入和 LLM 版本漂移（见设计文档第 10 节）。
