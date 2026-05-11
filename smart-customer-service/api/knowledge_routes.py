# smart-customer-service/api/knowledge_routes.py
import sys
import os

# 添加项目根目录到 Python 搜索路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from fastapi import APIRouter, HTTPException, File, UploadFile, BackgroundTasks
from typing import List
from datetime import datetime
from api.models import KnowledgeItemCreate, KnowledgeItemResponse, KnowledgeItemUpdate
from knowledge.vector_store import VectorStore
from knowledge.models import KnowledgeItem
from api.utils import ResultUtil
from api.init_data import knowledge_initializer
from utils.file_upload_utils import (
    validate_file, 
    save_uploaded_file, 
    process_uploaded_file
)

router = APIRouter(prefix="/api/knowledge", tags=["knowledge"])
vector_store = VectorStore()

# 获取所有问答对知识项
@router.get("/faq")
def list_faq_knowledge():
    """ 获取所有问答对知识项 """
    return ResultUtil.success(data=vector_store.get_all_faq(), message="获取成功")

# 获取所有文档分块知识项
@router.get("/documents")
def list_document_knowledge():
    """ 获取所有文档分块知识项 """
    return ResultUtil.success(data=vector_store.get_all_documents(), message="获取成功")

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
    # 2. 存入向量库（检查返回值）
    success = vector_store.add_item(kb_item)
    if not success:
        raise HTTPException(status_code=500, detail="添加知识库项失败")
    # 3. 将业务模型转换为 API 模型
    # 这里利用了 Pydantic 的特性：传入一个对象，它会自动提取同名字段
    response_model = KnowledgeItemResponse.model_validate(kb_item)
    # 4. 直接返回模型，不要调用 to_dict()
    # FastAPI 会自动将其转换为 JSON，并应用 api/models.py 中的格式化规则
    return ResultUtil.success(data=response_model, message="添加成功")

# 按文档名称删除整篇文档（必须放在 /{item_id} 路由之前）
@router.delete("/document")
def delete_document_by_name(document_name: str):
    """按文档名称删除整篇文档的所有分块（管理员操作）
    
    Args:
        document_name: 文档名称（不含扩展名）
    
    Returns:
        删除结果统计
    """
    deleted_count = vector_store.delete_by_metadata({"document_name": document_name})
    
    if deleted_count > 0:
        return ResultUtil.success(data={"deleted_count": deleted_count}, message=f"成功删除文档 '{document_name}' 的 {deleted_count} 条数据")
    else:
        raise HTTPException(status_code=404, detail=f"未找到文档 '{document_name}'")

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
        category=item_update.category or existing_item.get("category", "通用"),
        updated_at=datetime.now()
    )
    
    # 存入向量库
    vector_store.update(updated_item)
    
    # 转换为响应模型
    response_model = KnowledgeItemResponse.model_validate(updated_item)
    return ResultUtil.success(data=response_model, message="更新成功")

# ============ 文件上传接口 ============

# 上传文件保存目录
UPLOAD_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'knowledge-upload')

@router.post("/upload")
def upload_knowledge_file(
    background_tasks: BackgroundTasks,
    file: UploadFile = File(...)
):
    """
    上传知识库文件（管理员操作）
    
    支持的文件格式：
    - .json: 问答对格式或文档格式
    - .md: Markdown文档格式
    
    文件会先保存到服务器，然后异步加载到知识库
    
    Args:
        file: 要上传的文件
        
    Returns:
        上传结果
    """
    # 1. 读取文件内容
    content = file.file.read()
    file_size = len(content)
    
    # 2. 校验文件
    valid, error_msg = validate_file(content, file.filename)
    if not valid:
        raise HTTPException(status_code=400, detail=error_msg)
    
    # 3. 保存文件
    success, saved_path, error_msg = save_uploaded_file(content, file.filename, UPLOAD_DIR)
    if not success:
        raise HTTPException(status_code=500, detail=error_msg)
    
    # 4. 异步处理文件（加载到知识库）
    background_tasks.add_task(process_uploaded_file, saved_path, knowledge_initializer)
    
    # 提取保存的文件名
    saved_filename = os.path.basename(saved_path)
    
    return ResultUtil.success(
        data={
            "filename": file.filename,
            "saved_filename": saved_filename,
            "saved_path": saved_path,
            "file_size": file_size
        },
        message="文件上传成功，正在后台处理中"
    )

