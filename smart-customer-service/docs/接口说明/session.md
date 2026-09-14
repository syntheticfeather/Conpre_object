# 会话/消息接口（用户端）

> - 左侧会话列表（按最近更新倒序，支持置顶/归档）
> - 同会话向上滑动，分页加载更早历史
> - 新建/重命名/删除会话

所有接口均需在请求头携带 `Authorization: Bearer <token>`。

> **归属校验**：所有按 session_id 操作的接口（查详情/改/删/消息列表）都会校验会话归属，
> 会话不存在或不属于当前用户一律返回 **404**（不暴露会话是否存在）。

---

## 通用响应结构与数据模型

所有接口的响应均为统一外层包装：

| 字段 | 类型 | 说明 |
|---|---|---|
| code | int | 状态码，200 表示成功；401 未鉴权；404 会话不存在/不属于当前用户；400 参数错误；500 服务端错误 |
| message | string | 结果提示文本 |
| data | object/array/null | 业务数据，结构见各接口说明 |

**SessionItem（会话对象）** —— 第 1/2/3/4/5 节接口的 data 或 data.items 元素均为此模型：

| 字段 | 类型 | 说明 |
|---|---|---|
| session_id | string | 会话唯一 ID（UUID），首次对话/+新对话时会返回 |
| user_id | string | 会话所属用户 ID，token 解析而来 |
| title | string | 会话标题；新建时为占位标题"新对话 yyyy-MM-dd HH:mm"，首条消息后由 LLM 自动回填，用户可通过 PATCH 修改 |
| last_message_preview | string | 最近一条消息内容预览（截断前 50 字）；空会话为空串 |
| last_message_at | string | 最近一条消息时间，格式 `yyyy-MM-dd HH:mm:ss`；无消息时等于创建时间 |
| message_count | int | 会话消息总数（user 与 assistant 消息各计 1 条） |
| created_at | string | 会话创建时间，格式 `yyyy-MM-dd HH:mm:ss` |
| updated_at | string | 会话最近更新时间，格式 `yyyy-MM-dd HH:mm:ss`；每次写入消息/修改元信息都会刷新 |
| pinned | bool | 是否置顶 |
| archived | bool | 是否归档 |

**MessageItem（消息对象）** —— 第 7 节接口的 data.items 元素：

| 字段 | 类型 | 说明 |
|---|---|---|
| message_id | string | 消息唯一 ID（UUID） |
| session_id | string | 所属会话 ID |
| user_id | string | 消息所属用户 ID |
| role | string | 消息角色：`user`（用户）/ `assistant`（客服/AI 回复） |
| content | string | 消息文本内容 |
| created_at | string | 消息创建时间，格式 `yyyy-MM-dd HH:mm:ss` |

**分页响应（SessionListResponse / MessageListResponse 通用）**：

| 字段 | 类型 | 说明 |
|---|---|---|
| items | array | 当前页数据列表；会话列表元素为 SessionItem，消息列表元素为 MessageItem |
| next_cursor | string | 下一页游标（base64）；传给下一页请求的 cursor 参数，为 null 表示没有更多数据 |
| has_more | bool | 是否还有更早的数据可加载 |

---

## 1. 新建会话

网址：/api/sessions

请求方式：POST

**无请求体、无 Query 参数**（标题不由前端传入：新建时后端生成占位标题"新对话 yyyy-MM-dd HH:mm"，首条消息后由 LLM 异步摘要回填；用户手动改名走第 4 节 PATCH 接口）。

**空会话复用（服务端固定策略）**：该用户已有空会话（message_count=0）则直接复用并返回原 session_id（无时间限制），避免重复点"+"堆叠空会话；用户发首条消息后 message_count>0，下次点"+"正常新建。

返回结果（data 为 SessionItem 会话对象，字段说明见文首「通用响应结构与数据模型」）：

