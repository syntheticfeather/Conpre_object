# smart-customer-service/utils/token_utils.py
import jwt
import os
from typing import Optional, Dict, Any
import logging

logger = logging.getLogger(__name__)

# JWT 密钥配置（与 Java 后端保持一致）
JWT_SECRET = os.getenv("JWT_SECRET", "5H6fK9pL2mN8qR4sT7vW1xY3zA0bC2dE4fG6hJ8kL0nM2oP4qR6sT8uV0wX2yZ4")
JWT_ALGORITHM = "HS256"


def extract_user_id_from_token(token: str, secret_key: str = None) -> Optional[str]:
    """
    从 JWT token 中提取 user_id

    Args:
        token: JWT token 字符串
        secret_key: JWT 密钥，如果为 None 则使用默认密钥（与 Java 后端保持一致）

    Returns:
        user_id 如果提取成功，否则返回 None
    """
    try:
        # 使用提供的密钥或默认密钥
        key = secret_key if secret_key else JWT_SECRET

        # 解码 token（验证签名）
        payload = jwt.decode(token, key, algorithms=[JWT_ALGORITHM])

        # 尝试从不同的字段中获取 user_id
        # Java 端将 userId 存储在 "userId" claim 中
        user_id = (
            payload.get("userId") or
            payload.get("user_id") or
            payload.get("sub") or
            payload.get("id")
        )

        if user_id:
            logger.info(f"成功从 token 中提取 user_id: {user_id}")
            return str(user_id)
        else:
            logger.warning(f"Token 中未找到 user_id，payload: {payload}")
            return None

    except jwt.ExpiredSignatureError:
        logger.error("Token 已过期")
        return None
    except jwt.InvalidTokenError as e:
        logger.error(f"Token 无效: {str(e)}")
        return None
    except Exception as e:
        logger.error(f"提取 user_id 时发生错误: {str(e)}")
        return None


def get_session_id(user_id: Optional[str], provided_session_id: Optional[str] = None) -> str:
    """
    获取或生成 session_id

    优先级：
    1. 客户端提供的 session_id（如果不为空）
    2. 从 user_id 生成的 session_id（如果 user_id 存在）
    3. 匿名用户的临时 session_id

    Args:
        user_id: 从 token 中提取的用户 ID
        provided_session_id: 客户端提供的 session_id

    Returns:
        session_id 字符串
    """
    if provided_session_id and provided_session_id.strip():
        return provided_session_id

    if user_id:
        return f"user:{user_id}"

    import uuid
    return f"anon:{uuid.uuid4()}"


def extract_user_phone_from_token(token: str, secret_key: str = None) -> Optional[str]:
    """
    从 JWT token 中提取 user_phone（存储在 sub 字段中）

    Args:
        token: JWT token 字符串
        secret_key: JWT 密钥，如果为 None 则使用默认密钥

    Returns:
        user_phone 如果提取成功，否则返回 None
    """
    try:
        key = secret_key if secret_key else JWT_SECRET
        payload = jwt.decode(token, key, algorithms=[JWT_ALGORITHM])

        # sub 字段存储的是 userPhone
        user_phone = payload.get("sub")

        if user_phone:
            logger.info(f"成功从 token 中提取 user_phone: {user_phone}")
            return str(user_phone)
        else:
            logger.warning(f"Token 中未找到 user_phone")
            return None

    except jwt.ExpiredSignatureError:
        logger.error("Token 已过期")
        return None
    except jwt.InvalidTokenError as e:
        logger.error(f"Token 无效: {str(e)}")
        return None
    except Exception as e:
        logger.error(f"提取 user_phone 时发生错误: {str(e)}")
        return None
