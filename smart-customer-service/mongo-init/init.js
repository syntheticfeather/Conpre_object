// smart-customer-service/mongo-init/init.js

// 切换到应用数据库
db = db.getSiblingDB('smart_customer_service');

// 创建应用用户（可选，如果只用 root 可以省略）
db.createUser({
    user: 'app_user',
    pwd: 'app_password_123',
    roles: [
        {
            role: 'readWrite',
            db: 'smart_customer_service'
        }
    ]
});

// 创建集合
db.createCollection('prompts');
db.createCollection('mcp_servers');
db.createCollection('chat_sessions');  // 会话元信息（每会话一行）
db.createCollection('chat_messages');   // 消息明细（每条消息一行）

// 创建索引
// prompts 集合索引
db.prompts.createIndex({ "prompt_id": 1 }, { unique: true });
db.prompts.createIndex({ "name": 1 });
db.prompts.createIndex({ "category": 1 });
db.prompts.createIndex({ "is_active": 1 });

// mcp_servers 集合索引
db.mcp_servers.createIndex({ "server_id": 1 }, { unique: true });
db.mcp_servers.createIndex({ "created_at": -1 });

// chat_sessions 集合索引（会话列表查询）
db.chat_sessions.createIndex({ "session_id": 1 }, { unique: true });
db.chat_sessions.createIndex({ "user_id": 1, "updated_at": -1 });               // 按用户列会话
db.chat_sessions.createIndex({ "user_id": 1, "pinned": -1, "updated_at": -1 }); // 置顶排序

// chat_messages 集合索引（消息分页查询）
db.chat_messages.createIndex({ "message_id": 1 }, { unique: true });
db.chat_messages.createIndex({ "session_id": 1, "created_at": -1 }); // 向上加载更早（主路径）
db.chat_messages.createIndex({ "session_id": 1, "created_at": 1 });  // 向下加载更新

// 插入测试数据（可选）
// db.chat_history.insertOne({
//     session_id: "test_session_001",
//     user_id: "test_user",
//     "messages": [
//         {
//             "message_id": "msg_test_001",
//             "role": "user",
//             "content": "你好，我想咨询一下贷款",
//             "timestamp": new Date("2024-01-15T10:00:00Z"),
//         },
//         {
//             "message_id": "msg_test_003",
//             "role": "user",
//             "content": "我想了解一下个人消费贷",
//             "timestamp": new Date("2024-01-15T10:01:00Z"),
//         }
//     ],
//     created_at: new Date(),
//     updated_at: new Date()
// });

// ==================== 提示词种子（Phase 1 五层架构） ====================
// category：base（L1角色/L4语气/兜底规则）、general（低置信兜底通用规则）、
//           query_status/calculate/list_products/apply_loan/complaint/consult（意图片段）
// 已存在的库不重新执行 init.js；存量库迁移与播种运行 api/seed_prompts.py（幂等）。

var GENERAL_RULES = "1. 当用户提出具体问题时，首要任务是直接回答该问题，不要使用欢迎语。\n"+
    "2. **贷款申请状态/还款明细/信用分**：这些个人数据查询由系统自动处理（Workflow），Agent 不负责查询；如用户通过 Agent 问到，引导用户直接提问即可触发系统查询。\n"+
    "3. **产品推荐**：用户要求推荐适合的贷款产品时，使用 `recommend_products` 工具获取个性化推荐；该工具不可用时，引导用户查看产品列表自行选择，不得编造推荐。\n"+
    "4. **通用知识问答**：产品规则、政策、常见问题、办理流程等问题，必须先使用 `search_knowledge` 检索，基于检索结果回答，不要假设知识库内容。\n"+
    "5. **实时外部信息**：当前最新利率行情、政策新闻等外部信息，使用 `search_web` 搜索。\n"+
    "6. **未知问题处理**：检索无结果时，友好告知“暂无相关信息”，并可引导人工客服，不能编造。\n"+
    "7. 调用工具时直接调用，不要先询问用户是否需要调用。\n"+
    "8. 禁止使用“top-N”“top-k”等技术术语，用“为您推荐以下N款产品”代替。\n"+
    "9. 工具已成功返回数据时，回复必须基于该数据如实呈现，禁止丢弃数据或谎称“未获取到结果”；若数据存在疑问，按数据原样表述并提示以系统实际展示为准。";

