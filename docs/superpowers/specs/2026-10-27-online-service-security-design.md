# 风控 LLM 特征在线服务与安全设计

- 日期：2026-10-27
- 状态：已实施（Python 服务；Java 接入未做）
- 关联：[风控 LLM 特征接入设计](2026-10-26-risk-llm-features-design.md)

## 1. 目标

把离线特征抽取包装成一个可被业务调用的 HTTP 服务，并补上最小安全护栏：

1. FastAPI 服务，提供特征抽取与健康检查；
2. API Key 鉴权；
3. 文本侧 PII 脱敏；
4. 请求超时、失败降级、统一错误码；
5. 路径穿越、请求体大小、请求 ID、审计日志；
6. 单元测试与真实服务冒烟。

**非目标**：Java 申请流改造、异步转换队列、数据库状态表、分布式锁、限流、图片 PII 脱敏。

## 2. 架构

```
业务系统 / 审核后台
        │  POST /v1/risk/features   (X-API-Key)
        ▼
┌──────────────────────────┐
│ FastAPI                   │
│  - 鉴权 / 限流大小 / 请求ID│
│  - 超时控制               │
└────────────┬─────────────┘
             ▼
┌──────────────────────────┐
│ RiskFeatureService        │
│  1. source_path → .md     │
│  2. 文本 PII 脱敏         │
│  3. LLM 特征抽取          │
│  4. 失败降级              │
└────────────┬─────────────┘
             ▼
       特征 JSON 响应
```

## 3. API 契约

### 3.1 健康检查

```http
GET /health
```

```json
{
  "status": "ok",
  "model": "deepseek-flash",
  "auth_configured": true,
  "doc_base_dir_configured": true
}
```

不需要 API Key；不返回密钥或服务器路径。

### 3.2 特征抽取

```http
POST /v1/risk/features
X-API-Key: <RISK_API_KEY>
Content-Type: application/json
```

请求：

```json
{
  "application_id": 1001,
  "application": {"amount": 50000, "term": 12, "income": 8000},
  "documents": [
    {"type": "salary_cert", "text": "收入证明与申报收入一致"},
    {"type": "credit_report", "source_path": "1001/02_credit_report.png"}
  ],
  "application_note": "客户申请装修贷"
}
```

- `text` 与 `source_path` 可混用；
- `text` 优先；
- 不接受 `label` 等额外字段，防止标签注入；
- 单条申请最多 20 份材料；
- 单份文本最多 50000 字符，备注最多 4000 字符。

成功响应（HTTP 200）：

```json
{
  "application_id": 1001,
  "status": "ok",
  "error_code": null,
  "error": null,
  "features": {
    "llm_verdict": 1,
    "llm_confidence": 0.86,
    "doc_multi_lender_signal": 1,
    "...": "..."
  },
  "deterministic_features": {
    "doc_total_chars": 29,
    "doc_count": 1,
    "...": "..."
  },
  "document_conversion": [
    {"type": "salary_cert", "status": "provided", "cached": false, "error": null}
  ],
  "reasoning": "征信显示...",
  "model": "deepseek-flash",
  "cached": false
}
```

失败响应（HTTP 200，降级）：

```json
{
  "application_id": 1001,
  "status": "error",
  "error_code": "LLM_EXTRACTION_FAILED",
  "error": "特征抽取失败",
  "features": {"llm_feature_available": 0, "...": "其余全 0"},
  "deterministic_features": {"...": "..."},
  "document_conversion": [],
  "reasoning": "",
  "model": "deepseek-flash",
  "cached": false
}
```

**设计原则**：单条失败返回 200 + 降级特征，让业务继续流转；不把内部异常或材料原文返回给客户端。

### 3.3 错误码

| error_code | HTTP | 含义 |
|---|---:|---|
| `INVALID_PATH` | 400 | source_path 越界或非法 |
| `INVALID_INPUT` | 400 | 记录不符合输入契约 |
| `DOCUMENT_CONVERSION_FAILED` | 200 | 材料转换失败，已降级 |
| `LLM_EXTRACTION_FAILED` | 200 | 抽取失败，已降级 |
| `TIMEOUT` | 200 | 抽取超时，已降级 |
| 401 | 401 | API Key 缺失或错误 |
| 503 | 503 | 服务未配置好 API Key / 模型 |
| 413 | 413 | 请求体超过 1 MB |
| 422 | 422 | 请求 schema 校验失败 |

所有响应带 `X-Request-ID`，日志记录 request_id、application_id、status、error_code、耗时，不记录材料原文。

## 4. 安全措施

### 4.1 API Key

- 请求头 `X-API-Key`；
- 值来自 `.env` 的 `RISK_API_KEY`；
- 使用 `secrets.compare_digest` 比较，避免时序攻击；
- 未配置 Key 时服务返回 503，而不是放行；
- 本地演示默认 `classroom-demo-key`，生产必须替换。

### 4.2 PII 脱敏

在把文本发给 LLM 之前替换：

