# smart-customer-service/api/prompt_routes.py
from fastapi import APIRouter, HTTPException, Query
from api.models import PromptCreate, PromptUpdate, PromptResponse, ResponseModel
from utils.mongodb_client import mongodb_client
from agent.prompts import (
    BASE_CATEGORY, GENERAL_CATEGORY, INTENT_CATEGORIES, invalidate_prompt_cache,
)
from typing import List
import datetime
from datetime import datetime

router = APIRouter(prefix="/api/prompts", tags=["prompts"])

# 允许建档/管理的 category 白名单
ALLOWED_CATEGORIES = {BASE_CATEGORY, GENERAL_CATEGORY} | INTENT_CATEGORIES

@router.get("", response_model=ResponseModel[List[PromptResponse]])
def get_prompts():
    """获取所有提示词"""
    try:
        prompts = list(mongodb_client.get_prompts_collection().find())
        prompt_responses = []
        for prompt in prompts:
            prompt_response = PromptResponse(
                prompt_id=prompt.get("prompt_id"),
                name=prompt.get("name"),
                category=prompt.get("category"),
                is_active=prompt.get("is_active"),
                version=prompt.get("version"),
                content=prompt.get("content"),
                created_at=prompt.get("created_at"),
                updated_at=prompt.get("updated_at")
            )
            prompt_responses.append(prompt_response)
        return ResponseModel(code=200, data=prompt_responses, message="获取提示词列表成功")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取提示词列表失败: {str(e)}")

@router.get("/active", response_model=ResponseModel[PromptResponse])
def get_active_prompt(category: str = Query(BASE_CATEGORY, description="提示词分类")):
    """获取指定 category 当前激活的提示词（默认 base）"""
    try:
        prompt = mongodb_client.get_active_prompt(category=category)
        if not prompt:
            raise HTTPException(status_code=404, detail=f"未找到 category={category} 的激活提示词")
        prompt_response = PromptResponse(
            prompt_id=prompt.get("prompt_id"),
            name=prompt.get("name"),
            category=prompt.get("category"),
            is_active=prompt.get("is_active"),
            version=prompt.get("version"),
            content=prompt.get("content"),
            created_at=prompt.get("created_at"),
            updated_at=prompt.get("updated_at")
        )
        return ResponseModel(code=200, data=prompt_response, message="获取激活提示词成功")
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取激活提示词失败: {str(e)}")

@router.get("/{prompt_id}", response_model=ResponseModel[PromptResponse])
def get_prompt(prompt_id: str):
    """根据ID获取提示词"""
    try:
        prompt = mongodb_client.get_prompt_by_id(prompt_id)
        if not prompt:
            raise HTTPException(status_code=404, detail="提示词不存在")
        prompt_response = PromptResponse(
            prompt_id=prompt.get("prompt_id"),
            name=prompt.get("name"),
            category=prompt.get("category"),
            is_active=prompt.get("is_active"),
            version=prompt.get("version"),
            content=prompt.get("content"),
            created_at=prompt.get("created_at"),
            updated_at=prompt.get("updated_at")
        )
        return ResponseModel(code=200, data=prompt_response, message="获取提示词成功")
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"获取提示词失败: {str(e)}")

@router.post("", response_model=ResponseModel[PromptResponse])
def create_prompt(body: PromptCreate):
    """创建新的提示词（按 category 建档；新建默认未激活）"""
    try:
        import uuid

        category = (body.category or BASE_CATEGORY).strip().lower()
        if category not in ALLOWED_CATEGORIES:
            raise HTTPException(
                status_code=400,
                detail=f"非法 category={category}，允许值：{sorted(ALLOWED_CATEGORIES)}"
            )

        # version 缺省时，按同 category 激活版本递增
        version = body.version
        if not version:
            active_prompt = mongodb_client.get_active_prompt(category=category)
            if active_prompt:
                current_version = active_prompt.get("version", "1.0")
                major, minor = map(int, current_version.split("."))
                version = f"{major}.{minor + 1}"
            else:
                version = "1.0"

        # 构造提示词数据（exclude_none 保持旧文档不出现多余空字段）
        prompt_data = {
            "prompt_id": str(uuid.uuid4()),
            "name": body.name,
            "category": category,
            "is_active": False,  # 默认禁用，需显式激活（激活时同 category 互斥）
            "version": version,
            "content": body.content.model_dump(exclude_none=True),
            "created_at": datetime.now(),
            "updated_at": datetime.now()
        }

        # 创建提示词
        result = mongodb_client.create_prompt(prompt_data)
        if not result:
            raise HTTPException(status_code=500, detail="创建提示词失败")
        
        # 获取创建的提示词
        prompt = mongodb_client.get_prompt_by_id(prompt_data["prompt_id"])
        prompt_response = PromptResponse(
            prompt_id=prompt.get("prompt_id"),
            name=prompt.get("name"),
            category=prompt.get("category"),
            is_active=prompt.get("is_active"),
            version=prompt.get("version"),
            content=prompt.get("content"),
            created_at=prompt.get("created_at"),
            updated_at=prompt.get("updated_at")
        )
        return ResponseModel(code=201, data=prompt_response, message="创建提示词成功")
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"创建提示词失败: {str(e)}")

