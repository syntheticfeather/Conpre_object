# smart-customer-service/api/prompt_routes.py
from fastapi import APIRouter, HTTPException
from api.models import PromptContent, PromptUpdate, PromptResponse, ResponseModel
from utils.mongodb_client import mongodb_client
from typing import List
import datetime
from datetime import datetime

router = APIRouter(prefix="/api/prompts", tags=["prompts"])

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
def get_active_prompt():
    """获取当前激活的提示词"""
    try:
        prompt = mongodb_client.get_active_prompt()
        if not prompt:
            raise HTTPException(status_code=404, detail="未找到激活的提示词")
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
def create_prompt(name: str, content: PromptContent, version: str = None):
    """创建新的提示词"""
    try:
        import uuid
        
        # 如果 version 为 None 或空字符串，查找现有激活的提示词版本号并加1
        if not version:
            # 获取激活的提示词
            active_prompt = mongodb_client.get_active_prompt()
            if active_prompt:
                current_version = active_prompt.get("version", "1.0")
                major, minor = map(int, current_version.split("."))
                version = f"{major}.{minor + 1}"
            else:
                version = "1.0"
        
        # 构造提示词数据
        prompt_data = {
            "prompt_id": str(uuid.uuid4()),
            "name": name,
            "category": "customer_service", # 当前默认
            "is_active": False,  # 默认禁用
            "version": version,
            "content": content.model_dump(),
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
        
        # 激活提示词
        result = mongodb_client.update_prompt(prompt_id, {"is_active": True})
        if not result:
            raise HTTPException(status_code=500, detail="激活提示词失败")
        
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
        result = mongodb_client.get_prompts_collection().delete_one({"prompt_id": prompt_id})
        if result.deleted_count == 0:
            raise HTTPException(status_code=500, detail="删除提示词失败")
        
        return ResponseModel(code=200, data={"prompt_id": prompt_id}, message="删除提示词成功")
    except HTTPException:
        raise
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"删除提示词失败: {str(e)}")
