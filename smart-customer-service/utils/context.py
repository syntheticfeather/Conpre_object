# smart-customer-service/utils/context.py
import contextvars

# 创建一个 context variable 来存储 token
token_var = contextvars.ContextVar('token', default=None)

def set_token(token: str) -> None:
    """设置当前上下文的 token"""
    token_var.set(token)

def get_token() -> str:
    """获取当前上下文的 token"""
    return token_var.get()
