# 提示词管理

Phase 1 起提示词按 **category 分档管理**，同 category 多版本互斥激活（仅一条 `is_active=true`）。系统提示词每次对话实时组装，修改/激活/停用/删除激活文档后**立即生效**（无需重启）。

## category 一览

| category | 用途 | content 主要字段 |
| --- | --- | --- |
| base | 全局角色定义 + 语气 + 兜底业务规则 | role_definition、business_rules、tone_style |
| general | 低置信/未知意图时的通用业务规则 | business_rules |
| query_status / calculate / list_products / apply_loan / complaint / consult | 6 个意图专属片段（意图置信度 ≥0.6 时注入） | business_rules（本意图特有规则）、few_shots（少样本）、output_extra |

content 各字段均为可选，按文档类型选用；意图片段字段的详细写法参考 [提示词精细化.md](../提示词精细化.md) 3.1 节。

## 字段说明

### 提示词文档顶层字段

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| prompt_id | string | 系统生成 | 提示词唯一标识（UUID；种子文档使用固定 id 如 `prompt_calculate_default`），创建后不可修改 |
| name | string | 是 | 展示名称 |
| category | string | 否 | 分档标识，创建时缺省为 `base`；仅允许上表中的 8 个值，非法值返回 400 |
| is_active | boolean | 系统默认 | 是否为该 category 当前激活版本；新建默认 false；同一 category 至多一条 true，激活时其余自动停用 |
| version | string | 系统生成 | 版本号，格式 `主版本.次版本`（如 1.0）；创建时缺省按同 category 激活版本自增，content 实际变更时自动 +0.1 |
| content | object | 是 | 提示词内容，子字段见下表；创建时可传空对象 `{}` |
| created_at | datetime | 系统生成 | 创建时间，格式 `YYYY-MM-DD HH:mm:ss` |
| updated_at | datetime | 系统生成 | 最近更新时间，格式 `YYYY-MM-DD HH:mm:ss` |

### content 子字段（均为可选）

| 字段 | 类型 | 适用文档 | 说明 |
| --- | --- | --- | --- |
| role_definition | string | base | L1 角色层：AI 身份定义 |
| business_rules | string | base / general / 意图片段 | 业务规则：base 为兜底通用规则，general 为通用规则，意图片段只写**本意图特有规则**（建议 ≤300 字，措辞用"可能"） |
| tone_style | string | base | L4 输出层：回复语气与风格 |
| few_shots | array&lt;object&gt; | 意图片段 | 本场景少样本示例，元素结构见下表 |
| output_extra | string | 意图片段 | L4 输出层：该意图的附加输出要求（如利率口径、免责提示） |

### few_shots 元素字段

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| input | string | 是 | 示例用户消息 |
| expect | string | 是 | 期望的处理做法（如应调用的工具、应答边界） |

---

## 1. 获取所有提示词

网址：/api/prompts

请求方式：GET

返回数据

```json
{
  "code": 200,
  "data": [
    {
      "prompt_id": "prompt_base_default",
      "name": "基础全局提示词（L1角色/L4语气/兜底规则）",
      "category": "base",
      "is_active": true,
      "version": "1.0",
      "content": {
        "role_definition": "你是一个友好、专业的贷款智能客服，负责回答用户关于贷款产品、申请、还款等方面的问题。",
        "business_rules": "1. 当用户提出具体问题时，首要任务是直接回答该问题……",
        "tone_style": "语气友好、专业，简洁明了；分点陈述复杂内容；不使用绝对化表述。"
      },
      "created_at": "2026-09-22 20:58:12",
      "updated_at": "2026-09-22 20:58:12"
    },
    {
      "prompt_id": "prompt_calculate_default",
      "name": "意图片段-还款计算",
      "category": "calculate",
      "is_active": true,
      "version": "1.0",
      "content": {
        "business_rules": "- 用户可能需要月供、利息、总还款额、还款计划等具体数字，必须调用 `calculate_repayment` 工具，严禁心算或估算……",
        "few_shots": [
          { "input": "帮我算一下20万36期4.5%等额本息总利息", "expect": "参数齐全，直接调用 calculate_repayment，不从知识库找答案" }
        ],
        "output_extra": "展示利率统一使用年化利率口径；数字结果后附“测算结果仅供参考，实际以合同约定为准”。"
      },
      "created_at": "2026-09-22 20:58:12",
      "updated_at": "2026-09-22 20:58:12"
    }
  ],
  "message": "获取提示词列表成功"
}
```

## 2. 创建提示词

网址：/api/prompts

请求方式：POST

请求参数说明（JSON body）：

| 参数 | 类型 | 描述 | 是否必填 |
| --- | --- | --- | --- |
| name | string | 提示词名称 | 是 |
| category | string | 分类，默认 `base`；允许值：base / general / query_status / calculate / list_products / apply_loan / complaint / consult，非法值返回 400 | 否 |
| version | string | 版本号；不填则基于同 category 当前激活版本自增（如 1.0 → 1.1），该 category 无激活文档时为 1.0 | 否 |
| content | object | 提示词内容，字段见文首 category 一览表；均为可选 | 是（可为空对象） |

请求示例（新建意图片段）