db.prompts.insertMany([
{
    prompt_id: "prompt_base_default",
    name: "基础全局提示词（L1角色/L4语气/兜底规则）",
    category: "base",
    is_active: true,
    version: "1.0",
    content: {
        role_definition: "你是一个友好、专业的贷款智能客服，负责回答用户关于贷款产品、申请、还款等方面的问题。",
        business_rules: GENERAL_RULES,
        tone_style: "语气友好、专业，简洁明了；分点陈述复杂内容；不使用绝对化表述。"
    },
    created_at: new Date(), updated_at: new Date()
},
{
    prompt_id: "prompt_general_default",
    name: "通用场景片段（general 兜底）",
    category: "general",
    is_active: true,
    version: "1.0",
    content: { business_rules: GENERAL_RULES },
    created_at: new Date(), updated_at: new Date()
},
{
    prompt_id: "prompt_query_status_default",
    name: "意图片段-查询申请进度",
    category: "query_status",
    is_active: true,
    version: "1.0",
    content: {
        business_rules: "- 用户可能想查询自己的贷款申请进度/审批状态，该查询由系统 Workflow 自动处理，Agent 不负责查询个人数据。\n"+
            "- 如用户通过 Agent 问到申请进度，引导用户直接提问“查我的申请进度”即可触发系统查询，不要猜测进度。\n"+
            "- 可简要解释各审批状态（待补充资料、审核中、已批准、已拒绝）的含义。\n"+
            "- 不替用户解读审批结论，不承诺审批时效。",
        few_shots: [ { input: "我贷的钱批了没", expect: "引导用户直接提问查申请进度（系统 Workflow 处理），不检索知识库、不编造" } ],
        output_extra: "只展示与当前用户本人申请相关的信息，内部审核备注一律不展示。"
    },
    created_at: new Date(), updated_at: new Date()
},
{
    prompt_id: "prompt_calculate_default",
    name: "意图片段-还款计算",
    category: "calculate",
    is_active: true,
    version: "1.0",
    content: {
        business_rules: "- 用户可能需要月供、利息、总还款额、还款计划等具体数字，该计算由系统 Workflow 自动处理，Agent 严禁心算或估算后直接给出数字。\n"+
            "- 如用户通过 Agent 要求试算，引导用户直接给出金额/期限等要素提问（如“帮我算20万36期月供”）触发系统计算。\n"+
            "- 参数不全（金额/期限/利率/还款方式）时可一次性问全，不要逐项追问。\n"+
            "- 可解释等额本息/等额本金/先息后本的差异，但解释中不得自行编造具体数字。",
        few_shots: [ { input: "帮我算一下20万36期4.5%等额本息总利息", expect: "引导用户直接发起计算请求（系统 Workflow 处理），不心算、不从知识库找数字" } ],
        output_extra: "展示利率统一使用年化利率口径；数字结果后附“测算结果仅供参考，实际以合同约定为准”。"
    },
    created_at: new Date(), updated_at: new Date()
},
{
    prompt_id: "prompt_list_products_default",
    name: "意图片段-产品列表",
    category: "list_products",
    is_active: true,
    version: "1.0",
    content: {
        business_rules: "- 用户可能想了解可贷产品清单或某款产品详情，产品信息以 `search_knowledge` 检索结果为准，不得编造产品名、额度、利率。\n"+
            "- 列产品时尽量带出品类（个人消费/农业生产/企业经营）、额度区间与期限区间。\n"+
            "- 不主动承诺具体利率和放款结果，产品细节以产品页面为准。\n"+
            "- 用户给出金额/期限等条件时，可筛除明显不匹配的产品并说明筛选依据。",
        few_shots: [ { input: "我想借5万，有什么产品可以选", expect: "检索知识库后按额度区间筛选介绍，不承诺具体利率" } ],
        output_extra: "出现推荐措辞时提示“仅供参考，请结合自身还款能力谨慎决策”。"
    },
    created_at: new Date(), updated_at: new Date()
},
{
    prompt_id: "prompt_apply_loan_default",
    name: "意图片段-申请贷款",
    category: "apply_loan",
    is_active: true,
    version: "1.0",
    content: {
        business_rules: "- 用户可能想提交贷款申请；申请要素（产品、金额、期限、还款方式、资质信息）缺失时，一次性列全需补充的内容。\n"+
            "- 提交前向用户复述要素并请其明确确认，不得代用户确认或直接提交。\n"+
            "- 申请条件、材料清单类问题先用 `search_knowledge` 检索后回答。\n"+
            "- 不承诺审批结果，引导用户通过正规入口办理。",
        few_shots: [ { input: "我要申请贷款", expect: "先说明条件材料并一次性收集要素，确认前复述，不代用户提交" } ]
    },
    created_at: new Date(), updated_at: new Date()
},
{
    prompt_id: "prompt_complaint_default",
    name: "意图片段-投诉",
    category: "complaint",
    is_active: true,
    version: "1.0",
    content: {
        business_rules: "- 用户可能带有不满情绪，先共情安抚（如“非常理解您的心情”），再处理问题；不与用户争辩、不推卸责任。\n"+
            "- 不承诺赔偿、不代表公司认定责任；费用/违约金争议先用 `search_knowledge` 核对规则再回应。\n"+
            "- 需核实身份或订单时，引导用户通过安全渠道提供，不在对话中索取敏感信息。\n"+
            "- 达到升级条件（强烈不满、重复投诉、涉及资金损失）时，明确引导人工客服或投诉热线。",
        few_shots: [ { input: "提前还款扣了我违约金，这不合理吧", expect: "先安抚，再核对违约金规则说明，不承诺退款，必要时引导人工" } ]
    },
    created_at: new Date(), updated_at: new Date()
},
{
    prompt_id: "prompt_consult_default",
    name: "意图片段-咨询/推荐/评估",
    category: "consult",
    is_active: true,
    version: "1.0",
    content: {
        business_rules: "- 用户可能在咨询规则、做对比或要建议；规则/政策/流程类问题先 `search_knowledge` 检索，建议必须有检索或工具依据。\n"+
            "- 用户要求个性化产品推荐时（“帮我推荐个产品”“哪个适合我”），调用 `recommend_products` 工具，基于返回结果如实介绍，禁止丢弃工具已返回的数据。\n"+
            "- 对比类问题（还款方式、产品选择）列出关键维度差异，不做绝对化判断。\n"+
            "- 最新利率行情、政策新闻等外部信息用 `search_web`。\n"+
            "- 资金用途等合规红线问题要明确回答（如贷款资金不得用于炒股、购房等违规用途）；知识库无明确条目时依据通用合规要求说明并引导人工确认，不要回避问题。",
        few_shots: [ { input: "利息是怎么算的", expect: "这是问规则，检索知识库解释计息方式，不直接给某笔账的数字" },
            { input: "帮我推荐个产品", expect: "调用 recommend_products 获取个性化推荐，基于结果介绍，不说技术术语、不谎称无结果" } ],
        output_extra: "给建议时用“建议您结合自身情况”等表述，避免“一定”“肯定”等绝对化措辞。"
    },
    created_at: new Date(), updated_at: new Date()
}
]);

