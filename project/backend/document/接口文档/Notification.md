# 消息通知接口

## 获取所有消息

**网址** /api/notifications/my

**请求方式** GET

**返回数据**

``` json
{
    "code": 200,
    "data": [
        {
            "id": 4,
            "userId": 8,
            "businessId": 1,
            "businessType": "REPAYMENT",
            "title": "还款成功",
            "content": "订单(1)已完成第2期还款",
            "readFlag": false,
            "createdAt": "2026-04-03 17:32:26",
            "readAt": null
        },
        {
            "id": 3,
            "userId": 8,
            "businessId": 1,
            "businessType": "REPAYMENT",
            "title": "还款成功",
            "content": "订单(1)已完成第1期还款",
            "readFlag": false,
            "createdAt": "2026-04-03 17:25:39",
            "readAt": null
        },
        {
            "id": 2,
            "userId": 8,
            "businessId": 1,
            "businessType": "LOAN_APPLICATION",
            "title": "贷款申请已通过",
            "content": "您的贷款申请(1)已通过",
            "readFlag": false,
            "createdAt": "2026-04-03 17:25:03",
            "readAt": null
        },
        {
            "id": 1,
            "userId": 8,
            "businessId": 1,
            "businessType": "LOAN_APPLICATION",
            "title": "贷款申请已提交",
            "content": "您的贷款申请(1)已提交，正在审核中",
            "readFlag": false,
            "createdAt": "2026-04-03 17:24:58",
            "readAt": null
        }
    ],
    "message": "操作成功"
}
```

**postman测试结果**

![](../NotificationImgs/getMy.png.png "获取所有消息")

## 标记通知已读

**网址** /api/notifications/{notificationId}/read

**请求方式** PATCH

**参数**

| 参数 | 描述 | 类型 | 是否必填 |
| ---- | ---- | ---- | ---- |
| notificationId | 通知ID | Long | 是 |

**请求示例（网址）** /api/notifications/4/read

**返回数据**

``` json
{
    "code": 200,
    "data": "已标记通知为已读",
    "message": "操作成功"
}
```

**postman测试结果**
![](../NotificationImgs/read.png "标记通知已读")

## 实时通知流

**网址** /api/notifications/stream

**请求方式** GET

**响应数据格式** SSE 需要监听 message 事件

Content-Type: text/event-stream  
数据格式: JSON 字符串

**示例推送数据**：

``` json
{
    "id": 5,
    "userId": 8,
    "businessId": 2,
    "businessType": "LOAN_APPLICATION",
    "title": "贷款申请已提交",
    "content": "您的贷款申请(2)已提交，正在审核中",
    "readFlag": false,
    "createdAt": "2026-04-03 18:14:25",
    "readAt": null
}
```

**postman测试结果**
![](../NotificationImgs/stream.png "实时通知流")
![](../NotificationImgs/streamMessage.png "通知消息示例")