```json
{
  "name": "意图片段-投诉（测试版）",
  "category": "complaint",
  "content": {
    "business_rules": "- 先共情安抚，再处理问题\n- 不承诺赔偿，争议规则先检索知识库",
    "few_shots": [
      { "input": "提前还款扣了我违约金，这不合理吧", "expect": "先安抚，再核对违约金规则，不承诺退款" }
    ],
    "output_extra": "全程使用安抚性措辞，避免争辩。"
  }
}
```

返回数据

```json
{
  "code": 201,
  "data": {
    "prompt_id": "9c6d193d-dc2a-4824-8692-10192976794f",
    "name": "意图片段-投诉（测试版）",
    "category": "complaint",
    "is_active": false,
    "version": "1.1",
    "content": {
      "role_definition": "",
      "business_rules": "- 先共情安抚，再处理问题\n- 不承诺赔偿，争议规则先检索知识库",
      "tone_style": "",
      "few_shots": [
        {
          "input": "提前还款扣了我违约金，这不合理吧",
          "expect": "先安抚，再核对违约金规则，不承诺退款"
        }
      ],
      "output_extra": "全程使用安抚性措辞，避免争辩。"
    },
    "created_at": "2026-09-23 20:12:21",
    "updated_at": "2026-09-23 20:12:21"
  },
  "message": "创建提示词成功"
}
```

说明：新建文档默认 `is_active=false`，需调用启用接口（见第 7 节）激活；激活后同 category 旧文档自动停用。

## 3. 获取启用的提示词

网址：/api/prompts/active?category={category}

请求方式：GET

请求参数说明：

| 参数 | 类型 | 描述 | 是否必填 |
| --- | --- | --- | --- |
| category | string | 提示词分类，默认 `base` | 否 |

请求示例（网址）：/api/prompts/active?category=complaint

返回数据（该 category 无激活文档时返回 404）：

```json
{
  "code": 200,
  "data": {
    "prompt_id": "prompt_complaint_default",
    "name": "意图片段-投诉",
    "category": "complaint",
    "is_active": true,
    "version": "1.0",
    "content": {
      "role_definition": "",
      "business_rules": "- 用户可能带有不满情绪，先共情安抚（如“非常理解您的心情”），再处理问题；不与用户争辩、不推卸责任。\n- 不承诺赔偿、不代表公司认定责任；费用/违约金争议先用 `search_knowledge` 核对规则再回应。\n- 需核实身份或订单时，引导用户通过安全渠道提供，不在对话中索取敏感信息。\n- 达到升级条件（强烈不满、重复投诉、涉及资金损失）时，明确引导人工客服或投诉热线。",
      "tone_style": "",
      "few_shots": [
        {
          "input": "提前还款扣了我违约金，这不合理吧",
          "expect": "先安抚，再核对违约金规则说明，不承诺退款，必要时引导人工"
        }
      ],
      "output_extra": ""
    },
    "created_at": "2026-09-23 12:00:35",
    "updated_at": "2026-09-23 12:00:35"
  },
  "message": "获取激活提示词成功"
}
```

## 4. 根据 id 获取提示词

网址：/api/prompts/{prompt_id}

请求方式：GET

返回数据：结构同第 1 节单个元素；不存在时返回 404。

## 5. 修改提示词

网址：/api/prompts/{prompt_id}

请求方式：PUT

请求参数说明（JSON body，均可选，但至少提供一项）：

| 字段名 | 类型 | 是否必填 | 说明 |
| --- | --- | --- | --- |
| name | string | 否 | 新名称；空字符串视为不修改 |
| content | object | 否 | 部分更新：仅传需要修改的字段（role_definition / business_rules / tone_style / few_shots / output_extra），**空字符串字段视为不修改**；内容有实际变更时版本号自动自增 |
| is_active | boolean | 否 | 改为 true 时激活，同 category 其他文档自动停用 |

请求示例（网址）：/api/prompts/prompt_calculate_default

请求示例（请求体）：

```json
{
  "content": {
    "output_extra": "展示利率统一使用年化利率口径。"
  }
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

说明：若修改的是激活中的文档（或本次将其激活），组装缓存立即失效，改动即时生效，无需重启。

## 6. 删除提示词

网址：/api/prompts/{prompt_id}

请求方式：DELETE

请求示例（网址）：/api/prompts/prompt_calculate_default

返回数据

```json
{
  "code": 200,
  "data": {
    "prompt_id": "prompt_calculate_default"
  },
  "message": "删除提示词成功"
}
```

说明：删除激活中的文档同样会立即清空组装缓存。

## 7. 启用提示词

网址：/api/prompts/{prompt_id}/activate

请求方式：PUT

请求示例（网址）：/api/prompts/prompt_calculate_default/activate

返回数据：结构同第 1 节单个元素（返回激活后的文档，`is_active=true`）。

说明：激活后同 category 其他文档自动停用（互斥）；组装缓存立即失效。

## 8. 禁用提示词

网址：/api/prompts/{prompt_id}/deactivate

请求方式：PUT

请求示例（网址）：/api/prompts/prompt_calculate_default/deactivate

返回数据：结构同第 1 节单个元素（返回停用后的文档，`is_active=false`）。

## 错误码

| HTTP | 场景 |
| --- | --- |
| 400 | category 不在白名单；未提供任何更新数据 |
| 404 | 提示词不存在；该 category 无激活提示词 |
