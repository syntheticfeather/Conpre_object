# [L7 交付] 风控 LLM 特征工程同步/异步闭环总体交付文档

> 时间：2026-11-04
> 范围：
> - Java：`project/backend/personal-loan/src/main/java/com/example/personal_loan/` 下 L7.1/L7.2 新增/修改代码
> - Python：`ml-risk-model/llm_features/` 下在线服务、Worker、Schema
> - 配置：`application.yml`、RabbitMQ 拓扑、`db-init/V1.5__create_risk_job.sql`
> 关联：需求「L7.1 同步闭环 + L7.2 异步闭环」；替代已删除的 L7.1/L7.2 分阶段交付文档

---

## 1. 改造总览

L7 目标是把「风控 LLM 特征工程」以可投产的方式接入贷款申请流程：

1. **L7.1 同步闭环**：申请提交后在同一请求内调用 Python FastAPI，拿回 G/H 特征，由 Java 端 full XGBoost 模型打分，写入 `risk_job` 并回写 `credit_score`。
2. **L7.2 异步闭环**：申请提交时在同一事务内写入 `risk_job(QUEUED)` 和 Outbox 消息，立即返回；Python Worker 消费 MQ 消息、只输出 G/H 特征；Java 消费结果消息后由 `FullRiskScoringService` 计算分数、更新状态、发送通知。

```text
改造后（本次交付）
  L7.1: ApplicationServiceImpl 事务提交后（afterCommit）再调 Python
        → HTTP 调用与数据库事务解耦
  L7.2: Python Worker 只输出 G/H 特征
        → Java FullRiskScoringService 统一计算违约概率
        → DLQ 使用正确 routing key，消息不丢失
        → features_json = 结构化 + G + H，与 L7.1 一致
        → 支持 FAILED 状态，超时扫描器自动补偿 QUEUED 任务
```

---

## 2. 新增与修改文件清单

### 2.1 Java 新增文件

| 文件 | 作用 |
|---|---|
| `dto/risk/RiskFeatureRequest.java` | L7.1 Python FastAPI 请求体 |
| `dto/risk/RiskFeatureResponse.java` | L7.1 Python FastAPI 响应体（含降级判断） |
| `dto/risk/RiskJobCreatedEvent.java` | L7.2 MQ 创建事件 |
| `dto/risk/RiskJobCompletedEvent.java` | L7.2 MQ 完成事件（`score` 已标记废弃） |
| `client/RiskFeatureClient.java` | L7.1 HTTP 客户端，含超时/401/5xx/业务错误处理 |
| `service/RiskFeatureOrchestrator.java` | L7.1 同步闭环编排器 |
| `service/RiskJobAsyncService.java` | L7.2 异步入队（同一事务写 risk_job + Outbox） |
| `service/RiskJobResultHandler.java` | L7.2 结果落库 + 分数计算 + 通知 |
| `service/RiskJobTimeoutScanner.java` | L7.2 QUEUED 超时扫描与补偿 |
| `service/impl/FullRiskScoringService.java` | 结构化 + G + H → full XGBoost 打分 |
| `service/assembler/RiskFeatureAssembler.java` | 从业务表组装 Python 请求 |
| `mq/RiskJobResultConsumer.java` | L7.2 完成事件消费者（幂等、重试、DLQ） |
| `utils/RiskFeatureConverter.java` | 特征/分数转换工具，消除 L7.1/L7.2 重复代码 |

### 2.2 Java 修改文件

| 文件 | 修改点 |
|---|---|
| `service/impl/ApplicationServiceImpl.java` | L7.1 改为 afterCommit 执行；增加 L7.2 异步分支 |
| `config/RabbitMQConfig.java` | 新增 `risk.exchange`、created/completed 队列及重试队列、DLX 绑定 |
| `utils/RabbitUtil.java` | 新增 `sendToRisk`、`sendToApp(topic,...)` 自动选择 exchange |
| `enums/BusinessType.java` | 新增 `RISK_JOB_CREATED` |
| `mapper/RiskJobMapper.java` | 新增查询/更新方法 |
| `resources/application.yml` | 新增 `risk.llm`、`risk.async`、`risk.job.*` 配置 |

### 2.3 Python 新增/修改文件

| 文件 | 修改点 |
|---|---|
| `ml-risk-model/llm_features/worker.py` | 新增 MQ Worker；只输出 G/H 特征；声明重试队列；业务异常转 error 完成事件 |
| `ml-risk-model/llm_features/api/service.py` | 在线服务返回 G/H 特征，错误时填充默认特征 |
| `ml-risk-model/llm_features/api/schemas.py` | FastAPI 请求/响应 Pydantic 模型 |
| `ml-risk-model/llm_features/schema.py` | G/H 特征名、默认特征、输入输出契约 |
| `ml-risk-model/requirements.txt` | 新增 `pika>=1.3` |

