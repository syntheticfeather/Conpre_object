# ML 风控系统 — 完整 Spec

> 版本: 2.0 | 日期: 2026-06-11 | 状态: 已实现

---

## 一、架构总览

```
用户提交申请
    │
    ├── 1. extractModelFeatures()     → 9 个 XGBoost 特征
    ├── 2. countRealFeatures()        → 质量门槛（<3 → 0 分拒绝）
    ├── 3. XGBoost.predict()          → 违约概率
    ├── 4. toCreditScore(prob)        → 0-750 信用分
    ├── 5. UPDATE user_certification   → credit_score
    ├── 6. collectFeatures()          → 58 特征 → ml_training_log
    │
    └── [异步] AICheck()
         ├── 读 credit_score
         ├── ≥600 → 通过 → 创建订单
         ├── 400-599 → 转人工
         └── <400 → 拒绝

还款完成/逾期确认
    ├── PaymentSuccessConsumer → markCompleted(0)
    └── OrderServiceImpl.checkOverdue → markDefaulted(1)
```

## 二、特征体系

### XGBoost 模型特征（9个）

用于打分，从 GMS 数据集训练：
`age`, `monthly_income`, `dti_ratio`, `active_loan_count`, `has_mortgage`, `max_overdue_days_90`, `overdue_30_59_count`, `overdue_60_89_count`, `credit_card_usage_pct`

### 全量采集特征（58个）

写入 `ml_training_log.features` JSON，用于 Phase 2 自研模型训练。详见 `ml-risk-model/feature_config.py`。

## 三、评分映射

```
credit_score = round((1 - 违约概率) × 750)，范围 0-750

违约概率     信用分     决策
─────────────────────────────────
0%          750        通过
5%          713        通过
10%         675        通过
20%         600  ←───  通过线
30%         525        转人工
47%         398        拒绝线
100%        0          拒绝
```

配置: `application.yml` → `approval.score.pass=600`, `approval.score.reject=400`

## 四、数据质量门槛

9 个模型特征中统计"真实值"个数（非均值/默认填充）：

| 特征 | 真实判断标准 |
|------|-------------|
| age | id_card 存在且 ≠ 30（默认值） |
| has_mortgage | immovables_cert 存在 |
| active_loan_count | 有 orders 记录 |
| dti_ratio | 有 orders 记录 |
| max_overdue_days_90 | 有逾期记录 |
| overdue_30_59_count | 有逾期记录 |
| overdue_60_89_count | 有逾期记录 |
| monthly_income | 永远不真实（DB 无此字段） |
| credit_card_usage_pct | 永远不真实（DB 无此字段） |

`realFeatures < 3` → credit_score = 0（直接拒绝）

## 五、数据库变更

### V1.3: ml_training_log 表

```sql
CREATE TABLE ml_training_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    application_id INT NOT NULL,
    features JSON NOT NULL,
    model_version VARCHAR(20),
    model_score DECIMAL(5,4),
    actual_result TINYINT NULL,
    result_at DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (application_id) REFERENCES loan_applications(id)
);
```

### V1.4: orders 表增加 application_id

```sql
ALTER TABLE orders ADD COLUMN application_id INT NULL AFTER user_id;
```

## 六、Java 文件清单

### 新建文件

| 文件 | 说明 |
|------|------|
| `entity/MlTrainingLog.java` | 采集日志实体 |
| `mapper/MlTrainingLogMapper.java` | MyBatis Mapper |
| `service/MlTrainingLogService.java` | 接口 |
| `service/impl/MlTrainingLogServiceImpl.java` | **核心** — 特征采集（350行） |
| `service/RiskScoringService.java` | XGBoost 打分接口 |
| `service/impl/RiskScoringServiceImpl.java` | XGBoost 加载+预测+质量门槛 |
| `resources/ml/risk_model.json` | GMS 训练模型文件 |
| `db-init/V1.3__create_ml_training_log.sql` | DDL |
| `db-init/V1.4__add_application_id_to_orders.sql` | DDL |

### 修改文件

| 文件 | 变更 |
|------|------|
| `pom.xml` | 加 xgboost4j_2.13:3.2.0 |
| `application.yml` | 加 approval.score.pass/reject |
| `entity/Order.java` | 加 applicationId |
| `mapper/OrderMapper.java` | SELECT/INSERT 加 application_id |
| `ApplicationServiceImpl.java` | 打分流程：提取特征→门槛→XGBoost→写 credit_score |
| `AIApproveServiceImpl.java` | Order 构造加 applicationId |
| `ManualApproveServiceImpl.java` | Order 创建加 applicationId |
| `MockCreditScoreCalculator.java` | 重写为读 credit_score（不再随机） |
| `PaymentSuccessConsumer.java` | 注入 MlTrainingLogService，订单完成补标签 |
| `OrderServiceImpl.java` | 注入 MlTrainingLogService，逾期≥90天补标签 |
| `ml-risk-model/feature_config.py` | 扩展到 58 特征 |

### 测试文件

| 文件 | 用例数 | 说明 |
|------|--------|------|
| `MlTrainingLogServiceTest.java` | 4 | 特征采集、补标签、异常处理 |
| `RiskScoringServiceTest.java` | 12 | 质量门槛、分数映射、模型推理 |

## 七、Phase 2 → Phase 3 规划

| 时间点 | 里程碑 |
|--------|--------|
| 现在 | 打分链路就绪，开始积累数据 |
| 1 个月 | ~500 条申请 + 部分标签 |
| 3 个月 | ~2000 条 → 可初步验证模型准确率 |
| 6 个月 | ~5000 条带标签 → 训练自研模型，替换 GMS |

## 八、待做

- [ ] 策略引擎：分数→等级→额度/利率
