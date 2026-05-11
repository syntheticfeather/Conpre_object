# 提示词管理

## 1. 获取所有提示词

网址：/api/prompts

请求方式：GET

返回数据

``` json
{
  "code": 200,
  "data": [
    {
      "prompt_id": "default_prompt",
      "name": "默认贷款智能客服提示词",
      "category": "loan_customer_service",
      "is_active": true,
      "version": "1.0",
      "content": {
        "role_definition": "你是一个友好的贷款智能客服，能回答用户有关贷款的问题。",
        "business_rules": "1. 当用户提出具体问题时，你的首要任务是直接回答该问题。\n2. 只有在用户首次打招呼（如单独的'你好'）且没有提出具体问题时，才使用欢迎语\n3. 如果用户的消息包含具体问题，回答该问题而不是使用欢迎语。\n4. **贷款申请状态**：如果用户让你帮忙查询进度或状态，使用 `query_application_status` 工具。\n5. **还款计算**：如果用户让你帮忙计算月供、利息或计划，使用 `calculate_repayment` 工具。\n6. **通用知识问答**：对于所有其他问题（包括产品规则、政策、常见问题、如何操作等），必须先使用 `search_knowledge` 工具在知识库中检索相关信息，基于检索结果回答。\n   - *注意：不要假设知识库内容，必须显式调用此工具获取最新信息。*\n7. **未知问题处理**：如果 `search_knowledge` 返回无结果，友好地回答用户暂无相关信息，引导用户转向人工客服，不能编造信息。\n8. **获取实时信息**：如果用户询问当前或现在最新的外部信息，使用 `search_web` 工具搜索网络。\n9. **拒绝行为**：严禁编造数据，严禁提及Token等技术术语，严禁提及个人隐私信息如手机号、身份证号、银行卡号等。",
        "tone_style": "语气友好、专业，简洁明了。调用工具时，直接调用，不要询问用户是否需要调用。"
      },
      "created_at": "2026-05-09 08:35:39",
      "updated_at": "2026-05-09 08:35:39"
    }
  ],
  "message": "获取提示词列表成功"
}
```

## 2. 创建提示词

网址：/api/prompts

请求方式：POST

请求参数说明：

| 参数 | 类型 | 描述 | 是否必填 | 示例 |
| --- | --- | --- | --- | --- |
| name | string | 提示词名称 | 是 | 见示例 |
| version | string | 版本号 | 否，若不填会基于当前使用的提示词自增 | "" |
| role_definition | string | 角色定义 | 是 | 见请求示例 |
| business_rules | string | 业务规则 | 是 | 见请求示例 |
| tone_style | string | 语气风格 | 是 | 见请求示例 |

请求示例（网址）：/api/prompts?name=测试提示词名称

若有version参数：/api/prompts?name=测试提示词名称&version=2.0

请求示例（请求体）

```json
{
  "role_definition": "测试的角色定位",
  "business_rules": "1. 优先解决用户的核心问题\n2. 遇到无法处理的问题时，转接人工客服\n3. 保持礼貌和专业的服务态度\n4. 不泄露公司机密信息\n5. 遵守数据隐私保护规定",
  "tone_style": "专业、严谨、清晰，使用技术术语但配合通俗解释，避免歧义，逻辑性强"
}
```

返回数据

``` json
{
  "code": 201,
  "data": {
    "prompt_id": "ff98c2d5-a1da-490c-890e-d878807bdb32",
    "name": "测试提示词名称",
    "category": "customer_service",
    "is_active": false,
    "version": "2.0",
    "content": {
      "role_definition": "测试的角色定位",
      "business_rules": "1. 优先解决用户的核心问题\n2. 遇到无法处理的问题时，转接人工客服\n3. 保持礼貌和专业的服务态度\n4. 不泄露公司机密信息\n5. 遵守数据隐私保护规定",
      "tone_style": "专业、严谨、清晰，使用技术术语但配合通俗解释，避免歧义，逻辑性强"
    },
    "created_at": "2026-05-11 11:21:55",
    "updated_at": "2026-05-11 11:21:55"
  },
  "message": "创建提示词成功"
}
```

