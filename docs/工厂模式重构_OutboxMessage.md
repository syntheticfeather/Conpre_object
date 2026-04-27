# OutboxMessage 工厂模式重构

## 修改时间
2026/04/27

## 修改背景

原有 `OutboxMessage` 创建代码散落在多处，存在以下问题：
1. **代码重复**：5处创建逻辑高度相似，各有10行左右代码
2. **硬编码**：messageId前缀、businessType、topic 等字符串散落各处
3. **维护困难**：新增业务类型需要修改多处代码

## 重构方案

### 1. 新增 BusinessType 枚举类

路径：`enums/BusinessType.java`

```java
public enum BusinessType {
    LOAN_APPLICATION("loan_app_", "LOAN_APPLICATION", RabbitMQConfig.LOAN_APPLICATION_ROUTING_KEY),
    PAYMENT_REQUESTED("payment_requested_", "PAYMENT_REQUESTED", RabbitMQConfig.PAYMENT_REQUESTED_ROUTING_KEY),
    NOTIFICATION("notif_", "NOTIFICATION", RabbitMQConfig.NOTIFICATION_ROUTING_KEY),
    PAYMENT_SUCCESS("payment_success_", "PAYMENT_SUCCESS", RabbitMQConfig.PAYMENT_SUCCESS_ROUTING_KEY),
    ;
    // ...
}
```

### 2. 新增 OutboxMessageFactory 工厂类

路径：`factory/OutboxMessageFactory.java`

```java
public OutboxMessage create(BusinessType type, Object businessObject, Long businessId) {
    OutboxMessage outbox = new OutboxMessage();
    String messageId = type.getMessageIdPrefix() + businessId + "_" + System.currentTimeMillis();
    outbox.setMessageId(messageId);
    outbox.setBusinessType(type.getBusinessType());
    outbox.setBusinessId(businessId);
    outbox.setTopic(type.getTopic());
    outbox.setPayload(objectMapper.writeValueAsString(businessObject));
    outbox.setStatus("PENDING");
    outbox.setCreatedAt(LocalDateTime.now());
    return outbox;
}
```

## 修改文件

| 文件 | 改动内容 |
|------|---------|
| `enums/BusinessType.java` | **新建** - 枚举类 |
| `factory/OutboxMessageFactory.java` | **新建** - 工厂类 |
| `service/impl/ApplicationServiceImpl.java` | 注入工厂，outbox创建从14行简化为1行 |
| `service/impl/OrderServiceImpl.java` | 注入工厂，outbox创建从12行简化为1行 |
| `mq/NotificationOutboxPublisher.java` | 注入工厂，两处outbox创建各从10行简化为1行 |
| `mq/PaymentRequestedConsumer.java` | 注入工厂，outbox创建从9行简化为1行 |

## 效果对比

### Before
```java
OutboxMessage outbox = new OutboxMessage();
outbox.setMessageId("payment_requested_" + order.getId() + "_" + System.currentTimeMillis());
outbox.setBusinessType("PAYMENT_REQUESTED");
outbox.setBusinessId(order.getId());
outbox.setTopic(RabbitMQConfig.PAYMENT_REQUESTED_ROUTING_KEY);
try {
    outbox.setPayload(objectMapper.writeValueAsString(event));
} catch (Exception ex) {
    throw new BusinessException(500, "消息序列化失败");
}
outbox.setStatus("PENDING");
outbox.setCreatedAt(LocalDateTime.now());
outboxMapper.insert(outbox);
```

### After
```java
OutboxMessage outbox = outboxMessageFactory.create(BusinessType.PAYMENT_REQUESTED, event, order.getId());
outboxMapper.insert(outbox);
```

## 新增业务类型

以后新增 `OutboxMessage` 业务类型，只需：

1. 在 `BusinessType` 枚举添加一行
2. 调用处使用工厂：

```java
outboxMapper.insert(outboxMessageFactory.create(BusinessType.新类型, 业务对象, 业务ID));
```

**无需修改已有代码**。

## 设计模式总结

| 模式 | 应用场景 |
|------|---------|
| **工厂模式** | 封装复杂对象创建逻辑，统一创建入口 |
| **枚举+常量** | 消除硬编码字符串，统一管理业务类型配置 |

## 后续优化建议

1. **异常处理优化**：工厂内部捕获序列化异常，可选择抛出业务异常或设置FAILED状态
2. **消息ID生成策略**：可考虑使用UUID替代时间戳+ID的方式，避免并发重复
