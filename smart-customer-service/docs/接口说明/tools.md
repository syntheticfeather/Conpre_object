# 工具接口

## 1. 获取所有工具

网址：/api/tools

请求方式：GET

返回数据：

``` json
{
  "code": 200,
  "data": [
    {
      "name": "query_application_status",
      "description": "帮助用户查询贷款申请状态。当用户主动要求帮忙查询申请进度、申请状态时使用。",
      "enabled": true,
      "source": "static"
    },
    {
      "name": "calculate_repayment",
      "description": "计算还款计划。当用户询问月还款额、还款金额、还款计划时使用。\n\n    参数说明：\n    - repaid_type: 还款方式，支持\"等额本息\"、\"等额本金\"、\"先息后本\"、\"一次性还本付息\"\n    - loan_amount: 贷款金额，单位元\n    - loan_period: 贷款期限，单位月\n    - interest_rate: 年利率，如0.12表示12%",
      "enabled": true,
      "source": "static"
    },
    {
      "name": "query_loan_products",
      "description": "获取所有贷款产品列表。当用户询问有哪些贷款产品、贷款产品详情、可申请的贷款产品时使用。",
      "enabled": true,
      "source": "static"
    },
    {
      "name": "search_web",
      "description": "使用 Tavily 搜索引擎搜索网络信息。当需要获取最新的外部信息时使用。",
      "enabled": true,
      "source": "static"
    },
    {
      "name": "search_knowledge",
      "description": "在知识库中搜索相关信息。当用户询问常见问题、政策规则、产品信息时使用。",
      "enabled": true,
      "source": "static"
    }
  ],
  "message": "获取工具列表成功"
}
```

## 2. 按名称搜索工具

网址：/api/tools/search

请求方式：GET

请求参数：

|字段名|类型|是否必填|说明|示例值|
|---|---|---|---|---|
|name|string|是|工具名称|query_loan_products|

请求示例（网址）：/api/tools/search?name=query_loan_products

返回数据：

``` json
{
  "code": 200,
  "data": {
    "name": "query_loan_products",
    "description": "获取所有贷款产品列表。当用户询问有哪些贷款产品、贷款产品详情、可申请的贷款产品时使用。",
    "enabled": true
  },
  "message": "获取工具成功"
}
```

## 3. 设置工具状态（启用/禁用）

网址：/api/tools/{tool_name}

请求方式：PUT

请求参数：

|字段名|类型|是否必填|说明|示例值|
|---|---|---|---|---|
|tool_name|string|是|工具名称|search_web|
|enabled|boolean|是|启用、禁用，true为启用，false为禁用|false|

请求示例（网址）：/api/tools/search_web?enabled=false

返回数据：

``` json
{
  "code": 200,
  "data": null,
  "message": "工具 search_web 禁用 成功"
}
```

## 4. 上传mcp服务器配置

网址：/api/tools/mcp/server

请求方式：POST

请求参数说明：

|字段名|类型|是否必填|说明|示例值|
|---|---|---|---|---|
|server_id|string|是|服务器名称|tavily-mcp|
|config|object|是|服务器配置|{"command": "npx", "args": ["-y", "tavily-mcp@latest"], "env": {"TAVILY_API_KEY": "your-api-key"}}|

config配置中参数说明：

|字段名|类型|是否必填|说明|示例值|
|---|---|---|---|---|
|command|string|是|用于启动MCP服务器的命令|npx|
|args|array|是|服务器命令参数|["-y", "tavily-mcp@latest"]|
|env|object|是|服务器环境变量，需要填写自己的 api key|{"TAVILY_API_KEY": "your-api-key"}|

请求示例（请求体）:

``` json
{
  "server_id": "tavily-mcp",
  "config": {
    "command": "npx",
    "args": [
      "-y",
      "tavily-mcp@latest"
    ],
    "env": {
      "TAVILY_API_KEY": "your-api-key"
    }
  }
}
```

返回数据：

``` json
{
  "code": 200,
  "data": {
    "server_id": "tavily-mcp"
  },
  "message": "MCP服务器添加成功"
}
```

## 5. 获取mcp服务器配置

网址：/api/tools/mcp/servers

请求方式：GET

返回数据

``` json
{
  "code": 200,
  "data": [
    {
      "server_id": "tavily-mcp",
      "config": {
        "command": "npx",
        "args": [
          "-y",
          "tavily-mcp@latest"
        ],
        "env": {
          "TAVILY_API_KEY": "your-api-key(实际会返回你填写的api key)"
        }
      },
      "created_at": "2026-05-17 21:23:56",
      "updated_at": "2026-05-17 21:23:56"
    }
  ],
  "message": "获取MCP服务器列表成功"
}
```

## 6. 根据server_id获取配置

网址：/api/tools/mcp/server/{server_id}

请求方式：GET

请求参数：

|字段名|类型|是否必填|说明|示例值|
|---|---|---|---|---|
|server_id|string|是|服务器名称|tavily_mcp|

返回数据：

``` json
{
  "code": 200,
  "data": {
    "server_id": "tavily-mcp",
    "config": {
      "command": "npx",
      "args": [
        "-y",
        "tavily-mcp@latest"
      ],
      "env": {
        "TAVILY_API_KEY": "your-api-key(实际会返回你填写的api key)"
      }
    },
    "created_at": "2026-05-17 21:23:56",
    "updated_at": "2026-05-17 21:23:56"
  },
  "message": "获取MCP服务器配置成功"
}
```

## 7. 删除配置

网址：/api/tools/mcp/server/{server_id}

请求方式：DELETE

请求参数：

|字段名|类型|是否必填|说明|示例值|
|---|---|---|---|---|
|server_id|string|是|服务器名称|tavily_search|

返回数据

``` json
{
  "code": 200,
  "data": null,
  "message": "MCP服务器 tavily_search 删除成功"
}
```

## 8. 刷新mcp工具

网址：/api/tools/mcp/refresh

请求方式：POST

返回数据

``` json
{
  "code": 200,
  "data": {
    "count": 5,
    "tools": [
      {
        "name": "tavily_search",
        "description": "Search the web for current information on any topic. Use for news, facts, or data beyond your knowledge cutoff. Returns snippets and source URLs.",
        "enabled": true,
        "source": "mcp"
      },
      {
        "name": "tavily_extract",
        "description": "Extract content from URLs. Returns raw page content in markdown or text format.",
        "enabled": true,
        "source": "mcp"
      },
      {
        "name": "tavily_crawl",
        "description": "Crawl a website starting from a URL. Extracts content from pages with configurable depth and breadth.",
        "enabled": true,
        "source": "mcp"
      },
      {
        "name": "tavily_map",
        "description": "Map a website's structure. Returns a list of URLs found starting from the base URL.",
        "enabled": true,
        "source": "mcp"
      },
      {
        "name": "tavily_research",
        "description": "Perform comprehensive research on a given topic or question. Use this tool when you need to gather information from multiple sources to answer a question or complete a task. Returns a detailed response based on the research findings. Rate limit: 20 requests per minute.",
        "enabled": true,
        "source": "mcp"
      }
    ]
  },
  "message": "刷新成功，当前有 5 个动态工具"
}
```