```json
{
  "code": 200,
  "data": {
    "session_id": "fc6a8107-029e-41b0-abb0-754fe368c93e",
    "user_id": "7",
    "title": "新对话 2026-09-10 10:30",
    "last_message_preview": "",
    "last_message_at": "2026-09-10 10:30:00",
    "message_count": 0,
    "created_at": "2026-09-10 10:30:00",
    "updated_at": "2026-09-10 10:30:00",
    "pinned": false,
    "archived": false
  },
  "message": "创建会话成功"
}
```

---

## 2. 查询会话列表

网址：/api/sessions

请求方式：GET

请求参数说明（Query）：

| 参数 | 描述 | 类型 | 示例 |
|---|---|---|---|
| cursor | 上一页返回的 next_cursor，首次不传 | string | "" |
| limit | 每页数量，1-100，默认 20，可不填 | int | 20 |
| include_archived | 是否包含已归档会话，默认 false，可不填 | bool | false |
| has_messages | 仅返回 message_count > 0 的会话，用于进入上次对话场景，客户端进入聊天页时填true | bool | false |

返回结果（SessionItem 模型，与详情/新建通用，前端选择性展示）：

```json
{
  "code": 200,
  "data": {
    "items": [
      {
        "session_id": "80f37faa-b8bb-4728-add9-a50cbbdcb382",
        "user_id": "7",
        "title": "新对话 2026-09-09 18:37",
        "last_message_preview": "",
        "last_message_at": "2026-09-09 18:37:47",
        "message_count": 0,
        "created_at": "2026-09-09 18:37:47",
        "updated_at": "2026-09-09 18:37:47",
        "pinned": false,
        "archived": false
      },
      {
        "session_id": "85cb1e75-80dc-48a5-bf67-d680f275085a",
        "user_id": "7",
        "title": "贷款月还款计算咨询",
        "last_message_preview": "计算结果如下：\n\n贷款金额：10000元\n贷款期限：12期（1年）\n年利率：0.000650%\n还款",
        "last_message_at": "2026-09-09 18:36:36",
        "message_count": 2,
        "created_at": "2026-09-09 18:36:36",
        "updated_at": "2026-09-09 18:36:39",
        "pinned": false,
        "archived": false
      },
      {
        "session_id": "c2f5be07-513e-4143-8a73-330375622b95",
        "user_id": "7",
        "title": "贷款材料是否必须交",
        "last_message_preview": "**是的，申请贷款必须提交认证材料。**\n\n根据知识库中的官方信息，\"提供有效身份证明\"和\"完成实名",
        "last_message_at": "2026-09-09 18:18:59",
        "message_count": 2,
        "created_at": "2026-09-09 18:18:59",
        "updated_at": "2026-09-09 18:19:01",
        "pinned": false,
        "archived": false
      },
      {
        "session_id": "f0d22921-7cf5-4e41-8f26-8830cbf66c41",
        "user_id": "7",
        "title": "产品推荐咨询",
        "last_message_preview": "好的，您问的是**申请贷款需要什么条件**，结合知识库中的相关信息，我为您详细梳理如下：\n\n---\n",
        "last_message_at": "2026-09-09 15:19:19",
        "message_count": 4,
        "created_at": "2026-09-09 15:14:36",
        "updated_at": "2026-09-09 15:19:19",
        "pinned": false,
        "archived": false
      },
      {
        "session_id": "e6fcdea4-7344-45bb-80a2-3c1f7f2b5e3e",
        "user_id": "7",
        "title": "我们只需要输出标题，10-15字以内，概",
        "last_message_preview": "您好！很高兴为您解答贷款产品推荐的问题 😊\n\n由于您暂时没有说明具体的资金需求，我先根据贷款产品的两",
        "last_message_at": "2026-09-09 15:05:12",
        "message_count": 2,
        "created_at": "2026-09-09 15:03:35",
        "updated_at": "2026-09-09 15:05:18",
        "pinned": false,
        "archived": false
      }
    ],
    "next_cursor": null,
    "has_more": false
  },
  "message": "获取会话列表成功"
}
```