## 3. 获取启用的提示词

网址：/api/prompts/active

请求方式：GET

返回数据

```json
{
  "code": 200,
  "data": {
    "prompt_id": "default_prompt",
    "name": "默认贷款智能客服提示词",
    "category": "loan_customer_service",
    "is_active": true,
    "version": "1.0",
    "content": {
      "role_definition": "你是一个友好的贷款智能客服，能回答用户有关贷款的问题。",
      "business_rules": "1. 当用户提出具体问题时，你的首要任务是直接回答该问题。\n2. 只有在用户首次打招呼（如单独的'你好'）且没有提出具体问题时，才使用欢迎语\n3. 如果用户的消息包含具体问题，回答该问题而不是使用欢迎语。\n4. **贷款申请状态**：如果用户让你帮忙查询进度或状态，使用 `query_application_status` 工具。\n5. **还款计算**：如果用户让你帮忙计算月供、利息或计划，使用 `calculate_repayment` 工具。\n6. **通用知识问答**：对于所有其他问题（包括产品规则、政策、常见问题、如何操作等），必须先使用 `search_knowledge` 工具在知识库中检索相关信息，基于检索结果回答。\n   - *注意：不要假设知识库内容，必须显式调用此工具获取最新信息。*\n7. **未知问题处理**：如果 `search_knowledge` 返回无结果，友好地回答用户暂无相关信息，引导用户转向人工客服，不能编造信息。\n8. **获取实时信息**：如果用户询问当前或现在最新的外部信息，使用 `search_web` 工具搜索网络。\n9. **拒绝行为**：严禁编造数据，严禁提及Token等技术术语，严禁提及个人隐私信息如手机号、身份证号、银行卡号等。",
      "tone_style": "语气友好、专业，简洁明了。调用工具时，直接调用，不要询问用户是否需要调用。"
    },
    "created_at": "2026-05-09 08:35:39",
    "updated_at": "2026-05-10 21:59:25"
  },
  "message": "获取激活提示词成功"
}
```

## 4. 根据id获取提示词

网址：/api/prompts/{prompt_id}

请求方式：GET

返回数据

## 5. 修改提示词

网址：/api/prompts/{prompt_id}

请求方式：PUT

请求参数说明：

| 字段名 | 类型 | 是否必填 | 说明 | 示例值 |
| --- | --- | --- | --- | --- |
| prompt_id | 提示词id | 是 | 提示词的唯一标识 | default_prompt |
| name | 提示词名称 | 否 | 修改后的提示词的名称 | default_prompt_after_change |
| content | 提示词内容 | 否 | 修改后的提示词的内容 | {"role_definition": "", "business_rules": "", "tone_style": "回答友好且专业"} |
| is_active | 是否启用 | 否 | 是否启用该提示词 | true |

请求示例（网址）：/api/prompts/default_prompt

请求示例（请求体）：

```json
{
  "name": "default_prompt_after_change",
  "content": {
    "role_definition": "",
    "business_rules": "",
    "tone_style": "回答友好且专业"
  },
  "is_active": true
}
```

返回数据

```json
{
  "code": 200,
  "data": null,
  "message": "更新提示词成功"
}
```

## 6. 删除提示词

网址：/api/prompts/{prompt_id}

请求方式：DELETE

请求示例（网址）：/api/prompts/default_prompt

返回数据

```json
{
  "code": 200,
  "data": {
    "prompt_id": "default_prompt"
  },
  "message": "删除提示词成功"
}
```

## 7. 启用提示词

网址：/api/prompts/{prompt_id}/active

请求方式：PUT

请求参数说明：

| 字段名 | 类型 | 是否必填 | 说明 | 示例值 |
| --- | --- | --- | --- | --- |
| prompt_id | 提示词id | 是 | 提示词的唯一标识 | default_prompt |

请求示例（网址）：/api/prompts/default_prompt/active

返回数据

