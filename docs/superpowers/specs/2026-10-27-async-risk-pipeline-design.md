# 风控 LLM 特征 L7 三重优化设计（同步闭环 / 异步编排 / 数据闭环）

- 日期：2026-10-27
- 状态：**设计，未实现**
- 关联：[在线服务与安全设计](2026-10-27-online-service-security-design.md)

## 0. L7 三层定位

L7 不是单纯的性能优化，而是把 LLM 特征真正接进 Java 主业务，分三步：

| 层 | 目标 | 关键动作 | 本文档 |
|---|---|---|---|
| **L7.1 同步闭环** | 让 Java 真正用上 LLM 特征 | Java → FastAPI → 结果落库 | 概要 |
| **L7.2 异步编排** | 让用户不用等 | `risk_job` + MQ + 多进程 VLM + SSE | 第 1-11 节详细 |
| **L7.3 数据闭环** | 证明到底有没有用 | `actual_result` + 真实消融 + 重训 | 概要 |

### L7.1 同步闭环（先接上）

```text
Java 提交申请
  → 组装：结构化特征 + 材料路径 + 申请上下文
  → POST /v1/risk/features
  → 拿到 G/H 特征
  → 合并成 full 特征向量
  → 加载 full XGBoost 预测
  → 写 risk_job / 结果表
  → 更新申请状态
```

必须先统一：

1. **特征 schema**：Java 线上 9 维 vs 训练侧约 58 维，需要统一列表、顺序、版本；
2. **输入组装**：Java 把 58 维和认证表路径组装成 Python 请求；
3. **结果落库**：写 `risk_job` 或独立结果表，不能只返回 HTTP 响应。

这一层完成后业务闭环成立，但用户仍需等待 VLM/LLM。

### L7.3 数据闭环（证明有用）

```text
ml_training_log.actual_result
  + risk_job.features_json
  + 结构化特征
  → 按时间切分
  → baseline / +LLM / +full 三组消融
  → 选特征集 → 重训 → 新模型版本 → 灰度
```

关键约束：

- 按时间切分，不能随机打乱；
- 特征只能用申请时点之前的数据；
- 如果真实数据上没有提升，就诚实报告“没有提升”；
- 保留 baseline 作为回滚。

## 1. L7.2 目标

把“提交申请”和“风控计算”解耦：

1. 用户提交后立即返回“计算中”，不等待 VLM/LLM；
2. 后台通过 MQ 编排材料转换、特征抽取、最终评分；
3. VLM 多进程并行，每份材料重试 2 次；
4. 最终只跑一次 full 模式 XGBoost，不展示中间分数；
5. 结果通过 MQ 回传 Java，复用现有 SSE/通知提示用户。

**非目标**：本文档不包含代码实现；Java 消费者、Python worker、数据库迁移均为后续工作。

## 2. 总体流程

```text
用户提交
  → Java 事务：loan_application + risk_job + Outbox(RISK_JOB_CREATED)
  → 返回 {application_id, job_id, status: 计算中}

Outbox → RabbitMQ(risk.job.created)
  → Python Worker:
      1. VLM 多进程并行转换（每份材料重试 2 次）
      2. sync 等待全部完成
      3. LLM 抽取 G 组
      4. 结构化 + G + H → merge
      5. XGBoost full 模式跑一次
      6. 发 RISK_JOB_COMPLETED
  → Java RiskResultConsumer:
      更新申请状态 + 写通知 + SSE 推送
```

## 3. `risk_job` 表

新增表，不改 `loan_application` 主干。

| 字段 | 类型 | 说明 |
|---|---|---|
| `job_id` | BIGINT PK | 任务 ID |
| `application_id` | BIGINT | 关联申请 |
| `user_id` | BIGINT | 关联用户 |
| `job_version` | INT | 任务版本，用于幂等/重跑 |
| `status` | VARCHAR(20) | 见状态机 |
| `stage` | VARCHAR(20) | 当前阶段，供前端展示 |
| `attempt` | INT | 当前重试次数 |
| `error_code` | VARCHAR(50) | 失败原因 |
| `score` | DECIMAL(6,5) | 最终 XGBoost 分数，仅 COMPLETED 写入 |
| `features_json` | JSON | 最终特征向量 |
| `feature_set` | VARCHAR(20) | 固定 `full` |
| `model_version` | VARCHAR(50) | XGBoost 版本 |
| `prompt_version` | VARCHAR(50) | LLM prompt 版本 |
| `converter_version` | VARCHAR(50) | 文档转换器版本 |
| `document_count` | INT | 材料总数 |
| `converted_count` | INT | 成功转换数 |
| `created_at` | DATETIME | 创建时间 |
| `started_at` | DATETIME | 开始处理 |
| `finished_at` | DATETIME | 完成时间 |
| `updated_at` | DATETIME | 更新时间 |

索引与约束：

- `UNIQUE(application_id, job_version)`：幂等；
- `INDEX(status, updated_at)`：队列/监控；
- `INDEX(user_id, created_at)`：用户查询。

### 状态机

