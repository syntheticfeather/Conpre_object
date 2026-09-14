# smart-customer-service/api/chat_routes.py
"""
对话落库路由 —— WORKFLOW 路径的补录入口。

背景：
  /api/chat/stream（AGENT 路径）在 ChatAgent 内部完成会话/消息持久化；
  但 WORKFLOW 请求（查进度/算月供/列产品/申请贷款）由 Java 本地处理，
  不经过本服务。Java workflow 处理完后回调本接口，把这一轮对话补录进
  chat_sessions / chat_messages，保证两条链路的聊天记录完整一致。

  - POST /api/chat/persist   补录一轮对话（user 原话 + workflow 最终回复）

鉴权：与 /api/chat/stream 一致，透传用户 Bearer Token，user_id 从 token 解析。
"""
import sys
import os

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from fastapi import APIRouter, HTTPException, Depends
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials

from api.models import ResponseModel, ChatTurnPersistRequest
from api.utils import ResultUtil
from memory.chat_history_service import chat_history_service
from utils.token_utils import extract_user_id_from_token

router = APIRouter(prefix="/api/chat", tags=["chat"])
security = HTTPBearer()


@router.post("/persist", response_model=ResponseModel)
async def persist_chat_turn(
    body: ChatTurnPersistRequest,
    credentials: HTTPAuthorizationCredentials = Depends(security),
):
    """
    补录一轮 WORKFLOW 对话到聊天记录。

    - 会话不存在时自动用 session_id 兜底创建（跳过空会话复用）
    - 追加 user / assistant 两条消息（仅用户原话 + 最终回复）
    - 会话首条消息时自动触发 LLM 标题回填
    """
    user_id = extract_user_id_from_token(credentials.credentials)
    if not user_id:
        raise HTTPException(status_code=401, detail="无效的 token，无法识别用户")

    await chat_history_service.persist_turn(
        session_id=body.session_id,
        user_id=user_id,
        user_message=body.message,
        assistant_response=body.response,
    )
    return ResultUtil.success(message="对话已持久化")