> 排序：置顶（pinned desc）→ 更新时间倒序（updated_at desc）。游标分页，不用 skip。
>
> **进入上次对话**：客户端进入聊天页时调 `GET /api/sessions?limit=1&has_messages=true`，
> 从返回的会话列表中取第一条session_id，再调用 7 节消息接口获取会话详情，即可进入上一次会话，
> 空列表说明从未聊过，前端再调第 1 节 POST 新建。

---

## 3. 查询单个会话

网址：/api/sessions/{session_id}

请求方式：GET

请求示例（网址）：/api/sessions/85cb1e75-80dc-48a5-bf67-d680f275085a

返回结果（data 为 SessionItem 会话对象，字段说明见文首「通用响应结构与数据模型」）：

```json
{
  "code": 200,
  "data": {
    "session_id": "85cb1e75-80dc-48a5-bf67-d680f275085a",
    "user_id": "7",
    "title": "贷款月还款计算咨询",
    "last_message_preview": "计算结果如下：\n\n贷款金额：10000元\n贷款期限：12期（1年）\n年利率：0.000650%\n还款",
    "last_message_at": "2026-09-09 18:36:36",
    "message_count": 2,
    "created_at": "2026-09-09 18:36:36",
    "updated_at": "2026-09-09 18:36:39",
    "pinned": false,
    "archived": false
  },
  "message": "获取会话成功"
}
```

---

## 4. 更新会话

网址：/api/sessions/{session_id}

请求方式：PATCH

请求参数说明（请求体，任意子集）：

| 参数 | 描述 | 类型 | 示例 |
|---|---|---|---|
| title | 会话标题 | string | "贷款咨询" |
| pinned | 是否置顶 | bool | true |
| archived | 是否归档 | bool | false |

请求示例（网址）：/api/sessions/e6fcdea4-7344-45bb-80a2-3c1f7f2b5e3e

请求示例（请求体）：

```json
{
  "title": "产品咨询",
  "pinned": true,
  "archived": false
}
```

返回结果（data 为 SessionItem 会话对象，字段说明见文首「通用响应结构与数据模型」）：

```json
{
  "code": 200,
  "data": {
    "session_id": "e6fcdea4-7344-45bb-80a2-3c1f7f2b5e3e",
    "user_id": "7",
    "title": "产品咨询",
    "last_message_preview": "您好！很高兴为您解答贷款产品推荐的问题 😊\n\n由于您暂时没有说明具体的资金需求，我先根据贷款产品的两",
    "last_message_at": "2026-09-09 15:05:12",
    "message_count": 2,
    "created_at": "2026-09-09 15:03:35",
    "updated_at": "2026-09-10 11:32:30",
    "pinned": true,
    "archived": false
  },
  "message": "更新会话成功"
}
```

---

## 5. 置顶/取消置顶会话

网址：/api/sessions/{session_id}/pin

请求方式：POST

请求参数说明（请求体）：

| 参数 | 描述 | 类型 | 示例 |
|---|---|---|---|
| pinned | true=置顶，false=取消置顶 | bool | true |

请求示例（网址）：/api/sessions/c2f5be07-513e-4143-8a73-330375622b95/pin

请求示例（请求体）：

```json
{
  "pinned": true
}
```

返回结果（data 为 SessionItem 会话对象，字段说明见文首「通用响应结构与数据模型」）：

```json
{
  "code": 200,
  "data": {
    "session_id": "c2f5be07-513e-4143-8a73-330375622b95",
    "user_id": "7",
    "title": "贷款材料是否必须交",
    "last_message_preview": "**是的，申请贷款必须提交认证材料。**\n\n根据知识库中的官方信息，\"提供有效身份证明\"和\"完成实名",
    "last_message_at": "2026-09-09 18:18:59",
    "message_count": 2,
    "created_at": "2026-09-09 18:18:59",
    "updated_at": "2026-09-10 11:34:24",
    "pinned": true,
    "archived": false
  },
  "message": "置顶成功"
}
```

