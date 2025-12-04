# 用户接口说明

以下接口请求时都需携带请求头，实例：

| 字段 | 值  | 说明 |
| --   | -- | -- |
| Authorization | Bearer token | 需携带有效token |

## 用户使用

### 用户修改信息

**网址** /api/users/me

**请求方式** PATCH

**请求参数（可选）**:

|字段名|类型|是否必填|说明|示例值|
|---|---|---|---|---|
| userName | integer | 否 | 新的用户名 | 汤姆 |
| avatar | string | 否 | 新的头像 | 无,表示不更改头像 |

**请求示例（请求体）**:

``` json
{
    "userName":"汤姆"
}
```

**返回数据**:

``` json
{
    "code": 200,
    "data": {
        "userId": 1,
        "userName": "汤姆",
        "avatar": null
    },
    "message": "操作成功"
}
```

**postman测试结果**:

![](../UserImgs/userUpdate.png "用户更新信息成功")

**日志**:

![](../UserImgs/logUserUpdate.png)

### 查询信息

**网址** /api/users/me

**请求方式** GET

**返回数据**:

``` json
{
    "code": 200,
    "data": {
        "userId": 1,
        "userName": "Tom",
        "avatar": null
    },
    "message": "操作成功"
}
```

**postman测试结果**:

![](../UserImgs/userSelf.png "用户查询自己信息成功")

**日志**:

![](../UserImgs/logUserSelf.png)

### 上传头像

**网址** /api/users/avatar

**请求方式** POST

**请求示例(请求体)**

格式：form-data（多部分表单）  

| Key  | Type |               Value             |   说明  |
| --   |   -- |                --               | --     |
| file | File | 本地图片文件，如 tomAvatar.jpg） |   必填，支持常见图片格式：JPG、PNG、WEBP 等，大小不超过5MB |

**返回数据**

``` json
{
    "code": 200,
    "data": "/uploads/avatars/avatar_1_20251204_i6d1uv.jpg", // 上传后的路径
    "message": "操作成功"
}
```

**postman测试结果**

![](../UserImgs/uploadAvatar.png "上传图片成功")

**日志**

![](../UserImgs/logAvatar.png)

## 管理员使用

### 添加黑名单

**网址** /api/users/blacklist/add

**请求方式** POST

**请求参数**

|字段名|类型|是否必填|说明|示例值|
|---|---|---|---|---|
| userId | integer | 是 | 用户id | 7 |
| blackLevel | integer | 是 | 黑名单等级 | 1 |

**请求示例（请求体）**

``` json
{
    "userId":7,
    "blackLevel":1
}
```

**返回数据**

``` json
{
    "code": 200,
    "data": null,
    "message": "用户已加入黑名单"
}
```

**postman测试结果**

![](../UserImgs/addBlackList.png "添加用户进黑名单成功")

**日志**

![](../UserImgs/logAddBlackList.png)

### 解除黑名单

**网址** /api/users/blacklist/remove

**请求方式** POST

**请求参数**

|字段名|类型|是否必填|说明|示例值|
|---|---|---|---|---|
| userId | integer | 是 | 用户id | 7 |

**请求示例（请求头）** Content-Type: application/x-www-form-urlencoded

**请求示例（参数）** userId=7

**返回数据**

``` json
{
    "code": 200,
    "data": null,
    "message": "用户已解除黑名单"
}
```

**postman测试结果**

![](../UserImgs/removeBlackList.png "解除黑名单成功")

**日志**

![](../UserImgs/logRemove.png)

### 获取黑名单列表

**网址** /api/users/blacklist/list

**请求方式** GET

**返回数据**

``` json
{
    "code": 200,
    "data": [
        {
            "id": 1,
            "userId": 7,
            "userName": "lihua",
            "phone": "13500135000",
            "blackLevel": 1,
            "createTime": "2025-11-29T12:50:37",
            "updateTime": "2025-11-29T20:52:51",
            "removeTime": "2025-11-29T20:52:51"
        }
    ],
    "message": "操作成功"
}
```

**postman测试结果**

![](../UserImgs/blacklist.png "黑名单列表")

**日志**

![](../UserImgs/logBlacklist.png)

### 查询用户状态列表

**网址** /api/users/admin/stats

**请求方式** GET

**返回数据**:

``` json
{
    "code": 200,
    "data": [
        {
            "userId": 1,
            "userName": "Tom",
            "loanStatus": "无借贷",
            "totalTransactionCount": 0,
            "totalLoanAmount": 0,
            "totalRepaidAmount": 0
        },
        {
            "userId": 2,
            "userName": "王芳",
            "loanStatus": "无借贷",
            "totalTransactionCount": 0,
            "totalLoanAmount": 0,
            "totalRepaidAmount": 0
        },
        {
            "userId": 3,
            "userName": "张伟",
            "loanStatus": "无借贷",
            "totalTransactionCount": 0,
            "totalLoanAmount": 0,
            "totalRepaidAmount": 0
        },
        {
            "userId": 4,
            "userName": "李明",
            "loanStatus": "无借贷",
            "totalTransactionCount": 0,
            "totalLoanAmount": 0,
            "totalRepaidAmount": 0
        }
    ],
    "message": "操作成功"
}
```

**postman测试结果**:

![](../UserImgs/userStats.png )

**日志**:

![](../UserImgs/logUserStats.png)

### 查看单个用户详细信息

**网址** /api/users/admin/{userId}

**请求方式** GET

**请求参数**:

|字段名|类型|是否必填|说明|示例值|
|---|---|---|---|---|
| userId | integer | 是 | 用户id | 1 |

**请求示例(网址)** /api/users/admin/1

**返回数据**:

``` json
{
    "code": 200,
    "data": {
        "userId": 1,
        "userName": "汤姆",
        "avatar": null,
        "phone": "13712345678",
        "idCard": null,
        "role": 0,
        "creditScore": null,
        "blackLevel": 0,
        "createTime": "2025-11-21T13:21:32",
        "updateTime": "2025-11-22T10:50:53"
    },
    "message": "操作成功"
}
```

**postman测试结果**:

![](../UserImgs/adminGetUser.png "管理员获取单个用户详情成功")

**日志**:

![](../UserImgs/logAdminGetUser.png)

### 根据信誉分从高到低查询用户

**网址** /api/users/search-by-credit

**请求方式** GET

**请求参数**:

|字段名|类型|是否必填|说明|示例值|
|---|---|---|---|---|
| expr |  String  |  是  | 信仰分筛选表达式，支持>,<,>=,<=,=  |  <100  |

**请求实例（网址）** /api/users/search-by-credit?expr=<100

**返回数据**:

``` json
[
  {
    "id": 5,
    "name": "李华",
    "phone": "13912345678",
    "creditScore": 70,
    "createTime": "2025-11-11T16:08:50"
  },
  {
    "id": 4,
    "name": "Alice_Wang",
    "phone": "15912345678",
    "creditScore": 65,
    "createTime": "2025-11-11T16:02:20"
  },
  {
    "id": 3,
    "name": "张三",
    "phone": "13800138000",
    "creditScore": 60,
    "createTime": "2025-11-11T15:59:10"
  }
]
```

**postman测试**:

![](../UserImgs/search.png "搜索成功")
![](../UserImgs/searchFail.png "搜索失败")