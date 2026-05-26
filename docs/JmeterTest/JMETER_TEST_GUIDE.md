# Personal-Loan JMeter 高并发测试指南

## 一、测试计划概述

本测试计划针对个人贷款系统的核心业务流程进行高并发测试，覆盖以下场景：

| 场景 | 并发用户数 | 持续时间 | 核心接口 |
|------|-----------|---------|---------|
| 登录场景 | 100 | 一次性 | `/api/auth/login` |
| 贷款申请场景 | 200 | 30分钟 | `/api/loan-applications` |
| 还款场景 | 150 | 30分钟 | `/api/orders/{id}/repay` |
| 综合业务场景 | 200 | 60分钟 | 完整业务流程 |

## 二、消息队列架构

根据代码分析，系统使用 RabbitMQ 实现异步消息处理：

```
┌────────────────────────────────────────────────────────────────────┐
│                        消息队列架构                                 │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  贷款申请 → Outbox → loan.application.queue → AI审核              │
│                                                → Notification     │
│                                                                    │
│  还款请求 → Outbox → payment.requested.queue → PayService         │
│                                                → payment.success.queue → 订单更新 │
│                                                                    │
│  重试机制: 主队列 → 重试队列(10s TTL) → 主队列                      │
│  死信机制: 超过3次重试 → DLQ                                        │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

## 三、环境准备

### 3.1 前置条件

1. **JMeter 安装**：建议使用 JMeter 5.6+ 版本
2. **测试数据**：执行 `jmeter-test-data.sql` 初始化测试数据
3. **服务启动**：确保以下服务正常运行
   - MySQL 数据库
   - Redis 缓存
   - RabbitMQ 消息队列
   - 个人贷款应用服务

### 3.2 数据库配置

```bash
# 执行测试数据脚本
mysql -u admin -p1234 person-loan < jmeter-test-data.sql
```

### 3.3 JMeter 插件安装

建议安装以下插件：

- **PerfMon Metrics Collector**：用于监控服务器性能指标
- **Backend Listener**：用于将数据发送到 InfluxDB/Grafana

## 四、测试执行

### 4.1 非GUI模式执行（推荐）

```bash
# 执行完整测试计划
jmeter -n -t jmeter-test-plan.jmx -l test-results.jtl -e -o report

# 指定测试场景执行
jmeter -n -t jmeter-test-plan.jmx -l results.jtl -JthreadGroup=2

# 自定义并发数
jmeter -n -t jmeter-test-plan.jmx -l results.jtl -JnumThreads=300 -JrampUp=60
```

### 4.2 GUI模式执行（调试用）

```bash
jmeter -t jmeter-test-plan.jmx
```

### 4.3 分布式测试（大规模压测）

```bash
# 启动远程服务器
jmeter-server -Djava.rmi.server.hostname=192.168.1.100

# 主控端执行
jmeter -n -t jmeter-test-plan.jmx -R192.168.1.100,192.168.1.101 -l results.jtl
```

## 五、测试计划结构详解

### 5.1 线程组设计

| 线程组名称 | 线程数 | 启动时间 | 持续时间 | 延迟启动 |
|-----------|--------|---------|---------|---------|
| 登录场景 | 100 | 30s | - | - |
| 贷款申请场景 | 200 | 60s | 30分钟 | 60s |
| 还款场景 | 150 | 45s | 30分钟 | 120s |
| 综合业务场景 | 200 | 60s | 60分钟 | 180s |

### 5.2 核心测试逻辑

**登录场景**：

1. 调用登录接口获取 Token
2. 使用 JSON Extractor 提取 accessToken
3. 将 Token 保存到全局变量供后续场景使用

**贷款申请场景**：

1. 使用随机参数生成贷款申请
2. 验证申请提交成功
3. 查询申请列表验证数据一致性

**还款场景**：

1. 查询用户订单列表
2. 提取第一个订单ID
3. 执行还款操作
4. 查询还款计划验证还款成功

**综合业务场景**：

- 使用 Transaction Controller 模拟完整业务流程
- 使用 Constant Throughput Timer 控制吞吐量为 100/min

### 5.3 参数化配置

| 参数名 | 来源 | 说明 |
|--------|------|------|
| phone | CSV文件或随机生成 | 用户手机号 |
| productId | __Random(1,5) | 产品ID |
| optionId | __Random(1,10) | 产品选项ID |
| term | __Random(3,24) | 贷款期数 |
| loanAmount | __Random(1000,50000) | 贷款金额 |

## 六、性能监控

### 6.1 JMeter 内置监控

```bash
# 生成HTML报告
jmeter -g test-results.jtl -o html-report