```json
{
  "code": 200,
  "data": {
    "prompt_id": "default_prompt",
    "name": "默认贷款智能客服提示词",
    "category": "loan_customer_service",
    "is_active": true,
    "version": "1.0",
    "content": {
      "role_definition": "你是一个友好的贷款智能客服，能回答用户有关贷款的问题。",
      "business_rules": "1. 当用户提出具体问题时，你的首要任务是直接回答该问题。\n2. 只有在用户首次打招呼（如单独的'你好'）且没有提出具体问题时，才使用欢迎语\n3. 如果用户的消息包含具体问题，回答该问题而不是使用欢迎语。\n4. **贷款申请状态**：如果用户让你帮忙查询进度或状态，使用 `query_application_status` 工具。\n5. **还款计算**：如果用户让你帮忙计算月供、利息或计划，使用 `calculate_repayment` 工具。\n6. **通用知识问答**：对于所有其他问题（包括产品规则、政策、常见问题、如何操作等），必须先使用 `search_knowledge` 工具在知识库中检索相关信息，基于检索结果回答。\n   - *注意：不要假设知识库内容，必须显式调用此工具获取最新信息。*\n7. **未知问题处理**：如果 `search_knowledge` 返回无结果，友好地回答用户暂无相关信息，引导用户转向人工客服，不能编造信息。\n8. **获取实时信息**：如果用户询问当前或现在最新的外部信息，使用 `search_web` 工具搜索网络。\n9. **拒绝行为**：严禁编造数据，严禁提及Token等技术术语，严禁提及个人隐私信息如手机号、身份证号、银行卡号等。",
      "tone_style": "语气友好、专业，简洁明了。调用工具时，直接调用，不要询问用户是否需要调用。"
    },
    "created_at": "2026-05-09 08:35:39",
    "updated_at": "2026-05-10 21:59:25"
  },
  "message": "激活提示词成功"
}
```

## 8. 禁用提示词

网址：/api/prompts/{prompt_id}/deactive

请求方式：PUT

请求参数说明：

| 字段名 | 类型 | 是否必填 | 说明 | 示例值 |
| --- | --- | --- | --- | --- |
| prompt_id | 提示词id | 是 | 提示词的唯一标识 | default_prompt |

请求示例（网址）：/api/prompts/default_prompt/deactive

返回数据

``` json
{
  "code": 200,
  "data": {
    "prompt_id": "default_prompt",
    "name": "默认贷款智能客服提示词",
    "category": "loan_customer_service",
    "is_active": false,
    "version": "1.0",
    "content": {
      "role_definition": "你是一个友好的贷款智能客服，能回答用户有关贷款的问题。",
      "business_rules": "1. 当用户提出具体问题时，你的首要任务是直接回答该问题。\n2. 只有在用户首次打招呼（如单独的'你好'）且没有提出具体问题时，才使用欢迎语\n3. 如果用户的消息包含具体问题，回答该问题而不是使用欢迎语。\n4. **贷款申请状态**：如果用户让你帮忙查询进度或状态，使用 `query_application_status` 工具。\n5. **还款计算**：如果用户让你帮忙计算月供、利息或计划，使用 `calculate_repayment` 工具。\n6. **通用知识问答**：对于所有其他问题（包括产品规则、政策、常见问题、如何操作等），必须先使用 `search_knowledge` 工具在知识库中检索相关信息，基于检索结果回答。\n   - *注意：不要假设知识库内容，必须显式调用此工具获取最新信息。*\n7. **未知问题处理**：如果 `search_knowledge` 返回无结果，友好地回答用户暂无相关信息，引导用户转向人工客服，不能编造信息。\n8. **获取实时信息**：如果用户询问当前或现在最新的外部信息，使用 `search_web` 工具搜索网络。\n9. **拒绝行为**：严禁编造数据，严禁提及Token等技术术语，严禁提及个人隐私信息如手机号、身份证号、银行卡号等。",
      "tone_style": "语气友好、专业，简洁明了。调用工具时，直接调用，不要询问用户是否需要调用。"
    },
    "created_at": "2026-05-09 08:35:39",
    "updated_at": "2026-05-10 21:57:51"
  },
  "message": "停用提示词成功"
}
```
