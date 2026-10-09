# 提示词精细化 Phase 0 — 评测集与基线建设

> 时间：2026-09-22
> 范围：`test/eval_cases.json`、`test/benchmark_prompt.py`、`test/benchmark_results/`
> 关联：[提示词精细化.md](./提示词精细化.md) 第 3.9 节、Phase 0 实施路线

## 1. 改造总览

### 1.1 背景与目标

提示词精细化方案要求：任何提示词改动（意图片段 / Router / 工具描述）落地前，必须能跑离线评测集回归，回答"这次改动是变好了还是变坏了"。Phase 0 不改动任何线上代码，只交付两样东西：

1. **评测用例集**：100 条标注 case（route 60 + tool 20 + answer 20）
2. **基准脚本 + 基线报告**：量化现状，作为 Phase 1/2 改造后的回归对照

### 1.2 核心流程

```
test/eval_cases.json（100 条标注用例）
        │
        ├─ route 组（60）── 真实 HTTP POST /api/route ──→ 意图准确率/分流正确率/置信度校准表
        │
        └─ tool/answer 组（各 20）
              │  进程内直调真实 ChatAgent(react) 链
              │  （真实 LLM + 真实工具 + 真实落库，非 mock）
              │  启动前 await tool_manager.load_mcp_servers_from_db() 复刻应用启动
              │
              ├─ tool   → 工具命中率 / 误调用率 / skip 统计
              └─ answer → 要点覆盖率 / 通过率 / 禁词违规
        │
        ▼
test/benchmark_results/prompt_eval_baseline_20260922.json（统一基线，100 条明细）
```

**为什么 tool/answer 组不走 HTTP**：`api/main.py` 第 124-128 行在 SSE 层过滤了 `tool_call/tool_result` 事件，HTTP 无法观察实际工具调用。进程内直调可收到 ReactAgent 产出的原始 `tool_call` chunk，且与 `benchmark_rag.py` 的组件实测模式一致（均为真实调用，非 mock）。

## 2. 基线结果（2026-09-22 单次实测）

### 2.1 route 组：意图准确率 93.3%（56/60）

| 指标 | 结果 |
|---|---|
| 意图准确率 | **93.3%**（56/60） |
| 分流正确率（38 条标了期望 action） | **97.4%**（37/38） |
| easy / boundary / adversarial | 97% / 90% / 89% |
| 平均延迟 | 约 3.4s/条（含一次 35.2s 抖动） |

分意图：

| 意图 | 正确/总数 | 准确率 |
|---|---|---|
| APPLY_LOAN | 5/5 | 100% |
| COMPLAINT | 5/5 | 100% |
| LIST_PRODUCTS | 7/7 | 100% |
| CONSULT | 25/26 | 96% |
| QUERY_STATUS | 8/9 | 89% |
| CALCULATE | 6/8 | **75%（最弱）** |

**置信度校准表（自报分数段 → 该段实际准确率）**：

| 自报分段 | 样本数 | 实际准确率 | 解读 |
|---|---|---|---|
| 0.5-0.6 | 16 | 81% | 模型打 0.5 时并非纯瞎猜；**方案中透传门槛初值 0.6 基本成立** |
| 0.7-0.8 | 3 | 67% | 样本少，暂不可信 |
| 0.8-0.9 | 9 | 100% | 分流阈值 0.8 在本组数据下可靠 |
| 0.9-1.0 | 32 | 100% | 高置信区完全可信 |

4 条错例（已成为 Phase 2 few-shot 的首批候选）：

| case | 输入 | 期望 → 实际 | 性质 |
|---|---|---|---|
| route_014 | 帮我算一下20万36期4.5%等额本息**总利息** | CALCULATE → CONSULT(0.5) | "算…总利息"句式漏判；同款明确句式 route_010 正确，模型行为不稳定 |
| route_007 | 申请提交三天了还没消息正常吗 | QUERY_STATUS → CONSULT(0.5) | 进度+情绪混合句 |
| route_040 | APP一直闪退，怎么回事啊 | CONSULT → COMPLAINT(0.78) | 技术求助被当成投诉 |
| route_057 | 帮我算下月供，顺便查下我申请到哪了 | CALCULATE → CONSULT(0.5) | 多意图混杂无 tie-break 规则 |

### 2.2 tool 组：命中率 100%（12 条执行），8 条 skip

| 指标 | 结果 |
|---|---|
| 命中率 | **100%**（12/12，期望工具均被调用） |
| 误调用 | **0**（2 条误用反例均未误触 search_knowledge） |
| skip | 8 条（期望的 MCP 工具未加载，不计失败） |
| 平均延迟 | 10.8s/条 |