# 生成CSV报告
jmeter -g test-results.jtl -f -o csv-report
```

### 6.2 服务器端监控

**Linux 服务器监控**：

```bash
# CPU监控
mpstat 5

# 内存监控
free -h

# 磁盘IO监控
iostat 5

# 网络监控
netstat -s

# RabbitMQ队列监控
rabbitmqctl list_queues name messages consumers
```

**Windows 服务器监控**：

```powershell
# CPU使用率
Get-Counter '\Processor(_Total)\% Processor Time'

# 内存使用
Get-Counter '\Memory\Available MBytes'

# 磁盘IO
Get-Counter '\PhysicalDisk(*)\Disk Read Bytes/sec'
```

### 6.3 数据库监控

```sql
-- MySQL连接数
SHOW STATUS LIKE 'Threads_connected';

-- 慢查询日志
SET GLOBAL slow_query_log = 'ON';

-- 查看正在执行的查询
SHOW FULL PROCESSLIST;
```

## 七、测试指标收集

### 7.1 关键性能指标

| 指标 | 目标值 | 说明 |
|------|-------|------|
| 响应时间(P95) | ≤500ms | 95%请求响应时间 |
| 吞吐量 | ≥1000 TPS | 系统处理能力 |
| 错误率 | ≤1% | 失败请求比例 |
| CPU使用率 | ≤70% | 服务器CPU负载 |
| 内存使用率 | ≤80% | 服务器内存负载 |
| 消息队列堆积 | ≤1000 | 队列消息积压 |

### 7.2 消息队列指标

| 指标 | 监控方式 | 说明 |
|------|---------|------|
| 队列长度 | rabbitmqctl | 当前队列消息数 |
| 消费速率 | RabbitMQ管理界面 | 每秒消费消息数 |
| 死信数量 | DLQ队列 | 无法处理的消息 |
| 重试次数 | x-death header | 消息重试次数 |

## 八、测试结果分析

### 8.1 响应时间分析

```bash
# 从结果文件提取响应时间统计
awk -F',' '{sum+=$2; min=$2; max=$2} END {print "平均响应时间:", sum/NR, "ms"}' test-results.jtl
```

### 8.2 吞吐量计算

```bash
# 计算TPS
grep "200" test-results.jtl | wc -l | awk '{print "TPS:", $1/3600}'
```

### 8.3 错误分析

```bash
# 统计错误类型
grep -E "(500|400|403)" test-results.jtl | cut -d',' -f1 | sort | uniq -c
```

## 九、调优建议

### 9.1 消息队列调优

```yaml
# application.yml 调优配置
spring:
  rabbitmq:
    listener:
      simple:
        concurrency: 10      # 增加消费者并发数
        max-concurrency: 50  # 最大并发数
        prefetch: 10         # 预取数量
```

### 9.2 数据库连接池调优

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      idle-timeout: 300000
      connection-timeout: 20000
```

### 9.3 JVM调优

```bash
# JVM启动参数
java -Xms4g -Xmx8g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 \
     -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof \
     -jar personal-loan.jar
```

## 十、安全注意事项

1. **测试环境隔离**：确保测试环境与生产环境物理隔离
2. **数据脱敏**：测试数据使用脱敏数据，避免真实用户信息
3. **访问控制**：限制 JMeter 客户端IP访问权限
4. **测试时段**：避免在业务高峰期执行压测
5. **资源限制**：设置合理的并发数，避免过度消耗资源

## 附录：常用命令

```bash
# 查看JMeter版本
jmeter -v

# 生成测试报告
jmeter -g results.jtl -o report

# 启动远程服务器
jmeter-server

# 停止JMeter进程
pkill -f jmeter
```

---

**文档版本**：v1.0  
**创建时间**：2026年5月  
**适用场景**：个人贷款系统高并发性能测试