| 类型 | 正则 | 替换 |
|---|---|---|
| 身份证 | 18 位（末位可为 X）或 15 位 | `[身份证]` |
| 银行卡 | 16-19 位数字 | `[银行卡]` |
| 手机号 | `1[3-9]` 开头 11 位 | `[手机号]` |

顺序为身份证 → 银行卡 → 手机号，避免长数字被短规则截断。

**限制**：图片在 VLM 阶段已经上传，文本脱敏无法撤销图片中的 PII；
生产环境需要本地化模型、图片脱敏或供应商合规评估。

### 4.3 其他护栏

| 措施 | 实现 |
|---|---|
| 路径穿越 | 源文件必须位于 `RISK_DOC_BASE_DIR` 内 |
| 请求体大小 | 中间件限制 1 MB |
| 请求 ID | 每个响应带 `X-Request-ID` |
| 超时 | `RISK_REQUEST_TIMEOUT`，超时返回降级特征 |
| 错误信息 | 客户端只看到通用错误，细节写日志 |
| 标签注入 | 请求模型 `extra="forbid"`，不接受 `label` |
| 材料原文 | 日志和响应都不包含原文 |

### 4.4 LLM 特征值缓存

三层缓存：

| 层 | 缓存内容 | 命中后 |
|---|---|---|
| 文档缓存 | 源文件 → `.md` | 跳过 VLM |
| 特征值缓存 | 材料 + 上下文 → 特征 JSON | **跳过整个 LLM 调用** |
| 前缀缓存 | system prompt + 字段说明 | 只降低输入 token 费用 |

特征值缓存实现：

- 复用 `FileCache`，键由 `model + prompt 版本 + system prompt + user prompt` 计算；
- user prompt 已包含申报金额/期限/收入、材料文本、备注，任一变化都会 miss；
- 命中时直接返回缓存特征，响应 `cached=true`，不调用 LLM；
- 未命中时抽取成功并 `flush_cache()` 落盘；
- `FileCache` 增加线程锁，避免 API 线程池并发写坏文件；
- 配置 `RISK_FEATURE_CACHE_PATH=.cache/risk_features.json`，设为空字符串可关闭。

限制：

- 只做**精确匹配**，不做语义近似缓存；风控里相似材料可能结论不同；
- 缓存文件包含特征和 reasoning，按敏感数据管理；
- 目前没有 TTL，靠 model/prompt 版本和输入变化失效；生产建议用 Redis + TTL。

真实服务实测：

```text
首次请求：10.6 秒，cached=false
第二次相同请求：0.016 秒，cached=true
```

## 5. 配置

```dotenv
RISK_API_KEY=classroom-demo-key
RISK_DOC_BASE_DIR=data/llm_features/source_docs
RISK_REQUEST_TIMEOUT=30
RISK_MAX_WORKERS=4
RISK_REDACT_PII=true
RISK_FEATURE_CACHE_PATH=.cache/risk_features.json
```

生产环境把 `RISK_DOC_BASE_DIR` 改为 `./uploads`，并替换 `RISK_API_KEY`。

## 6. 运行

```bash
cd ml-risk-model
PYTHONIOENCODING=utf-8 python -m uvicorn llm_features.api.app:app \
  --host 127.0.0.1 --port 8000
```

```bash
curl http://127.0.0.1:8000/health

curl -X POST http://127.0.0.1:8000/v1/risk/features \
  -H "Content-Type: application/json" \
  -H "X-API-Key: classroom-demo-key" \
  -d '{"application_id":1,"documents":[{"type":"salary_cert","text":"收入稳定"}]}'
```

## 7. 测试

| 文件 | 用例数 | 覆盖点 |
|---|---:|---|
| `test_security.py` | 6 | 身份证/手机号/银行卡脱敏、短数字不误伤 |
| `test_api.py` | 16 | 健康检查、鉴权、PII、失败降级、超时、路径穿越、422、413、特征值缓存 |

真实服务冒烟：

- `/health` 返回 200；
- 无 Key / 错误 Key 返回 401；
- 正确 Key + 中文材料返回 200 和特征；
- 路径穿越返回 400 `INVALID_PATH`；
- 所有响应带 `X-Request-ID`。

## 8. 仍未覆盖

1. **Java 申请流接入**：Java 还没有调用这个服务；
2. **异步转换**：VLM 仍在请求内同步执行，未来应改队列 + 先降级打分；
3. **分布式锁/限流**：当前是单进程线程池，生产多实例需要 Redis/网关限流；
4. **图片 PII**：图片仍会发送给 VLM；
5. **监控告警**：只有日志，没有指标看板；
6. **灰度/回滚**：模型和 prompt 版本还没有完整的发布流程；
7. **L7 闭环/异步/数据闭环**：见 [L7 三重优化设计](2026-10-27-async-risk-pipeline-design.md)，代码未实现；
8. **超时语义**：`future.result(timeout=...)` 只保证请求快速返回，
   底层线程可能继续跑完并写入缓存；生产环境需要更彻底的取消机制。