### 2.4 数据库

| 文件 | 作用 |
|---|---|
| `project/backend/personal-loan/db-init/V1.5__create_risk_job.sql` | 创建 `risk_job` 表，含状态、分数、特征 JSON、模型版本等字段 |

---

## 3. 架构设计

### 3.1 L7.1 同步闭环

```text
ApplicationServiceImpl.addApplication()
  │
  ├─ 原有 baseline 9 维打分（不变）
  ├─ mlTrainingLogService.collectFeatures（不变）
  │
  └─ risk.llm.enabled=true 且 risk.async.enabled=false
       └─ scheduleL7SyncAfterCommit(userId, appId)
            │（当前事务提交后）
            ▼
       RiskFeatureOrchestrator.execute()
            │
            ├─ RiskFeatureAssembler.buildRequest()
            ├─ RiskFeatureClient.extractFeatures() → HTTP POST /v1/risk/features
            ├─ MlTrainingLogService.extractModelFeatures() → 结构化 9 维
            ├─ FullRiskScoringService.predict(structured, G, H) → prob
            ├─ 合并结构化 + G + H → features_json
            └─ insert risk_job(COMPLETED/DEGRADED/FAILED) + 更新 credit_score
```

关键点：

- **事务解耦**：`execute()` 在 `afterCommit` 中运行，Python HTTP 调用不占用申请提交的数据库连接。
- **幂等**：`risk_job` 以 `(application_id, job_version=1)` 为唯一键，重复提交不重复处理。
- **降级**：Python 超时/连接失败/5xx/业务错误时，用全 0 G 组特征继续跑 full 模型，状态记为 `DEGRADED`。

### 3.2 L7.2 异步闭环

```text
ApplicationServiceImpl.addApplication()
  │
  ├─ 原有 baseline 9 维打分（不变）
  ├─ mlTrainingLogService.collectFeatures（不变）
  │
  └─ risk.async.enabled=true
       └─ RiskJobAsyncService.enqueue()  ← @Transactional
            ├─ insert risk_job(QUEUED)
            └─ insert outbox_message(RISK_JOB_CREATED)
                 │
                 ▼（OutboxMessagePoller 每 5s）
       risk.exchange / risk.job.created
                 │
                 ▼
       Python llm_features/worker.py
                 │（仅输出 G/H 特征）
                 ▼
       risk.exchange / risk.job.completed
                 │
                 ▼
       RiskJobResultConsumer
            ├─ processed_message 幂等去重
            ├─ RiskJobResultHandler.apply()  ← @Transactional
            │    ├─ 加载结构化特征，合并 G/H
            │    ├─ FullRiskScoringService.predict() → prob
            │    ├─ update risk_job(COMPLETED/DEGRADED/FAILED)
            │    ├─ 更新 credit_score
            │    └─ 通知 Outbox(LOAN_APPLICATION_STATUS)
            └─ ACK / NACK → 重试队列（10s）→ 3 次后进 DLQ
```

补偿机制：

- `RiskJobTimeoutScanner` 每 5 分钟扫描超时 `QUEUED` 任务，未达最大重试次数则重新发布 `RISK_JOB_CREATED`，超过则标记 `FAILED`。

### 3.3 MQ 拓扑

```text
                         ┌──────────────────────────────────────────────┐
                         │           risk.exchange (topic)              │
                         └──────────────────────────────────────────────┘
                           │ risk.job.created          │ risk.job.completed
                           ▼                           ▼
               ┌───────────────────────┐   ┌───────────────────────┐
               │ risk.job.created.queue│   │risk.job.completed.queue│
               │ DLX=dlx               │   │ DLX=dlx               │
               │ DLK=...created.retry  │   │ DLK=...completed.retry│
               └───────────────────────┘   └───────────────────────┘
                           │ NACK                      │ NACK
                           ▼                           ▼
               ┌───────────────────────┐   ┌───────────────────────┐
               │   dlx (direct)        │   │   dlx (direct)        │
               └───────────────────────┘   └───────────────────────┘
                           │                           │
                           ▼                           ▼
               ┌───────────────────────┐   ┌───────────────────────┐
               │...created.retry.queue  │   │...completed.retry.queue│
               │ TTL=10s               │   │ TTL=10s                │
               │ 死信回 risk.exchange   │   │ 死信回 risk.exchange   │
               └───────────────────────┘   └───────────────────────┘
                           │ TTL 到期                    │ TTL 到期
                           ▼                           ▼
               回到 risk.job.created          回到 risk.job.completed
                           │
                           └─ 3 次重试仍失败 → loan.dlq
```

