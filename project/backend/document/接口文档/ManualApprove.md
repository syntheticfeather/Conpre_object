# 人工审核接口说明

以下接口请求时都需携带请求头，示例：

| 字段 | 值  | 说明 |
| --   | -- | -- |
| Authorization | Bearer token | 需携带有效token |

## 查看代办审核列表

**网址** /api/approval/pending

**请求方式** GET

**返回数据**

``` json
{
    "code": 200,
    "data": [
        {
            "applicationId": 5,
            "userName": "alice",
            "productName": "优享贷 Pro",
            "loanAmount": 80000,
            "loanPeriod": 36,
            "term": 12,
            "applyTime": "2025-12-09T20:55:36",
            "rejectReason": "AI rejected\n"
        },
        {
            "applicationId": 4,
            "userName": "alice",
            "productName": "优享贷 Pro",
            "loanAmount": 30000,
            "loanPeriod": 24,
            "term": 12,
            "applyTime": "2025-12-09T20:55:25",
            "rejectReason": "AI rejected\n"
        }
    ],
    "message": "操作成功"
}
```

**postman测试结果**

![](../ManualApproveImgs/getList.png)

**日志**

![](../ManualApproveImgs/logGetList.png)

## 查看单个代办审核申请详情

**网址** /api/approval/detail/{loanApplicationId}

**请求方式** GET

**请求参数**

|字段名|类型|是否必填|说明|示例值|
|---|---|---|---|---|
| loanApplicationId | integer | 是 | 申请id | 4 |

**请求示例（网址）** /api/approval/detail/4

**返回数据**

``` json
{
  "code": 200,
  "data": {
    "userName": "王芳",
    "phone": "13100001111",
    "createTime": "2025-11-21T13:22:17",
    "idCard": null,
    "workCertId": null,
    "triCertId": null,
    "immovableCertId": null,
    "creditsScore": null,
    "productName": "优享贷",
    "loanAmount": 150000.00,
    "loanPeriod": 60,
    "term": 6
  },
  "message": "操作成功"
}
```

**postman测试结果**

![](../ManualApproveImgs/getDetail.png)

**日志**

![](../ManualApproveImgs/logGetDetail.png)

## 返回审核结果

**网址** /api/approval/check

**请求方式** POST

**请求参数**

|字段名|类型|是否必填|说明|示例值|
|---|---|---|---|---|
| loanApplicationId | integer | 是 | 申请id | 17 |
| approved | string | 是 | true表示通过，false表示不通过 | true |
| manualRejectReason | String | 否 | 拒绝时需要填写原因 | 材料信息有误 |

**请求示例（请求体）（通过）**

``` json
{
    "loanApplicationId":17,
    "approved":"true"
}
```

**返回数据（通过）**

``` json
{
    "code": 200,
    "data": {
        "loanApplicationId": 17,
        "status": "APPROVED",
        "rejectReason": "AI rejected\nManual approve",
        "reviewTime": "2025-12-09T21:59:05.9672001"
    },
    "message": "操作成功"
}
```

**请求示例（请求体）（拒绝）**

``` json
{
    "loanApplicationId":15,
    "approved":"false",
    "manualRejectReason": "材料信息有误"
}
```

**返回数据（拒绝）**

``` json
{
    "code": 200,
    "data": {
        "loanApplicationId": 15,
        "status": "MANUAL_REJECTED",
        "rejectReason": "AI rejected\n人工拒绝: 材料信息有误",
        "reviewTime": null
    },
    "message": "操作成功"
}
```

**postman测试结果**

![](../ManualApproveImgs/approve.png "通过人工审核")
![](../ManualApproveImgs/reject.png "不通过人工审核")
![](../ManualApproveImgs/rejectWithoutReason.png "拒绝，但未填写人工拒绝的理由")

**日志**

![](../ManualApproveImgs/logCheck.png)