---

## 6. 删除会话

网址：/api/sessions/{session_id}

请求方式：DELETE

请求参数说明：

| 参数 | 描述 | 类型 | 示例 |
|---|---|---|---|
| session_id | 会话 ID | string | "332f7682-a7db-411b-8626-e7bfdc79c6ec" |

请求示例（网址）：/api/sessions/332f7682-a7db-411b-8626-e7bfdc79c6ec

返回结果：

```json
{
  "code": 200,
  "data": {
    "session_id": "332f7682-a7db-411b-8626-e7bfdc79c6ec"
  },
  "message": "删除会话成功"
}
```

data 字段说明：

| 字段 | 类型 | 说明 |
|---|---|---|
| session_id | string | 被删除的会话 ID |

> 事务删除：同时删除该会话的元信息和该会话下的全部消息。

---

## 7. 分页查询当前会话的消息

网址：/api/sessions/{session_id}/messages

请求方式：GET

请求参数说明（Query）：

| 参数 | 描述 | 类型 | 示例 |
|---|---|---|---|
| cursor | 上一页返回的 next_cursor，首次不传，取最新一页 | string | "" |
| limit | 每页数量，1-200，默认 30，可不填 | int | 30 |

请求示例（网址）：/api/sessions/f0d22921-7cf5-4e41-8f26-8830cbf66c41/messages?limit=30

返回结果（分页响应，items 元素为 MessageItem 消息对象；items/next_cursor/has_more 及 MessageItem 字段说明见文首「通用响应结构与数据模型」。首屏无 cursor 示例）：

