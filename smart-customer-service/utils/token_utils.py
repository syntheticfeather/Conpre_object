# smart-customer-service/utils/token_utils.py
import jwt
import os
import base64
from typing import Optional, Dict, Any
import logging
from dotenv import load_dotenv

load_dotenv()

logger = logging.getLogger(__name__)

# JWT 密钥配置（与 Java 后端保持一致）
JWT_SECRET = os.getenv("JWT_SECRET", "5H6fK9pL2mN8qR4sT7vW1xY3zA0bC2dE4fG6hJ8kL0nM2oP4qR6sT8uV0wX2yZ4")
JWT_ALGORITHM = "HS256"


def _resolve_secret_key(secret_key: str = None) -> bytes:
    """
    将 JWT 密钥解析为用于 HMAC 签名的原始字节。

    背景：Java 端使用 jjwt 0.11.x，其 signWith(alg, String) / setSigningKey(String)
    会把传入的 String 当作 Base64 编码，先解码再用作 HMAC 密钥字节。
    Python 端若直接用字符串的 UTF-8 字节，与 Java 端密钥不一致，导致 "Signature verification failed"。

    本函数对齐 Java 端行为：先尝试 Base64 解码得到密钥字节；
    若字符串不是合法 Base64，则退回使用 UTF-8 字节（保证向后兼容）。
    """
    key_str = secret_key if secret_key else JWT_SECRET
    try:
        # jjwt 的 Decoders.BASE64.decode 对 padding 容错，这里补齐 padding 以对齐行为
        padded = key_str + "=" * (-len(key_str) % 4)
        return base64.b64decode(padded)
    except Exception:
        logger.warning("JWT_SECRET 非 Base64 编码，退回使用 UTF-8 字节作为密钥")
        return key_str.encode("utf-8")


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
        # 对齐 Java jjwt 0.11.x 行为：密钥先 Base64 解码再用于 HMAC
        key = _resolve_secret_key(secret_key)

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
        # 对齐 Java jjwt 0.11.x 行为：密钥先 Base64 解码再用于 HMAC
        key = _resolve_secret_key(secret_key)
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
