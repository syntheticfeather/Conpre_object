# 认证接口说明

## 登录

**网址：**http://localhost:8080/api/auth/login

**请求方式：** POST

**请求参数：**

|字段名|类型|是否必填|说明|示例值|
|---|---|---|---|---|
|phone|string|是|手机号|13545678901|
|password|string|是|密码|Admin01!!|

**请求实例（请求体）：**

``` json
{
    "phone": "13545678901",
    "password": "Admin01!!"
}
```

**返回数据:**

``` json
{
    "token": "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiI4IiwiaWF0IjoxNzYyODUxMjExLCJleHAiOjE3NjI5Mzc2MTF9.fWRVaBcAEujJwfdFxgOaUBnOu9I_tF1g14D8EOAXcVY"
}
```

**postman测试结果：**

![](AuthImgs/login.png "登录成功")
![](AuthImgs/loginFail.png "登录失败，用户名或密码错误")

## 注册

**网址：**/api/auth/register

**请求方式：**POST

**请求参数：**

|字段名|类型|是否必填|说明|示例值|
|---|---|---|---|---|
|name|string|是|姓名|李华|
|phone|string|是|手机号|13912345678|
|password|string|是|密码|lihuaPass123!|

**请求实例（请求体）：**

``` json
{
    "name":"李华",
    "phone":"13912345678",
    "password":"lihuaPass123!"
}
```

**返回数据:**

``` json
{
    "id": 5,
    "name": "李华",
    "createTime": "2025-11-11T16:08:50.1617295"
}
```

**postman测试结果：**

![](AuthImgs/register.png "注册成功")
![](AuthImgs/registerNameFail.png "注册失败，用户名格式错误")
![](AuthImgs/registerPasswordFail.png "注册失败，密码格式错误")
![](AuthImgs/registerPhoneFail.png "注册失败，手机号码格式错误")
![](AuthImgs/registerAllFail.png "多种格式错误")
![](AuthImgs/registerFailPhoneExist.png "注册失败，该手机号已被注册")