```json
{
  "code": 200,
  "data": {
    "items": [
      {
        "message_id": "f68bdf04-da25-4597-9886-7dfffe4b83d7",
        "session_id": "f0d22921-7cf5-4e41-8f26-8830cbf66c41",
        "user_id": "7",
        "role": "user",
        "content": "有哪些产品推荐",
        "created_at": "2026-09-09 15:15:53"
      },
      {
        "message_id": "9b9f1a90-b960-4911-8dde-5ab148cd3595",
        "session_id": "f0d22921-7cf5-4e41-8f26-8830cbf66c41",
        "user_id": "7",
        "role": "assistant",
        "content": "您好！很高兴为您推荐贷款产品。由于每个贷款产品都对应不同的使用场景，我根据常见需求为您整理了一份**按场景匹配的产品推荐清单**，方便您对照参考：\n\n---\n\n### 🏠 场景一：购房 / 大额资金需求\n**推荐：抵押贷款（住房贷款）**\n- ✅ 额度通常较高，可满足大额资金需求\n- ✅ 利率相对较低，期限较长，适合长期还款\n- 📌 需提供合格抵押物（如房产），需经过评估和抵押登记，审批通常需 **1–2 周**，建议提前规划时间\n\n### 💳 场景二：装修、购车、教育、旅游等个人消费 / 短期周转\n**推荐：信用贷款（消费贷）**\n- ✅ 无需抵押物，手续简便\n- ✅ 审批速度快，通常 **1–3 个工作日**即可完成\n- 📌 适合收入稳定、征信良好的个人客户；注意额度相对有限、利率通常高于抵押贷款\n\n### 🏢 场景三：企业日常经营、扩大生产、资金周转\n**推荐：企业经营贷款**\n- ✅ 可满足企业大额资金需求，助力经营发展\n- 📌 需评估企业经营状况，审批链条相对复杂，耗时可能较长\n\n---\n\n### 📊 还款方式小贴士（供您参考）\n| 还款方式 | 特点 | 适合人群 |\n|---|---|---|\n| **等额本息** | 每月还款额固定，方便规划 | 收入稳定的工薪族 |\n| **等额本金** | 前期还款压力较大、总利息更低 | 前期还款能力较强的客户 |\n| **先息后本** | 前期只还利息，后期一次性还本 | 短期周转、资金回笼期明确的客户 |\n\n---\n\n如果您能告诉我更具体的信息，比如：\n1. **贷款用途**（买房、买车、装修、经营等）\n2. **贷款金额**和**期望期限**\n3. **个人还是企业申请**，以及大致收入/经营情况\n\n我可以帮您进一步缩小范围，推荐更精准的产品，还可以为您介绍具体**利率、申请条件、所需材料**或**申请流程**哦！期待您的补充，随时为您解答～ 😊",
        "created_at": "2026-09-09 15:15:53"
      },
      {
        "message_id": "1ed88d86-198d-4615-a2a2-d92d03fcdd71",
        "session_id": "f0d22921-7cf5-4e41-8f26-8830cbf66c41",
        "user_id": "7",
        "role": "user",
        "content": "申请贷款需要什么条件",
        "created_at": "2026-09-09 15:19:19"
      },
      {
        "message_id": "12793eaf-a93b-462e-91d7-dfc8d7a0692d",
        "session_id": "f0d22921-7cf5-4e41-8f26-8830cbf66c41",
        "user_id": "7",
        "role": "assistant",
        "content": "好的，您问的是**申请贷款需要什么条件**，结合知识库中的相关信息，我为您详细梳理如下：\n\n---\n\n### 📋 一、个人贷款申请的基本条件（通用）\n\n根据知识库信息，申请个人贷款通常需满足以下条件：\n\n1. **年龄要求**：年满18周岁，具有完全民事行为能力\n2. **收入要求**：有稳定的工作和收入来源，具备按时还款能力\n3. **信用要求**：信用记录良好\n4. **身份证明**：能够提供有效的身份证明\n5. **实名认证**：需完成实名认证\n\n---\n\n### 💡 二、不同贷款类型的资质侧重点\n\n虽然基本条件相同，但不同产品的**审核重点**会有所差异：\n\n| 贷款类型 | 资质侧重点 |\n|---|---|\n| **信用贷款（消费贷）** | 看重个人收入稳定性和征信记录，无需抵押物 |\n| **抵押贷款（住房/车辆）** | 除基本条件外，还需提供合格抵押物，并通过价值评估 |\n| **企业经营贷款** | 需评估企业实际经营状况、营收流水等 |\n\n---\n\n### 📄 三、审批时长参考\n\n- **信用贷款**：通常 **1–3 个工作日**\n- **抵押贷款**：涉及评估和抵押登记，通常需 **1–2 周**\n- 复杂业务可能需要更长时间，具体可咨询您的客户经理\n\n> 📌 小提示：以上为通用基础条件。不同贷款产品在**收入证明、银行流水、具体材料清单**等方面的细化要求可能有所不同，建议办理时以客户经理的最终告知为准。\n\n---\n\n### ✨ 为了更好地帮您匹配，方便告诉我几个信息吗？\n\n1. 🎯 **贷款用途**是什么？（买房、装修、购车、消费还是企业经营？）\n2. 👤 您是**个人申请**还是**以企业名义**申请？\n3. 💰 大致需要**多少金额**、期望的**期限**是多久？\n\n告诉我这些后，我可以为您进一步梳理对应的专属条件和申请建议哦～ 😊",
        "created_at": "2026-09-09 15:19:19"
      }
    ],
    "next_cursor": null,
    "has_more": false
  },
  "message": "获取消息列表成功"
}
```

**分页调用约定**：

- 首屏：不传 cursor，返回最新 N 条（已按 created_at 升序排好，前端可直接"从上到下"渲染，滚动锚定到底部）。
- 向上滑到顶：把当前最顶条目对应的 `next_cursor` 回传，请求更早一页。
- 向下查看已加载记录：无需请求，前端本地缓存直接渲染。
- `next_cursor` 为 null 表示没有更早的消息可加载。
- 返回的 items 始终按 created_at 升序，前端前置插入列表并保持滚动位置不跳动。