// 同 category 激活互斥由应用层 update_prompt 保证；种子数据每 category 仅一条且均激活。

// 插入示例MCP服务器配置
// db.mcp_servers.insertOne({
//     server_id: "brave_search",
//     config: {
//         url: "https://brave-search-mcp.example.com/sse",  // 实际部署地址
//         api_key: "YOUR_BRAVE_SEARCH_API_KEY_HERE",  // 申请后替换
//         transport: "sse",
//         timeout: 30000
//     },
//     created_at: new Date(),
//     updated_at: new Date()
// });

// {
//     "server_id": "fetch-tool",
//     "config": {
//         "transport": "stdio",
//         "command": "npx",
//         "args": ["-y", "@modelcontextprotocol/server-fetch"],
//         "timeout": 60
//     }
// }

// ==================== chat_sessions / chat_messages 示例数据 ====================
// 用于联调"向上滑动加载更多"和"左侧会话列表"分页接口

var now = new Date();
var sid1 = "demo_session_001";   // 一条置顶会话
var sid2 = "demo_session_002";   // 一条普通会话

db.chat_sessions.insertMany([
    {
        session_id: sid1,
        user_id: "demo_user",
        title: "个人消费贷咨询",
        last_message_preview: "好的，已为您生成等额本息还款计划",
        last_message_at: new Date(now.getTime() - 60 * 1000),    // 1 分钟前
        message_count: 3,
        created_at: new Date(now.getTime() - 60 * 60 * 1000),    // 1 小时前
        updated_at: new Date(now.getTime() - 60 * 1000),
        pinned: true,
        archived: false
    },
    {
        session_id: sid2,
        user_id: "demo_user",
        title: "新对话 " + new Date(now.getTime() - 30 * 60 * 1000).toISOString().slice(0,16).replace("T"," "),
        last_message_preview: "您也可以选择先息后本的方式",
        last_message_at: new Date(now.getTime() - 30 * 60 * 1000), // 30 分钟前
        message_count: 0,
        created_at: new Date(now.getTime() - 30 * 60 * 1000),
        updated_at: new Date(now.getTime() - 30 * 60 * 1000),
        pinned: false,
        archived: false
    }
]);

db.chat_messages.insertMany([
    // session1 的 3 条消息（时间从早到晚）
    {
        message_id: "demo_msg_001",
        session_id: sid1,
        user_id: "demo_user",
        role: "user",
        content: "你好，我想咨询个人消费贷",
        created_at: new Date(now.getTime() - 60 * 60 * 1000)       // 1 小时前
    },
    {
        message_id: "demo_msg_002",
        session_id: sid1,
        user_id: "demo_user",
        role: "assistant",
        content: "您好！很高兴为您服务。请问您想了解额度、利率还是还款方式？",
        created_at: new Date(now.getTime() - 59 * 60 * 1000)      // 59 分钟前
    },
    {
        message_id: "demo_msg_003",
        session_id: sid1,
        user_id: "demo_user",
        role: "assistant",
        content: "好的，已为您生成等额本息还款计划，月供约 3320 元",
        created_at: new Date(now.getTime() - 60 * 1000)           // 1 分钟前
    }
]);
