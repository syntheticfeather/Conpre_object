# 用户接口文档

可以通过 http://localhost:8000/docs 查看接口文档。

## 用户接口

**网址** http://127.0.0.1:8000/api/chat/stream

**请求方式** POST

**对话示例**  

### 1. 初始化会话

    ``` text
    event: session_init
    data: {"session_id": "24c2f2d5-6499-4f1c-8e66-df46e986abf7"}

    event: message
    data: 您好

    event: message
    data: ！

    event: message
    data: 我是

    event: message
    data: 贷款

    event: message
    data: 智能

    event: message
    data: 客服

    event: message
    data: ，

    event: message
    data: 很高兴

    event: message
    data: 为您

    event: message
    data: 服务

    event: message
    data: ！

    event: message
    data: 请问

    event: message
    data: 有什么

    event: message
    data: 可以

    event: message
    data: 帮

    event: message
    data: 您的

    event: message
    data: 吗

    event: message
    data: ？
    ```

### 2. 用户询问还款计划

**请求示例（请求体）**  

    ```json
    {
      "message": "我有一笔贷款，10000元，贷12个月，利率0.065，还款方式是先息后本，帮我计算一下还款计划",
      "session_id": "24c2f2d5-6499-4f1c-8e66-df46e986abf7"
    }
    ```

**返回数据示例（响应体）**  

[calculate_repayment_response.txt](../calculate_repayment_response.txt)

### 3. 用户询问贷款状态

**返回数据示例（响应体）**  

[query_app_status_response.txt](../query_app_status_response.txt)

### 4. 用户询问最新贷款政策

**请求示例（请求体）**  

    ```json
    {
      "message": "当前最新的贷款政策有哪些",
      "session_id": "24c2f2d5-6499-4f1c-8e66-df46e986abf7"
    }
    ```

### 5. 用户询问贷款产品

**请求示例（请求体）**  

    ```json
    {
      "message": "帮我查询有哪些贷款产品可以使用",
      "session_id": "41948804-c3be-4719-91ae-9711e53b1195"
    }
    ```

**返回数据示例（响应体）**  

[query_products_response.txt](../query_products_response.txt)  
![日志](../ChatImgs/queryProducts.png)
