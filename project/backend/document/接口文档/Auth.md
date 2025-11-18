# 认证接口说明

## 登录

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

## 注册

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
