# smart-customer-service/api/message_routes.py
"""
消息分页路由 — 配套 utils/mongodb_client.py 中 chat_messages 集合。

实现"向上滑动加载更早历史"的核心能力：
  GET /api/sessions/{session_id}/messages
      ?cursor=<base64>             （上一页返回的 next_cursor）
      &limit=30

  返回：{ items: [...], next_cursor: "...", has_more: true }

调用约定：
  - 首屏：不传 cursor，返回最新 N 条（已按时间升序排好，前端可直接渲染）。
  - 上滑到顶：把当前最顶一条的 next_cursor 回传，继续请求更早的消息。
  - 用户向下查看已加载记录无需再请求（前端本地缓存即可）。
"""
import sys
import os

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from fastapi import APIRouter, HTTPException, Depends, Query
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from typing import Optional

from api.models import ResponseModel, MessageItem, MessageListResponse
from api.utils import ResultUtil
from utils.mongodb_client import mongodb_client
from utils.token_utils import extract_user_id_from_token

router = APIRouter(prefix="/api/sessions", tags=["messages"])
security = HTTPBearer()


def _current_user_id(credentials: HTTPAuthorizationCredentials) -> str:
    """从 Bearer Token 中解析 user_id，鉴权失败直接 401。"""
    user_id = extract_user_id_from_token(credentials.credentials)
    if not user_id:
        raise HTTPException(status_code=401, detail="无效的 token，无法识别用户")
    return user_id


def _to_message_item(doc: dict) -> MessageItem:
    """MongoDB 文档 → MessageItem 响应模型"""
    return MessageItem(
        message_id=doc.get("message_id", ""),
        session_id=doc.get("session_id", ""),
        user_id=doc.get("user_id"),
        role=doc.get("role", "user"),
        content=doc.get("content", ""),
        created_at=doc.get("created_at"),
    )


# ==================== 路由 ====================

@router.get("/{session_id}/messages", response_model=ResponseModel[MessageListResponse])
def list_messages(
    session_id: str,
    cursor: Optional[str] = Query(None, description="上一页返回的 next_cursor"),
    limit: int = Query(30, ge=1, le=200, description="每页数量"),
    credentials: HTTPAuthorizationCredentials = Depends(security),
):
    """
    分页查询某会话的消息（仅向上加载更早）。

    - 首屏无 cursor → 返回最新 N 条
    - 带 cursor → created_at < cursor.ts（同毫秒用 message_id 兜底）

    返回的 items 已按 created_at 升序排好，前端可直接"从上到下"渲染。
    用户向下查看已加载记录无需再请求，前端本地缓存即可。
    """
    user_id = _current_user_id(credentials)

    # 校验会话存在且属于当前用户（不存在/越权一律 404）
    session = mongodb_client.get_session(session_id)
    if not session or session.get("user_id") != user_id:
        raise HTTPException(status_code=404, detail="会话不存在")

    result = mongodb_client.get_messages_paginated(
        session_id=session_id,
        cursor=cursor,
        limit=limit,
    )

    items = [_to_message_item(d) for d in result.get("items", [])]
    resp = MessageListResponse(
        items=items,
        next_cursor=result.get("next_cursor"),
        has_more=result.get("has_more", False),
    )
    return ResultUtil.success(resp, message="获取消息列表成功")
