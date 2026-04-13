# smart-customer-service/api/main.py
import sys
import os

# 添加项目根目录到 Python 搜索路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from fastapi import FastAPI, Depends, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from sse_starlette.sse import EventSourceResponse
from fastapi.exceptions import RequestValidationError # 导入 RequestValidationError 异常类型
from fastapi.responses import JSONResponse # 导入 JSONResponse 响应类型
from api.utils import ResultUtil
from typing import Dict
from api.models import ChatRequest
import uuid
import logging

from api.knowledge_routes import router as knowledge_router
from agent.chat_agent import ChatAgent

# 定义 Bearer Token 认证
security = HTTPBearer()

# 创建 FastAPI 应用实例
app = FastAPI(title="智能客服 API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,  # 允许携带 Cookie 或 Token 等凭证信息
    allow_methods=["*"],
    allow_headers=["*"],
)

# 挂载路由api.knowledge_routes
app.include_router(knowledge_router,dependencies=[Depends(security)])

# 会话存储（生产环境用 Redis）
sessions: Dict[str, list] = {}

# --- 配置日志系统 ---
logging.basicConfig(
    level=logging.INFO,  # 日志级别：INFO, ERROR 等
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s", # 日志格式
    handlers=[
        logging.StreamHandler(), # 输出到控制台
    ]
)

# 创建一个日志记录器实例
logger = logging.getLogger(__name__)

@app.post("/api/chat/stream")
async def chat_stream(body: ChatRequest, credentials: HTTPAuthorizationCredentials = Depends(security)):
    # 获取 token
    token = credentials.credentials 

    # 从请求体中获取 message 和 session_id
    message = body.message
    session_id = body.session_id or str(uuid.uuid4())

    # 初始化会话历史
    if session_id not in sessions:
        sessions[session_id] = []

    # 实例化 ChatAgent
    agent = ChatAgent()

    # 异步生成事件流
    async def event_generator():
        # 获取会话历史
        chat_history = sessions[session_id]
        full_response = ""

        # 流式接收与分发
        async for chunk in agent.chat(message, token, chat_history):
            # 处理工具调用
            if chunk.startswith("[tool_call]"):
                yield {"event": "tool", "data": chunk}
            # 处理普通消息
            else:
                full_response += chunk
                yield {"event": "message", "data": chunk}

        # 保存对话历史
        sessions[session_id].append({"role": "user", "content": message})
        sessions[session_id].append({"role": "assistant", "content": full_response})

    return EventSourceResponse(event_generator())

# 1. 捕获所有未处理的系统异常
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    # 记录日志
    logger.error(
        f"500 Internal Server Error: {request.method} {request.url.path}", 
        # 打印堆栈
        exc_info=True
    )
    return JSONResponse(
        status_code=500,
        content=ResultUtil.error(code=500, message="系统内部错误").model_dump()
    )

# 2. 捕获参数校验异常 (Pydantic 报错)
@app.exception_handler(RequestValidationError)
async def validation_exception_handler(request: Request, exc: RequestValidationError):
    # 记录日志
    logger.error(
        f"400 Bad Request: {exc.errors()}", 
        # 打印堆栈
        exc_info=True
    )
    return JSONResponse(
        status_code=400,
        content=ResultUtil.error(code=400, message="参数校验失败").model_dump()
    )

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)