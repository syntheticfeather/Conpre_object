# smart-customer-service/api/prompt_routes.py
from fastapi import APIRouter, HTTPException
from api.models import PromptContent, PromptUpdate, PromptResponse, ResponseModel
from utils.mongodb_client import mongodb_client
from typing import List
import datetime

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
def create_prompt(name: str, content: PromptContent,version: str = "1.0"):
    """创建新的提示词"""
    try:
        import uuid
        
        # 构造提示词数据
        prompt_data = {
            "prompt_id": str(uuid.uuid4()),
            "name": name,
            "category": "loan_customer_service",
            "is_active": False,
            "version": version,
            "content": content.model_dump(),
            "config": {
                "protected_tools": [
                    {"name": "query_application_status", "description": "查询贷款申请状态"},
                    {"name": "calculate_repayment", "description": "计算贷款还款计划"},
                    {"name": "search_knowledge", "description": "在知识库中搜索相关信息"},
                    {"name": "search_web", "description": "搜索网络获取实时信息"}
                ],
                "variables": ["current_date"]
            },
            "created_at": datetime.datetime.now(),
            "updated_at": datetime.datetime.now()
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

@router.put("/{prompt_id}", response_model=ResponseModel[PromptResponse])
def update_prompt(prompt_id: str, prompt_update: PromptUpdate):
    """更新提示词"""
    try:
        # 检查提示词是否存在
        prompt = mongodb_client.get_prompt_by_id(prompt_id)
        if not prompt:
            raise HTTPException(status_code=404, detail="提示词不存在")
        
        # 构造更新数据
        update_data = {}
        if prompt_update.name is not None:
            update_data["name"] = prompt_update.name
        if prompt_update.content is not None:
            update_data["content"] = prompt_update.content.model_dump()
        if prompt_update.is_active is not None:
            update_data["is_active"] = prompt_update.is_active
        
        # 如果更新了内容，增加版本号
        if "content" in update_data:
            current_version = prompt.get("version", "1.0")
            major, minor = map(int, current_version.split("."))
            new_version = f"{major}.{minor + 1}"
            update_data["version"] = new_version
        
        # 更新提示词
        result = mongodb_client.update_prompt(prompt_id, update_data)
        if not result:
            raise HTTPException(status_code=500, detail="更新提示词失败")
        
        # 获取更新后的提示词
        updated_prompt = mongodb_client.get_prompt_by_id(prompt_id)
        prompt_response = PromptResponse(
            prompt_id=updated_prompt.get("prompt_id"),
            name=updated_prompt.get("name"),
            category=updated_prompt.get("category"),
            is_active=updated_prompt.get("is_active"),
            version=updated_prompt.get("version"),
            content=updated_prompt.get("content"),
            created_at=updated_prompt.get("created_at"),
            updated_at=updated_prompt.get("updated_at")
        )
        return ResponseModel(code=200, data=prompt_response, message="更新提示词成功")
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
