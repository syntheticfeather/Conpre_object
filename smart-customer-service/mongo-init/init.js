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

// 创建索引
db.chat_history.createIndex({ "session_id": 1 }, { unique: true });
db.chat_history.createIndex({ "user_id": 1, "updated_at": -1 });

// prompts 集合索引
db.prompts.createIndex({ "prompt_id": 1 }, { unique: true });
db.prompts.createIndex({ "name": 1 });
db.prompts.createIndex({ "category": 1 });
db.prompts.createIndex({ "is_active": 1 });

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
            "tokens": 10
        },
        {
            "message_id": "msg_test_003",
            "role": "user",
            "content": "我想了解一下个人消费贷",
            "timestamp": new Date("2024-01-15T10:01:00Z"),
            "tokens": 8
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
        role_definition: "你是一个友好的贷款智能客服。",
        business_rules: "1. **贷款申请状态**：如果用户询问进度或状态"+
                        "必须使用 `query_application_status` 工具。\n"+
                        "2. **还款计算**：如果用户询问月供、利息或计划，必须使用 `calculate_repayment` 工具。\n"+
                        "3. **通用知识问答**：对于所有其他问题（包括产品规则、政策、常见问题等），必须先使用 `search_knowledge` 工具在知识库中检索相关信息，基于检索结果回答。\n   "+
                        "- *注意：不要假设知识库内容，必须显式调用此工具获取最新信息。*\n"+
                        "4. **未知问题处理**：如果 `search_knowledge` 返回无结果，友好地回答暂无相关信息，不能编造信息。\n"+
                        "5. **获取实时信息**：如果用户询问当前或现在最新的外部信息，使用 `search_web` 工具搜索网络。\n6. **拒绝行为**：严禁编造数据，严禁提及Token等技术术语。",
        tone_style: "语气友好、专业，简洁明了。调用工具时，直接调用，不要询问用户是否需要调用。"
    },
    config: {
        protected_tools: [
            {
                name: "query_application_status",
                description: "查询贷款申请状态"
            },
            {
                name: "calculate_repayment",
                description: "计算贷款还款计划"
            },
            {
                name: "search_knowledge",
                description: "在知识库中搜索相关信息"
            },
            {
                name: "search_web",
                description: "搜索网络获取实时信息"
            }
        ],
        variables: ["current_date"]
    },
    created_at: new Date(),
    updated_at: new Date()
});

print('========================================');
print('Smart Customer Service DB initialized!');
print('Database: smart_customer_service');
print('Collections: chat_history, prompts');
print('========================================');