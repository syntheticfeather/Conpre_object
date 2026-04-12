# 贷款订单接口说明

以下接口请求时都需携带请求头，示例：

| 字段 | 值  | 说明 |
| --   | -- | -- |
| Authorization | Bearer token | 需携带有效token |

## 用户获取订单列表

**网址** /api/orders/my

**请求方式** GET

**返回数据**

```json
{
    "code": 200,
    "data": [
        {
            "id": 3,
            "loanAmount": 30000.00,
            "status": "已逾期",
            "startTime": "2025-12-10 16:59:47",
            "term": 24,
            "currentTerm": 0,
            "overdueDays": 1
        },
        {
            "id": 2,
            "loanAmount": 30000.00,
            "status": "正常",
            "startTime": "2025-12-10 16:57:50",
            "term": 24,
            "currentTerm": 0,
            "overdueDays": 0
        },
        {
            "id": 1,
            "loanAmount": 30000.00,
            "status": "正常",
            "startTime": "2025-12-10 16:57:45",
            "term": 24,
            "currentTerm": 0,
            "overdueDays": 0
        }
    ],
    "message": "操作成功"
}
```

**postman测试结果**

![](../LoanOrderImgs/list.png "用户获取订单列表")

## 用户获取订单详情

**网址** /api/orders/{orderId}

**请求方式** GET

**请求示例（网址）** /api/orders/2

**返回数据**

``` json
{
    "code": 200,
    "data": {
        "productName": "优享贷 Pro",
        "order": {
            "id": 2,
            "userId": 1,
            "productId": 1,
            "status": "正常",
            "repaidAmount": 0.00,
            "loanAmount": 30000.00,
            "interestRate": 0.0390,
            "repaidType": "等额本息",
            "loanPeriod": 24,
            "term": 24,
            "currentTerm": 0,
            "contract": null,
            "overdueDays": 0,
            "startTime": "2025-12-10 16:57:50"
        }
    },
    "message": "操作成功"
}
```

**postman测试结果**

![](../LoanOrderImgs/detail.png "用户获取单个订单详情")

## 用户发起还款

**网址** /api/orders/{orderId}/repay

**请求方式** POST

**请求示例（网址）** /api/orders/1/repay

**返回数据**

``` json
{
    "code": 200,
    "data": "已发起还款",
    "message": "操作成功"
}
```

**postman测试结果**

![](../LoanOrderImgs/repay.png "用户发起还款")

## 用户获取还款计划

**网址** /api/orders/{orderId}/repayment-plan

**请求方式** GET

**请求示例（网址）** /api/orders/2/repayment-plan

**返回数据**

``` json
{
    "code": 200,
    "data": [
        {
            "term": 1,
            "principal": 0.00,
            "interest": 54.17,
            "total": 54.17
        },
        {
            "term": 2,
            "principal": 0.00,
            "interest": 54.17,
            "total": 54.17
        },
        {
            "term": 3,
            "principal": 0.00,
            "interest": 54.17,
            "total": 54.17
        },
        {
            "term": 4,
            "principal": 0.00,
            "interest": 54.17,
            "total": 54.17
        },
        {
            "term": 5,
            "principal": 0.00,
            "interest": 54.17,
            "total": 54.17
        },
        {
            "term": 6,
            "principal": 0.00,
            "interest": 54.17,
            "total": 54.17
        },
        {
            "term": 7,
            "principal": 0.00,
            "interest": 54.17,
            "total": 54.17
        },
        {
            "term": 8,
            "principal": 0.00,
            "interest": 54.17,
            "total": 54.17
        },
        {
            "term": 9,
            "principal": 0.00,
            "interest": 54.17,
            "total": 54.17
        },
        {
            "term": 10,
            "principal": 0.00,
            "interest": 54.17,
            "total": 54.17
        },
        {
            "term": 11,
            "principal": 0.00,
            "interest": 54.17,
            "total": 54.17
        },
        {
            "term": 12,
            "principal": 10000.00,
            "interest": 54.17,
            "total": 10054.17
        }
    ],
    "message": "操作成功"
}
```

**postman测试结果**

![](../LoanOrderImgs/repaymentPlan.png "用户获取还款计划")
