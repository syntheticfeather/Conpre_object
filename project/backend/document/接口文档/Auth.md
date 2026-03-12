# 认证接口说明

## 用户使用

### 登录

**网址**：http://localhost:8080/api/auth/login

**请求方式**：POST

**请求参数**：

|字段名|类型|是否必填|说明|示例值|
|---|---|---|---|---|
|phone|string|是|手机号|13545678901|
|password|string|是|密码|Admin01!!|

**请求实例（请求体）**：

``` json
{
    "phone": "13545678901",
    "password": "Admin01!!"
}
```

**返回数据**：

``` json
{
    "code": 200,
    "data": {
        "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMzU0NTY3ODkwMSIsInVzZXJJZCI6IjgiLCJpYXQiOjE3NjM0NDg1MjYsImV4cCI6MTc2MzUzNDkyNn0.FjIp4FZTe_Wur65rKrMV5KPN5t-HAJQaYweBLiVeKrg"
    },
    "message": "登录成功"
}
```

**postman测试结果**：

**成功**
![](../AuthImgs/loginSuccess.png "登陆成功")

**失败**
![](../AuthImgs/loginFail.png "登录失败，用户名或密码错误")

### 注册

**网址**：/api/auth/register

**请求方式**：POST

**请求参数**：

|字段名|类型|是否必填|说明|示例值|
|---|---|---|---|---|
|name|string|是|姓名|Tom|
|phone|string|是|手机号|13712345678|
|password|string|是|密码|Tom12345!|

**请求实例（请求体）**：

``` json
{
    "name":"Tom",
    "phone":"13712345678",
    "password":"Tom12345!"
}
```

**返回数据**：

``` json
{
    "code": 200,
    "data": {
        "id": 11,
        "name": "Tom",
        "createTime": "2025-11-18T14:47:41.9154566"
    },
    "message": "注册成功"
}
```

**postman测试结果**：

**成功**
![](../AuthImgs/registerSuccess.png "注册成功")

**失败**
![](../AuthImgs/registerNameFail.png "注册失败，用户名格式错误")

![](../AuthImgs/registerPasswordFail.png "注册失败，密码格式错误")

![](../AuthImgs/registerPhoneFail.png "注册失败，手机号码格式错误")

![](../AuthImgs/registerAllFail.png "多种格式错误")

![](../AuthImgs/registerPhoneExist.png "注册失败，该手机号已被注册")

### 上传认证材料

**网址** /api/auth/submit-all

**请求方式** POST

**请求头**

|字段名|值|说明|
| --- | --- | --- |
| Authorization | Bearer token | 需携带有效token |

**请求参数**

格式 form-data(多部分表单)

| Key                |  Type   |    Value  |  是否必填  |   说明    |  示例值    |
|---|---|---|---|---|---|
| propertyFile       |   File  |   房产证明图片           |    否   |  可选，上传图片         |           |
| carFile            |   File  |   车产证明图片           |    否   |  可选，上传图片         |           |
| employmentFile     |   File  |   工作证明图片           |    否   |  可选，上传图片         |           |
| salaryFile         |   File  |   工资流水截图或银行流水  |    否   |  可选，上传图片         |           |
| socialSecurityFile |   File  |   社保缴纳记录图片       |    否    |  可选，上传图片         |           |
| creditReportFile   |   File  |   征信报告图片           |    否    |  可选，上传图片         |           |
| idCard             |   Text  |   身份证号码             |    是    | 必须填写，18位数字    | 110101199003072316 |
| bankCardId         |   Text  |   银行卡号               |    是    | 必须填写，长度16位    | 6214850200001234 |

**返回数据**

``` json
{
    "code": 200,
    "data": null,
    "message": "全部认证材料提交成功"
}
```

**postman测试结果**

![](../AuthImgs/submitAll.png "用户上传认证材料成功")

### 获取已经上传的认证信息

**网址**：/api/auth/cert-info

**请求方式** GET

**返回数据**：

``` json
{
    "code": 200,
    "data": {
        "userCert": {
            "userId": 10,
            "idCard": "110101199003072316",
            "creditScore": 625,
            "bankCardId": "6214850200001234",
            "workCertId": 2,
            "triCertId": 2,
            "immovableCertId": 2
        },
        "workCert": {
            "workCertId": 2,
            "employmentCertPath": "/uploads/work/employment/employment_10_20260311_cz77r0.png",
            "salaryCertPath": "/uploads/work/salary/salary_10_20260310_gkkvl6.png"
        },
        "triCert": {
            "triCertId": 2,
            "socialSecurityPath": "/uploads/tri/social-security/social_10_20260311_yne0co.png",
            "creditReportPath": null
        },
        "immovablesCert": {
            "immovableCertId": 2,
            "propertyCertPath": "/uploads/immovables/property/property_10_20260311_jtsfab.png",
            "carCertPath": "/uploads/immovables/car/car_10_20260311_gwgs90.png",
            "totalValue": null
        }
    },
    "message": "认证信息获取成功"
}
```

**postman测试结果**：

![](../AuthImgs/getCert.png "获取认证信息成功")

## 管理员使用

### 根据 workCertId 查询工作认证信息

**网址**：/api/auth/work-cert

**请求方式**：GET

**请求参数**：

|字段名|类型|是否必填|说明|示例值|
|---|---|---|---|---|
|workCertId|integer|是|工作认证ID|2|

**返回数据**：

``` json
{
    "code": 200,
    "data": {
        "workCertId": 2,
        "employmentCertPath": "/uploads/work/employment/employment_10_20260311_cz77r0.png",
        "salaryCertPath": "/uploads/work/salary/salary_10_20260310_gkkvl6.png"
    },
    "message": "工作认证信息获取成功"
}
```

**postman测试结果**：

![](../AuthImgs/getWorkCert.png "获取工作认证信息成功")

### 根据 triCertId 查询第三方认证信息

**网址**：/api/auth/tri-cert

**请求方式**：GET

**请求参数**：

|字段名|类型|是否必填|说明|示例值|
|---|---|---|---|---|
triCertId|integer|是|第三方认证ID|2|

**返回数据**：

``` json
{
    "code": 200,
    "data": {
        "triCertId": 2,
        "socialSecurityPath": "/uploads/tri/social-security/social_10_20260311_yne0co.png",
        "creditReportPath": null
    },
    "message": "第三方认证信息获取成功"
}
```

**postman测试结果**：

![](../AuthImgs/getTriCert.png "获取第三方认证信息成功")

### 根据 immovableCertId 查询不动产认证信息

**网址**：/api/auth/immovables-cert

**请求方式**：GET

**请求参数**：

|字段名|类型|是否必填|说明|示例值|
|---|---|---|---|---|
|immovableCertId|integer|是|不动产认证ID|2|

**返回数据**：

``` json
{
    "code": 200,
    "data": {
        "immovableCertId": 2,
        "propertyCertPath": "/uploads/immovables/property/property_10_20260311_jtsfab.png",
        "carCertPath": "/uploads/immovables/car/car_10_20260311_gwgs90.png",
        "totalValue": null
    },
    "message": "不动产认证信息获取成功"
}
```

**postman测试结果**：

![](../AuthImgs/getImmovableCert.png "获取不动产认证信息成功")