```text
QUEUED → CONVERTING → EXTRACTING → SCORING → COMPLETED
                                      ↘ FAILED → MANUAL_REVIEW
```

- `QUEUED`：已入队；
- `CONVERTING`：VLM 多进程转换中；
- `EXTRACTING`：LLM 特征抽取中；
- `SCORING`：合并特征 + XGBoost；
- `COMPLETED`：最终分数已写入；
- `FAILED`：重试耗尽或不可恢复错误；
- `MANUAL_REVIEW`：转人工。

## 4. VLM 多进程并行

```text
Risk Worker（编排进程）
  submit → 进程 1: doc1  VLM（retry ≤ 2）
  submit → 进程 2: doc2  VLM（retry ≤ 2）
  submit → 进程 3: doc3  VLM（retry ≤ 2）
  ...
  sync 等待全部 futures
```

- 每个进程创建自己的 VLM 客户端；
- PDF 渲染/图片处理在子进程完成；
- 并发数 `VLM_WORKERS` 可配置，需低于 API 速率限制；
- 同步等待使用 `concurrent.futures.wait` + 总超时；
- 部分失败：用成功材料继续，记录 `converted_count < document_count`；
- 全部失败：`FAILED` → `MANUAL_REVIEW`。

**重试**：每份材料最多 3 次尝试（首次 + 2 次重试），指数退避 1s/2s。
只对超时、429、5xx 重试；文件类型/路径错误不重试。

## 5. 最终模型：full 模式

```text
结构化特征 + LLM(G) + H → 同一个 XGBoost → 最终概率 → 阈值
```

- 不在 VLM/LLM 之前先跑结构化 XGBoost；
- 不展示中间分数；
- `score` 只在 `COMPLETED` 写入；
- baseline / +LLM / +LLM+H 只用于离线消融，不是线上三个模型；
- 命名说明：代码里 `full` = 结构化 + G + H；`llm` = 结构化 + G。上线默认 `full`；
- LLM 失败时：G 填默认值 + `llm_feature_available=0`，仍进入同一个 full 模型；
  根据金额/风险阈值决定正常走还是转人工。

## 6. MQ 消息

| 事件 | 方向 | 内容 |
|---|---|---|
| `RISK_JOB_CREATED` | Java → Worker | `job_id, application_id, user_id, job_version, attempt` |
| `RISK_JOB_COMPLETED` | Worker → Java | `job_id, application_id, score, features, model_version, prompt_version` |
| `RISK_JOB_FAILED` | Worker → Java | `job_id, application_id, error_code, retryable` |

- MQ 消息只放 ID 和结果，不放材料原文/PII；
- 完成消息通过 Outbox 可靠发送；
- 失败重试后进 DLQ，最终转人工。

## 7. 用户通知

复用现有 Java 通知体系：

1. `RiskResultConsumer` 更新 `loan_application` 状态；
2. 写 `Notification`（Outbox）；
3. `NotificationSseService` 推送给在线用户；
4. 离线用户下次登录能在通知列表看到。

前端状态：

```text
提交 → 风控计算中
     → 材料识别中
     → 特征提取中
     → 评分完成（通过/拒绝/转人工）
```

## 8. 可靠性

| 问题 | 方案 |
|---|---|
| 消息重复 | `UNIQUE(application_id, job_version)` + Worker 先查状态 |
| Worker 崩溃 | 手动 ACK + 重试 + 指数退避 |
| 部分材料失败 | 继续用成功材料，记录 `converted_count` |
| 全部材料失败 | `FAILED` → `MANUAL_REVIEW` |
| 通知丢失 | Outbox + 通知表，SSE 只是推送通道 |
| 队列积压 | 优先级队列 + 限流 + 监控 |
| 重复扣费 | 完成状态先落库再 ACK；重试前查状态 |
| VLM 超时 | 单任务超时 + 总超时；进程无法强杀时标记失败 |

## 9. 性能估算

以每份材料 VLM 3 秒、每申请 5 份材料为例：

| 方式 | 转换耗时 |
|---|---:|
| 串行 | 15 秒 |
| 4 进程 | 约 6 秒 |
| 5 进程 | 约 3 秒 |

LLM 抽取约 3-5 秒，XGBoost 毫秒级。
用户提交后立即返回，感知等待为 0；后台完成后通知。

## 10. 实施顺序（后续）

1. 建 `risk_job` 表和状态枚举；
2. Java 提交申请写 Outbox + 入队；
3. Python Worker 消费 + Mock VLM/LLM 跑通；
4. Java `RiskResultConsumer` 更新状态 + 通知；
5. 接入真实 VLM 多进程 + 重试；
6. 接入 full 模型推理；
7. 幂等/重试/DLQ/监控测试。

## 11. 仍未覆盖

- L7.1 同步闭环、L7.3 数据闭环也均未实现；
- Java/Python 代码均未实现；
- 多进程池与 API 限流的配合未压测；
- 部分失败的降级策略需要业务确认；
- 队列积压、优先级、监控告警未做；
- 线上发布/回滚流程未定义。
