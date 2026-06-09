# 用户接口文档

## 用户接口

**网址** localhost:8080/api/chat

**请求头**  

| 名称 | 值 | 描述 |
| --- | --- | --- |
| Content-Type | application/json | 请求体为 JSON 格式 |
| Authorization | Bearer token | 认证令牌 |

**请求方式** POST

**请求参数说明**  

| 参数名 | 类型 | 描述 | 示例 |
| --- | --- | --- | --- | --- |
| message | string | 用户输入的问题 | "有哪些产品推荐" |
| sessionId | string | 会话ID，初始可以为空 | "" |
| agentMode | string | 智能体范式，默认react，可以不填 | "react" |

**请求示例1（请求体）**  

```json
{
  "message": "有哪些产品推荐",
  "sessionId": "",
  "agentMode": "react"
}
```

**测试结果**  

![返回内容（workflow）](../ChatImgs/queryProducts.png)