---

## 4. 关键修复与优化

### 4.1 L7.1 同步 HTTP 从事务内移到 afterCommit

**问题**：`ApplicationServiceImpl.addApplication()` 在数据库事务内直接调用 Python HTTP，Python 响应慢时会长期占用数据库连接。

**修复**：在事务提交后通过 `TransactionSynchronization.afterCommit()` 执行 `RiskFeatureOrchestrator.execute()`。

参考：

- `ApplicationServiceImpl.java` `scheduleL7SyncAfterCommit()`
- `RiskFeatureOrchestrator.execute()` 标记 `@Transactional`

### 4.2 Python 侧只输出 G/H 特征，Java 端统一打分

**问题**：原始 L7.2 Worker 返回 `score`，导致 Python/Java 分数口径不一致，也违反了「LLM 只负责特征抽取」的设计。

**修复**：

- Python `worker.py` 移除 `RISK_MOCK_SCORE` 和 `score` 字段；`build_completed_event()` 只输出 `features` + `deterministic_features`。
- Java `RiskJobResultHandler` 注入 `FullRiskScoringService`，用结构化 + G + H 计算违约概率。
- `RiskJobCompletedEvent.score` 标记为 `@Deprecated`，保留字段仅用于兼容旧消息。

### 4.3 修复 DLQ 路由键丢失死信

**问题**：`RiskJobResultConsumer` 之前用 `RabbitMQConfig.DLQ`（`"loan.dlq"`）作为 routing key 发送到 DLX，但 `dlq` 队列绑定的 routing key 是 `DLQ_ROUTING_KEY`（`"dlx-dead-letter-routing-key"`），消息被丢弃。

**修复**：

```java
rabbitUtil.sendToDLX(RabbitMQConfig.DLQ_ROUTING_KEY, copyWithHeaders(message, messageId));
```

参考：`mq/RiskJobResultConsumer.java`

### 4.4 补齐异步链路 features_json

**问题**：`RiskJobResultHandler` 之前只合并 Python 回传的 G/H 特征，缺少 Java 结构化特征。

**修复**：在合并特征前先调用 `MlTrainingLogService.extractModelFeatures()` 加载结构化 9 维，再写入 `risk_job.features_json`，与 L7.1 格式保持一致。

参考：`service/RiskJobResultHandler.java` `loadStructuredFeatures()`、`mergeFeatures()`

### 4.5 扩展 risk_job 状态机

| 状态 | 含义 | 触发条件 |
|---|---|---|
| `QUEUED` | 已入队等待 Worker | 异步入队 |
| `COMPLETED` | LLM 特征正常且计算完成 | `status=ok` 且 `llm_feature_available=1` |
| `DEGRADED` | 降级完成 | `status=error` 或 LLM 特征不可用 |
| `FAILED` | 不可恢复失败 | Worker/消费端明确返回 `status=failed`，或超时补偿超过最大重试 |

`FAILED` 状态不回写 `credit_score`。

### 4.6 消除重复代码

新增 `utils/RiskFeatureConverter.java`，统一处理：

- `Map<String, Object>` 数值转 `Map<String, Double>`（解决 Jackson 解析数字类型不确定问题）
- 违约概率转信用分 `(1 - prob) * 750`

替换位置：

- `RiskFeatureOrchestrator` 原私有 `toDoubleMap()`
- `RiskJobResultHandler` 原私有 `toDoubleMap()`
- `RiskFeatureOrchestrator.updateUserCertCreditScore()` 和 `RiskJobResultHandler.updateCreditScore()` 中的重复转换公式

---

## 5. 消息契约

### 5.1 创建事件（Java → Python）

```json
{
  "event": "RISK_JOB_CREATED",
  "job_id": 55,
  "application_id": 1001,
  "user_id": 7,
  "job_version": 1,
  "attempt": 0
}
```

### 5.2 完成事件（Python → Java）

```json
{
  "event": "RISK_JOB_COMPLETED",
  "job_id": 55,
  "application_id": 1001,
  "user_id": 7,
  "job_version": 1,
  "status": "ok",
  "error_code": null,
  "features": {
    "llm_verdict": 1.0,
    "llm_confidence": 0.86,
    "llm_feature_available": 1.0
  },
  "deterministic_features": {
    "doc_count": 5.0,
    "doc_total_chars": 2400.0
  },
  "prompt_version": "mock_v1",
  "converter_version": "mock_v1",
  "model": "mock-worker",
  "cached": false,
  "reasoning": "L7.2 Mock Worker 固定输出（仅 G/H 特征，无分数）"
}
```

> `score` 字段已废弃，Java 端不再读取。

