# smart-customer-service/api/knowledge_routes.py
import sys
import os

# 添加项目根目录到 Python 搜索路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from fastapi import APIRouter, HTTPException
from typing import List
from api.models import KnowledgeItemCreate, KnowledgeItemResponse
from knowledge.vector_store import VectorStore
from knowledge.models import KnowledgeItem
from api.utils import ResultUtil

router = APIRouter(prefix="/knowledge", tags=["knowledge"])
vector_store = VectorStore()

# 获取所有知识库项
@router.get("/")
def list_knowledge():
    return ResultUtil.success(data=vector_store.get_all(), message="获取成功")

# 创建知识库项
@router.post("/")
def create_knowledge(item: KnowledgeItemCreate):
    # 1. 创建业务模型
    kb_item = KnowledgeItem(
        question=item.question,
        answer=item.answer,
        category=item.category
    )
    # 2. 存入向量库
    vector_store.add_item(kb_item)
    # 3. 将业务模型转换为 API 模型
    # 这里利用了 Pydantic 的特性：传入一个对象，它会自动提取同名字段
    response_model = KnowledgeItemResponse.model_validate(kb_item)
    # 4. 直接返回模型，不要调用 to_dict()
    # FastAPI 会自动将其转换为 JSON，并应用 api/models.py 中的格式化规则
    return ResultUtil.success(data=response_model, message="添加成功")

# 删除知识库项
@router.delete("/{item_id}")
def delete_knowledge(item_id: str):
    vector_store.delete(item_id)
    return ResultUtil.success(message="删除成功")