- search_knowledge 正例 8 条、search_web 正例 4 条全部命中
- 8 条 skip：`calculate_repayment` ×5（tool_013~016、020）、`query_application_status` ×3（tool_017~019）

  **真实原因（已查库核实）**：MongoDB `mcp_servers` 集合为空（0 条配置），Java 侧也尚无 MCP Server 实现——这两个业务工具目前只存在于 Java WORKFLOW 路径内部（`CalculateHandler` 等），**尚未通过 MCP 暴露给 Python Agent**，AGENT 路径的工具清单里只有 `search_knowledge`/`search_web`。这不是"跑分环境没启动"，而是"Java 业务能力 → MCP 工具"这座桥还没修（即五步扩展方案第三步"新增工具"的目标形态，用例已按目标工具名预先备好）。桥建好、`mcp_servers` 配置生效后重跑 tool 组，这 8 条自动从 skip 转为实测。

  > 概念区分：tool 组评测的是 Python AGENT 路径的自主选工具行为，不是 Java WORKFLOW。WORKFLOW 路径由 Java Handler 直接处理、不经过 Python Agent；MCP 是反向通道（Python Agent → Java MCP Server SSE），用于让 Agent 调用 Java 侧的计算/查询能力。

### 2.3 answer 组：通过率 85%（17/20），禁词违规 0

| 指标 | 结果 |
|---|---|
| 通过率 | **85%**（17/20） |
| 平均要点覆盖率 | 90% |
| 禁词违规（全局+case 禁词） | **0**（合规底线本次守住） |
| 平均延迟 | 10.6s/条 |

3 条失败经人工复核分两类：

- **真实失败 1 条**：answer_007"贷款资金可以用来炒股吗"——知识库无资金用途条目，Agent 未按规则明确否定，而是回避问题让问客户经理。属**知识缺口 + 过度保守**，需要在知识条目和 CONSULT 意图片段中补"资金用途禁止性规则"
- **判定严格性 2 条**：answer_002 / answer_017 回答完整，但用"**信用记录**良好"表述，未出现"征信"字面词导致关键词未命中。语义合格，属关键词匹配的固有局限，Phase 3 引入 LLM-as-judge 后消除；**本次不回溯改用例，保留基线原貌**

另记录一个用例弱点：answer_010/011（产品列表/推荐）仅凭弱关键词"产品"通过，但实际内容是"知识库暂无产品清单"——与 9 月 22 日线上实测一致，知识库产品类内容缺口真实存在。

## 3. 详细变更清单

| 文件路径 | 类型 | 作用说明 |
|---|---|---|
| `test/eval_cases.json` | 新增 | 100 条标注用例（route 60 / tool 20 / answer 20）+ 全局禁词表 |
| `test/benchmark_prompt.py` | 新增 | 评测基准脚本：route 走 HTTP，tool/answer 进程内直调 react 链，出指标与 JSON 报告 |
| `test/benchmark_results/prompt_eval_baseline_20260922.json` | 新增 | 统一基线（合并两组跑分，含 100 条逐条明细） |
| `test/benchmark_results/prompt_eval_report_20260922_144425.json` | 新增 | route 组原始跑分报告（溯源留档） |
| `test/benchmark_results/prompt_eval_report_20260922_145334.json` | 新增 | tool/answer 组原始跑分报告（溯源留档） |

线上代码（`api/`、`agent/`、`tools/` 等）**零改动**。

### 3.1 用例结构（eval_cases.json）

```json
{
  "case_id": "route_001 / tool_001 / answer_001",
  "group": "route | tool | answer",
  "input": "用户消息原文",
  "difficulty": "easy | boundary | adversarial",
  "expected": {
    "intent": "CALCULATE",            // route 组
    "action": "WORKFLOW",             // route 组，可选（边界样本不标）
    "tools": ["search_knowledge"],    // tool 组：期望调用
    "forbidden_tools": ["search_knowledge"], // tool 组：误用反例
    "must_contain": ["征信"],          // answer 组：要点
    "must_not_contain": ["包过"],      // answer 组：禁词
    "match": "all | any",             // answer 组，默认 all
    "min_length": 20                  // answer 组，默认 15
  },
  "note": "考察点说明"
}
```

route 组构成：每意图 4~6 条 easy 正例 + 跨意图 boundary 对（利息怎么算 vs 算账、查进度 vs 咨询、要建议 vs 要列表等）+ 9 条 adversarial（闲聊、跨域、单词、提示词注入、多意图混杂）。

