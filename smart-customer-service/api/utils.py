# api/utils.py
from typing import Optional, Generic, TypeVar
from .models import ResponseModel

T = TypeVar('T')

class ResultUtil:
    @staticmethod
    def success(data: Optional[T] = None, message: str = "操作成功") -> ResponseModel[T]:
        return ResponseModel[T](code=200, message=message, data=data)

    @staticmethod
    def error(code: int = 500, message: str = "操作失败") -> ResponseModel:
        return ResponseModel(code=code, message=message, data=None)