# smart-customer-service/api/main.py
import sys
import os

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
import asyncio
import json
from utils.context import set_token
from utils.token_utils import extract_user_id_from_token

from api.knowledge_routes import router as knowledge_router
from api.tool_routes import router as tool_router
from api.prompt_routes import router as prompt_router
from agent.chat_agent import get_chat_agent

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

# 挂载路由
app.include_router(knowledge_router, dependencies=[Depends(security)])
app.include_router(tool_router, dependencies=[Depends(security)])
app.include_router(prompt_router, dependencies=[Depends(security)])

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
    # 设置 token 到 contextvars
    set_token(token)

    # 从 token 中解析 user_id
    user_id = extract_user_id_from_token(token)

    # 从请求体中获取 message 和 session_id
    message = body.message
    session_id = body.session_id or str(uuid.uuid4())

    # 获取 ChatAgent 单例实例
    agent = get_chat_agent()

    # 异步生成事件流
    async def event_generator():
        full_response = ""
        # 第一个数据块：发送session_id
        yield {"event": "session_init", "data": json.dumps({"session_id": session_id})}

        try:
            # 设置 60 秒超时控制
            async with asyncio.timeout(60):
            
                # 流式接收与分发
                async for chunk in agent.chat(message, session_id, user_id, token):
                    # 尝试解析 JSON 格式的事件
                    try:
                        event_data = json.loads(chunk)
                        event_type = event_data.get("type", "")

                        if event_type == "tool_call":
                            # 处理工具调用事件
                            yield {
                                "event": "tool_call",
                                "data": {
                                    "tool_name": event_data.get("tool_name", ""),
                                    "arguments": event_data.get("arguments", {})
                                }
                            }
                        elif event_type == "tool_result":
                            # 处理工具执行结果事件
                            yield {
                                "event": "tool_result",
                                "data": {
                                    "tool_name": event_data.get("tool_name", ""),
                                    "result": event_data.get("result", None)
                                }
                            }
                        elif event_type == "error":
                            # 处理错误事件
                            yield {"event": "error", "data": event_data.get("message", "")}

                        elif event_type == "message":
                            content = event_data.get("content", "")
                            if content:
                                full_response += content
                                yield {"event": "message", "data": content}
                        # 如果还有 done 事件等，也可以在这里处理
                        
                    except json.JSONDecodeError:
                        # 如果不是 JSON 格式，当作普通文本消息处理
                        full_response += chunk
                        yield {"event": "message", "data": chunk}
        except asyncio.TimeoutError:
            # 处理超时错误
            logger.error("请求超时")
            yield {"event": "error", "data": "请求超时，请稍后重试"}
        except Exception as e:
            # 处理其他错误
            logger.error(f"发生错误：{str(e)}")
            yield {"event": "error", "data": f"发生错误：{str(e)}"}

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