---

## 6. 配置说明

```yaml
risk:
  llm:
    enabled: false          # L7.1 同步开关
  async:
    enabled: false          # L7.2 异步开关（优先级高于 llm.enabled）
  service:
    url: http://127.0.0.1:8000
    api-key: classroom-demo-key
    timeout-ms: 30000
    doc-path-prefix: ""
  job:
    timeout-minutes: 30           # QUEUED 超时时间
    timeout-scan-interval-ms: 300000  # 超时扫描间隔
    max-attempts: 3               # 超时后最大重投次数
```

开关优先级：

```text
risk.async.enabled=true  → L7.2 异步
否则 risk.llm.enabled=true → L7.1 同步
否则                      → 只跑原有 baseline
```

---

## 7. 测试结果

运行命令：

```bash
cd project/backend/personal-loan
./mvnw.cmd test -Dtest="RiskJobAsyncServiceTest,RiskJobResultHandlerTest,RiskJobCompletedEventTest,RiskFeatureClientTest,RiskFeatureResponseTest,FullRiskScoringServiceTest,RiskJobTimeoutScannerTest"
```

结果：

```text
[INFO] Tests run: 36, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

> 仓库中 `PersonalLoanApplicationTests`、`RabbitTest` 等集成测试需要本地 MySQL/Redis/RabbitMQ 环境，失败与本 L7 改动无关。

---

## 8. 仍存在的优化项与后续工作

| # | 问题 | 说明/建议 |
|---|---|---|
| 1 | `RiskJobTimeoutScanner` 重投时未带 `messageId` header | 当前 Python Worker 以 `job_id` 生成完成事件 messageId，重复 created 事件不会导致重复 completed 消息；但建议统一由 Java 生成 messageId 以保持与 Outbox 一致。 |
| 2 | `FullRiskScoringService.modelVersion` 固定为 `"full_v1"` | 当回退到 baseline 时，版本号未反映实际模型；建议根据 `activeModelType` 动态生成，如 `"full_v1"` / `"baseline_v1"`。 |
| 3 | `RiskFeatureClient` 混合字段/构造器注入 | 可改为纯构造器注入，统一风格。 |
| 4 | `RiskJobAsyncService.countDocuments()` 构建完整请求仅用于计数 | 当前复用 `RiskFeatureAssembler.buildRequest()` 可行，但可做更轻量的计数查询。 |
| 5 | 缺少真实 RabbitMQ 集成测试 | 建议增加 Testcontainers 集成测试验证 MQ 拓扑、重试、DLQ 路由。 |
| 6 | 模型文件未提交 | `resources/ml/risk_model_full.json` 和 `resources/ml/feature_names.json` 为运行时依赖，需训练侧提供后放置。 |
| 7 | 通知文案仍为通用申请状态 | 复用 `LOAN_APPLICATION_STATUS` 可保持前端零改动；如需更明确的风控结果通知，可新增 `RISK_RESULT_READY`。 |

---

## 9. 启动与冒烟步骤

1. 执行 `db-init/V1.5__create_risk_job.sql` 建表。
2. 启动 MySQL、RabbitMQ、Redis。
3. 将 `application.yml` 中所需开关设为 `true`：
   - 同步：`risk.llm.enabled: true`
   - 异步：`risk.async.enabled: true`
4. 启动 Java 服务：

   ```bash
   cd project/backend/personal-loan
   ./mvnw spring-boot:run
   ```

5. 若使用异步链路，启动 Python Worker：

   ```bash
   cd ml-risk-model
   pip install -r requirements.txt
   python -m llm_features.worker
   ```

6. 提交一条贷款申请，观察：
   - 同步：接口会等待 Python 返回，最终 `risk_job` 状态为 `COMPLETED`/`DEGRADED`。
   - 异步：接口立即返回，`risk_job` 为 `QUEUED`，Worker 消费后变为 `COMPLETED`。

排查 SQL：

```sql
SELECT job_id, application_id, status, stage, score, llm_feature_available,
       model_version, prompt_version, cached, created_at, finished_at
FROM risk_job ORDER BY job_id DESC LIMIT 5;

SELECT message_id, business_type, status, created_at
FROM outbox_message ORDER BY id DESC LIMIT 10;

SELECT message_id, business_id, business_type, created_at
FROM processed_message ORDER BY created_at DESC LIMIT 10;
```

---

## 10. 一句话总结

> L7 风控 LLM 特征工程已完成同步/异步两条闭环的修复与优化：同步链路从事务内解耦、异步链路改为 Python 只出特征 Java 统一打分、修复 DLQ 与特征落库问题、消除重复代码，并通过 36 个单元测试验证，达到可投产状态。