@router.put("/{prompt_id}")
def update_prompt(prompt_id: str, prompt_update: PromptUpdate):
    """更新提示词"""
    try:
        # 检查提示词是否存在
        prompt = mongodb_client.get_prompt_by_id(prompt_id)
        if not prompt:
            raise HTTPException(status_code=404, detail="提示词不存在")
        
        # 过滤空数据
        update_data = {}
        if prompt_update.name is not None and prompt_update.name != "":
            update_data["name"] = prompt_update.name
        if prompt_update.is_active is not None and prompt_update.is_active != prompt.get("is_active", False):
            update_data["is_active"] = prompt_update.is_active
        
        # 处理 content 的部分更新
        if prompt_update.content is not None:
            content_update = prompt_update.content.model_dump(exclude_unset=True)
            # 过滤掉空字符串的字段，空字符串表示不修改
            content_update = {k: v for k, v in content_update.items() if v != ""}
            if content_update:
                # 获取现有 content
                current_content = prompt.get("content", {})
                # 合并更新（只更新传入的非空字段）
                updated_content = {**current_content, **content_update}
                update_data["content"] = updated_content
        
        # 如果更新了内容，增加版本号
        if "content" in update_data:
            current_version = prompt.get("version", "1.0")
            major, minor = map(int, current_version.split("."))
            new_version = f"{major}.{minor + 1}"
            update_data["version"] = new_version
        
        # 如果没有任何更新数据，返回错误
        if not update_data:
            raise HTTPException(status_code=400, detail="没有提供任何更新数据")
        
        # 更新提示词
        result = mongodb_client.update_prompt(prompt_id, update_data)
        if not result:
            raise HTTPException(status_code=500, detail="更新提示词失败")

        # 若改的是激活文档（内容或激活态），立即清空组装缓存，不等 TTL
        if prompt.get("is_active") or update_data.get("is_active"):
            invalidate_prompt_cache()

        return {"code": 200, "data": None, "message": "更新提示词成功"}
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"更新提示词失败: {str(e)}")

@router.put("/{prompt_id}/activate", response_model=ResponseModel[PromptResponse])
def activate_prompt(prompt_id: str):
    """激活提示词"""
    try:
        # 检查提示词是否存在
        prompt = mongodb_client.get_prompt_by_id(prompt_id)
        if not prompt:
            raise HTTPException(status_code=404, detail="提示词不存在")
        
        # 激活提示词（update_prompt 内部已做同 category 互斥停用）
        result = mongodb_client.update_prompt(prompt_id, {"is_active": True})
        if not result:
            raise HTTPException(status_code=500, detail="激活提示词失败")

        # 立即生效，不等 TTL
        invalidate_prompt_cache()

        # 获取激活后的提示词
        activated_prompt = mongodb_client.get_prompt_by_id(prompt_id)
        prompt_response = PromptResponse(
            prompt_id=activated_prompt.get("prompt_id"),
            name=activated_prompt.get("name"),
            category=activated_prompt.get("category"),
            is_active=activated_prompt.get("is_active"),
            version=activated_prompt.get("version"),
            content=activated_prompt.get("content"),
            created_at=activated_prompt.get("created_at"),
            updated_at=activated_prompt.get("updated_at")
        )
        return ResponseModel(code=200, data=prompt_response, message="激活提示词成功")
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"激活提示词失败: {str(e)}")

@router.put("/{prompt_id}/deactivate", response_model=ResponseModel[PromptResponse])
def deactivate_prompt(prompt_id: str):
    """停用提示词"""
    try:
        # 检查提示词是否存在
        prompt = mongodb_client.get_prompt_by_id(prompt_id)
        if not prompt:
            raise HTTPException(status_code=404, detail="提示词不存在")
        
        # 停用提示词
        result = mongodb_client.deactivate_prompt(prompt_id)
        if not result:
            raise HTTPException(status_code=500, detail="停用提示词失败")
        invalidate_prompt_cache()
        
        # 获取停用后的提示词
        deactivated_prompt = mongodb_client.get_prompt_by_id(prompt_id)
        prompt_response = PromptResponse(
            prompt_id=deactivated_prompt.get("prompt_id"),
            name=deactivated_prompt.get("name"),
            category=deactivated_prompt.get("category"),
            is_active=deactivated_prompt.get("is_active"),
            version=deactivated_prompt.get("version"),
            content=deactivated_prompt.get("content"),
            created_at=deactivated_prompt.get("created_at"),
            updated_at=deactivated_prompt.get("updated_at")
        )
        return ResponseModel(code=200, data=prompt_response, message="停用提示词成功")
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"停用提示词失败: {str(e)}")

@router.delete("/{prompt_id}", response_model=ResponseModel[dict])
def delete_prompt(prompt_id: str):
    """删除提示词"""
    try:
        # 检查提示词是否存在
        prompt = mongodb_client.get_prompt_by_id(prompt_id)
        if not prompt:
            raise HTTPException(status_code=404, detail="提示词不存在")
        
        # 删除提示词
        was_active = bool(prompt.get("is_active"))
        result = mongodb_client.get_prompts_collection().delete_one({"prompt_id": prompt_id})
        if result.deleted_count == 0:
            raise HTTPException(status_code=500, detail="删除提示词失败")

        # 删除的若是激活文档，清空组装缓存（否则最长 TTL 内仍组装已删除文档）
        if was_active:
            invalidate_prompt_cache()

        return ResponseModel(code=200, data={"prompt_id": prompt_id}, message="删除提示词成功")
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"删除提示词失败: {str(e)}")
