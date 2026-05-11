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
db.createCollection('chat_history')
db.createCollection('prompts');
db.createCollection('mcp_servers');  // 新增MCP服务器配置集合

// 创建索引
db.chat_history.createIndex({ "session_id": 1 }, { unique: true });
db.chat_history.createIndex({ "user_id": 1, "updated_at": -1 });

// prompts 集合索引
db.prompts.createIndex({ "prompt_id": 1 }, { unique: true });
db.prompts.createIndex({ "name": 1 });
db.prompts.createIndex({ "category": 1 });
db.prompts.createIndex({ "is_active": 1 });

// mcp_servers 集合索引
db.mcp_servers.createIndex({ "server_id": 1 }, { unique: true });
db.mcp_servers.createIndex({ "created_at": -1 });

// 插入测试数据（可选）
db.chat_history.insertOne({
    session_id: "test_session_001",
    user_id: "test_user",
    "messages": [
        {
            "message_id": "msg_test_001",
            "role": "user",
            "content": "你好，我想咨询一下贷款",
            "timestamp": new Date("2024-01-15T10:00:00Z"),
        },
        {
            "message_id": "msg_test_003",
            "role": "user",
            "content": "我想了解一下个人消费贷",
            "timestamp": new Date("2024-01-15T10:01:00Z"),
        }
    ],
    created_at: new Date(),
    updated_at: new Date()
});

// 插入默认提示词数据
db.prompts.insertOne({
    prompt_id: "default_prompt",
    name: "默认贷款智能客服提示词",
    category: "loan_customer_service",
    is_active: true,
    version: "1.0",
    content: {
        role_definition: "你是一个友好的贷款智能客服，能回答用户关于贷款的问题。",
        business_rules: "1. 当用户提出具体问题时，你的首要任务是直接回答该问题。\n"+
                        "2. 只有在用户首次打招呼（如单独的'你好'）且没有提出具体问题时，才使用欢迎语\n"+
                        "3. 如果用户的消息包含具体问题，回答该问题而不是使用欢迎语。\n"+
                        "4. **贷款申请状态**：如果用户让你帮忙查询进度或状态，"+
                        "使用 `query_application_status` 工具。\n"+
                        "5. **还款计算**：如果用户让你帮忙计算月供、利息或计划，使用 `calculate_repayment` 工具。\n"+
                        "6. **通用知识问答**：对于所有其他问题（包括产品规则、政策、常见问题、如何操作等），必须先使用 `search_knowledge` 工具在知识库中检索相关信息，基于检索结果回答。\n   "+
                        "- *注意：不要假设知识库内容，必须显式调用此工具获取最新信息。*\n"+
                        "7. **未知问题处理**：如果 `search_knowledge` 返回无结果，友好地回答用户暂无相关信息，引导用户转向人工客服，不能编造信息。\n"+
                        "8. **获取实时信息**：如果用户询问当前或现在最新的外部信息，使用 `search_web` 工具搜索网络。\n"+
                        "9. **拒绝行为**：严禁编造数据，严禁提及Token等技术术语，严禁提及个人隐私信息如手机号、身份证号、银行卡号等。",
        tone_style: "语气友好、专业，简洁明了。调用工具时，直接调用，不要询问用户是否需要调用。"
    },
    created_at: new Date(),
    updated_at: new Date()
});

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