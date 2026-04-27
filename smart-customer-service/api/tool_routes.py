# smart-customer-service/api/tool_routes.py
import sys
import os

# 添加项目根目录到 Python 搜索路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from fastapi import APIRouter, HTTPException
from api.utils import ResultUtil
from tools.tool_manager import tool_manager

router = APIRouter(prefix="/api/tools", tags=["tools"])

# 获取所有工具列表
@router.get("/")
def list_tools():
    """ 获取所有工具列表 """
    tools = tool_manager.get_all_registered_tools()
    return ResultUtil.success(data=tools, message="获取工具列表成功")

# 按名称搜索工具
@router.get("/search")
def search_tool(name: str):
    """ 按名称搜索工具 """
    tool = tool_manager.get_tool(name)
    if not tool:
        return ResultUtil.error(code=404, message="工具不存在")
    tool_info = {
        "name": tool.name,
        "description": tool.description,
        "enabled": tool_manager.enabled_tools.get(name, True)
    }
    return ResultUtil.success(data=tool_info, message="获取工具成功")

# 设置工具状态
@router.put("/{tool_name}")
def set_tool_status(tool_name: str, enabled: bool):
    """ 设置工具状态（启用/禁用） """
    success = tool_manager.set_tool_status(tool_name, enabled)
    if not success:
        return ResultUtil.error(code=404, message="工具不存在")
    return ResultUtil.success(message=f"工具 {tool_name} {'启用' if enabled else '禁用'} 成功")
