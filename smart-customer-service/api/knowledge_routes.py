# smart-customer-service/api/knowledge_routes.py
import sys
import os

# 添加项目根目录到 Python 搜索路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from fastapi import APIRouter, HTTPException
from typing import List
from api.models import KnowledgeItemCreate, KnowledgeItemResponse, KnowledgeItemUpdate
from knowledge.vector_store import VectorStore
from knowledge.models import KnowledgeItem
from api.utils import ResultUtil


router = APIRouter(prefix="/api/knowledge", tags=["knowledge"])
vector_store = VectorStore()

# 获取所有知识库项
@router.get("/")
def list_knowledge():
    """ 获取所有知识库项 """
    return ResultUtil.success(data=vector_store.get_all(), message="获取成功")

# 创建知识库项
@router.post("/")
def create_knowledge(item: KnowledgeItemCreate):
    """ 创建知识库项 """
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
    """ 删除知识库项 """
    vector_store.delete(item_id)
    return ResultUtil.success(message="删除成功")

# 修改知识库项
@router.put("/{item_id}")
def update_knowledge(item_id: str, item_update: KnowledgeItemUpdate):
    """ 修改知识库项 """
    # 获取所有知识项
    all_items = vector_store.get_all()
    # 查找要更新的项
    existing_item = None
    for item in all_items:
        if item["id"] == item_id:
            existing_item = item
            break
    
    if not existing_item:
        raise HTTPException(status_code=404, detail="知识库项不存在")
    
    # 更新字段
    updated_item = KnowledgeItem(
        id=item_id,
        question=item_update.question or existing_item["question"],
        answer=item_update.answer or existing_item["answer"],
        category=item_update.category or existing_item.get("category", "通用")
    )
    
    # 存入向量库
    vector_store.update(updated_item)
    
    # 转换为响应模型
    response_model = KnowledgeItemResponse.model_validate(updated_item)
    return ResultUtil.success(data=response_model, message="更新成功")