### 3.2 评测判定规则（benchmark_prompt.py 关键逻辑）

- **route**：意图精确匹配；置信度归入 6 个分数段统计实际准确率（校准表）；`expected.action` 存在时额外计分流正确率
- **tool**：期望工具任一被调用即命中；`forbidden_tools` 被调用计误用；期望工具不在启动时已加载工具清单中 → `skipped`（不计失败）
- **answer**：覆盖率 = 命中要点数/要点总数（`any` 模式取任一命中）；通过条件 = 覆盖率 ≥ 0.66 且 长度 ≥ min_length 且 无禁词 且 无执行错误；全局禁词对每条生效
- **隔离**：tool/answer 每条用独立会话 `bench-<case_id>-<时分秒>` + 专用用户 `userId=999999`（脚本用与 Java 对齐的 JWT 密钥本地签发），不污染真实用户历史；走完整 ChatAgent 链路（含真实落库与异步记忆提取）

## 4. 配置与依赖

- 新增环境变量：无。可选 `AGENT_URL`（默认 `http://localhost:8000`，仅 route 组使用）
- 新增第三方依赖：无（PyJWT、langchain 等均为项目既有依赖）
- 运行环境：必须使用项目虚拟环境 `.venv\Scripts\python.exe`（全局解释器无 langchain）
- 前置服务：
  - route 组：Python 服务在 8000 端口
  - tool/answer 组：MongoDB 可用；8 条 MCP 类 case（calculate/query_status）依赖 Java 侧 MCP Server 已在 `mcp_servers` 集合配置且连通，当前该通道未建（集合为空），8 条自动 skip

## 5. 测试与验证

执行命令（`cd smart-customer-service`）：

```powershell
# 全量（route 需要 Python 服务在线；tool/answer 需要 MongoDB）
$env:PYTHONIOENCODING="utf-8"
.\.venv\Scripts\python.exe test/benchmark_prompt.py

# 只跑某几组
.\.venv\Scripts\python.exe test/benchmark_prompt.py --groups route
.\.venv\Scripts\python.exe test/benchmark_prompt.py --groups tool,answer
```

预期输出（节选，与本次基线一致即通过回归）：

```
[route] 意图准确率: 93.3% (60 条)  分流正确率: 97.4% (38 条有期望动作)
  置信度校准（自报分数段 → 该段实际准确率）:
    0.5-0.6: n=16  实际=81%
    0.8-0.9: n=9   实际=100%
    0.9-1.0: n=32  实际=100%
[tool] 命中率: 100.0%  执行 12 条 / skip 8 条 / 误用 0 条
[answer] 通过率: 85.0%  平均要点覆盖: 90%  执行 20 条 / 禁词违规 0 条
报告已保存: .../test/benchmark_results/prompt_eval_report_<时间戳>.json
```

回归判定（Phase 1/2 使用）：route 总体准确率与分流正确率不低于基线；tool 命中率不降、误用不升；answer 禁词违规必须为 0（硬门禁）。

## 6. 注意事项与后续

1. **MCP 用例待通道建成后补跑**：当前 `mcp_servers` 集合为空、Java 侧无 MCP Server 实现，8 条 MCP 类 case 处于 skip。待五步方案第三步（Java 以 MCP 暴露 calculate_repayment / query_application_status）落地并配置连通后重跑 tool 组，tool 组基线才完整
2. **单次基线有随机性**：线上 LLM temperature=0.7，报告为单次实测；后续可考虑同一用例跑 3 次取多数，成本允许时再做
3. **关键词判定局限**：answer_002/017 的"信用记录 vs 征信"问题留待 Phase 3 LLM-as-judge；当前不为此放宽用例
4. **已知知识缺口**（评测暴露的真实问题，移交后续步骤）：① 产品列表/推荐类内容缺失（answer_010/011、线上实测同款）；② 资金用途禁止性规则缺失（answer_007）
5. **Reranker 既有故障**：跑分日志中大量 `Reranker API 调用失败: ... missing 'cast_to'`，为 openai SDK 版本兼容问题，非本次引入，但会影响 search_knowledge 召回质量，需单独立项修复
6. **落库残留**：跑分在 MongoDB 产生 `bench-*` 会话（userId=999999，约 40 个会话/全量一次），如需清理按 session_id 前缀 `bench-` 删除即可
7. **Phase 1/2 对接点**：4 条 route 错例直接进 Router few-shot 库；透传门槛 0.6 获得校准数据初步支持（0.5-0.6 段实际 81%），Phase 2 few-shot 上线后需重跑校准
