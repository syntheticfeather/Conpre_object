# smart-customer-service/api/session_routes.py
"""
会话管理路由 — 配套 utils/mongodb_client.py 中 chat_sessions 集合。

为移动端提供会话列表能力：
  - 新建会话        POST   /api/sessions
  - 会话列表（游标） GET    /api/sessions?cursor=&limit=20
  - 单个会话        GET    /api/sessions/{session_id}
  - 更新会话        PATCH  /api/sessions/{session_id}   （title / pinned / archived）
  - 置顶切换        POST   /api/sessions/{session_id}/pin
  - 删除会话        DELETE /api/sessions/{session_id}  （同时删该会话下全部消息）
"""
import sys
import os

sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from fastapi import APIRouter, HTTPException, Depends, Query
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from typing import Optional

from api.models import (
    ResponseModel,
    SessionUpdate,
    SessionPinRequest,
    SessionItem,
    SessionListResponse,
)
from api.utils import ResultUtil
from utils.mongodb_client import mongodb_client
from utils.token_utils import extract_user_id_from_token

router = APIRouter(prefix="/api/sessions", tags=["sessions"])
security = HTTPBearer()


def _current_user_id(credentials: HTTPAuthorizationCredentials) -> str:
    """从 Bearer Token 中解析 user_id，鉴权失败直接 401。"""
    user_id = extract_user_id_from_token(credentials.credentials)
    if not user_id:
        raise HTTPException(status_code=401, detail="无效的 token，无法识别用户")
    return user_id


def _get_owned_session(session_id: str, user_id: str) -> dict:
    """取会话并校验归属：不存在或不属于当前用户一律 404（不暴露会话是否存在）。"""
    doc = mongodb_client.get_session(session_id)
    if not doc or doc.get("user_id") != user_id:
        raise HTTPException(status_code=404, detail="会话不存在")
    return doc


def _to_session_item(doc: dict) -> SessionItem:
    """MongoDB 文档 → SessionItem 响应模型"""
    return SessionItem(
        session_id=doc.get("session_id", ""),
        user_id=doc.get("user_id"),
        title=doc.get("title", ""),
        last_message_preview=doc.get("last_message_preview", ""),
        last_message_at=doc.get("last_message_at"),
        message_count=doc.get("message_count", 0),
        created_at=doc.get("created_at"),
        updated_at=doc.get("updated_at"),
        pinned=doc.get("pinned", False),
        archived=doc.get("archived", False),
    )


# ==================== 路由 ====================

@router.post("", response_model=ResponseModel[SessionItem])
def create_session(
    credentials: HTTPAuthorizationCredentials = Depends(security),
):
    """
    新建会话，返回 session_id 供后续 /api/chat 使用。无请求体、无参数。

    - 标题由后端生成占位标题"新对话 yyyy-MM-dd HH:mm"，首条消息后 LLM 异步摘要回填；
      用户手动改名走 PATCH /api/sessions/{id}。
    - 固定复用策略：该用户已有 message_count=0 的空会话则直接复用（无时间限制），
      避免狂点"+"堆叠空会话；用户发首条消息后 message_count>0，下次点"+"正常新建。
    """
    user_id = _current_user_id(credentials)
    doc = mongodb_client.create_session(
        user_id=user_id,
        reuse_window_minutes=5,
    )
    if not doc:
        raise HTTPException(status_code=500, detail="创建会话失败")
    return ResultUtil.success(_to_session_item(doc), message="创建会话成功")


@router.get("", response_model=ResponseModel[SessionListResponse])
def list_sessions(
    cursor: Optional[str] = Query(None, description="上一页返回的 next_cursor"),
    limit: int = Query(20, ge=1, le=100, description="每页数量"),
    include_archived: bool = Query(False, description="是否包含已归档会话"),
    has_messages: bool = Query(False, description="仅返回有消息的会话（进入上次对话场景）"),
    credentials: HTTPAuthorizationCredentials = Depends(security),
):
    """
    列出当前用户的会话，按 pinned desc + updated_at desc 排序。
    游标分页：首次不传 cursor 取最新一页；后续把上页 next_cursor 透传回来。
    has_messages=true 时只返回 message_count > 0 的会话，用于客户端进入时定位上次对话。
    """
    user_id = _current_user_id(credentials)
    result = mongodb_client.list_sessions(
        user_id=user_id,
        cursor=cursor,
        limit=limit,
        include_archived=include_archived,
        has_messages=has_messages,
    )
    items = [_to_session_item(d) for d in result.get("items", [])]
    resp = SessionListResponse(
        items=items,
        next_cursor=result.get("next_cursor"),
        has_more=result.get("has_more", False),
    )
    return ResultUtil.success(resp, message="获取会话列表成功")


@router.get("/{session_id}", response_model=ResponseModel[SessionItem])
def get_session(session_id: str,
                credentials: HTTPAuthorizationCredentials = Depends(security)):
    """
    获取单个会话详情（元信息）。

    侧栏点击进入会话时无需调用（列表已含标题）；用于刷新页面 / URL 深链进入时
    仅凭 session_id 获取标题等元信息，消息内容走 GET /{session_id}/messages。
    """
    user_id = _current_user_id(credentials)
    doc = _get_owned_session(session_id, user_id)
    return ResultUtil.success(_to_session_item(doc), message="获取会话成功")


@router.patch("/{session_id}", response_model=ResponseModel[SessionItem])
def update_session(
    session_id: str,
    body: SessionUpdate,
    credentials: HTTPAuthorizationCredentials = Depends(security),
):
    """更新会话字段（title / pinned / archived 任意子集）。"""
    user_id = _current_user_id(credentials)
    _get_owned_session(session_id, user_id)

    update_fields = {}
    if body.title is not None:
        update_fields["title"] = body.title
    if body.pinned is not None:
        update_fields["pinned"] = body.pinned
    if body.archived is not None:
        update_fields["archived"] = body.archived

    if not update_fields:
        raise HTTPException(status_code=400, detail="未提供任何更新字段")

    if not mongodb_client.update_session_meta(session_id, **update_fields):
        raise HTTPException(status_code=404, detail="会话不存在或更新失败")

    doc = mongodb_client.get_session(session_id)
    if not doc:
        raise HTTPException(status_code=404, detail="会话不存在")
    return ResultUtil.success(_to_session_item(doc), message="更新会话成功")


@router.post("/{session_id}/pin", response_model=ResponseModel[SessionItem])
def toggle_pin(
    session_id: str,
    body: SessionPinRequest,
    credentials: HTTPAuthorizationCredentials = Depends(security),
):
    """置顶 / 取消置顶会话。"""
    user_id = _current_user_id(credentials)
    _get_owned_session(session_id, user_id)
    if not mongodb_client.pin_session(session_id, body.pinned):
        raise HTTPException(status_code=404, detail="会话不存在或更新失败")
    doc = mongodb_client.get_session(session_id)
    if not doc:
        raise HTTPException(status_code=404, detail="会话不存在")
    return ResultUtil.success(_to_session_item(doc),
                              message="置顶成功" if body.pinned else "取消置顶成功")


@router.delete("/{session_id}", response_model=ResponseModel[dict])
def delete_session(session_id: str,
                   credentials: HTTPAuthorizationCredentials = Depends(security)):
    """删除会话：同时删除该会话下的全部消息（事务）。"""
    user_id = _current_user_id(credentials)
    _get_owned_session(session_id, user_id)
    if not mongodb_client.delete_session(session_id):
        raise HTTPException(status_code=500, detail="删除会话失败")
    return ResultUtil.success(data={"session_id": session_id}, message="删除会话成功")
