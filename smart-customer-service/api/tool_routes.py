# smart-customer-service/api/tool_routes.py
import sys
import os

# 添加项目根目录到 Python 搜索路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from fastapi import APIRouter, HTTPException
from api.utils import ResultUtil
from tools.tool_manager import tool_manager
from api.models import MCPServerCreate, MCPServerResponse
from utils.mongodb_client import mongodb_client

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

# ------------------------------
# MCP服务器管理接口
# ------------------------------

# 添加MCP服务器
@router.post("/mcp/server")
async def add_mcp_server(server: MCPServerCreate):
    """ 添加MCP服务器配置 """
    # 1. 基础校验（只校验必须的）
    if not server.server_id:
        raise HTTPException(status_code=400, detail="server_id不能为空")
    
    # 2. 直接使用前端传来的 config
    # 注意：这里不再组装 config，直接使用用户传入的
    raw_config = server.config 
    
    # 3. 将 transport 也放入 config 中（或者更好的方式是让 Manager 自动推断）
    # 为了兼容 Manager 的旧逻辑，我们暂时把 transport 信息也放进 config
    # 但实际上，Manager 应该根据 raw_config 里的 key 来判断
    
    # 策略：如果用户传了 command，就是 stdio；如果传了 url，就是 sse
    # 我们不需要在 API 层显式存储 transport
    final_config = raw_config.copy()
    
    # 4. 检查是否已存在（保持不变）
    existing_server = mongodb_client.get_mcp_server(server.server_id)
    if existing_server:
        raise HTTPException(status_code=409, detail=f"MCP服务器 {server.server_id} 已存在")
    
    # 5. 调用 Manager
    success = await tool_manager.add_mcp_server(server.server_id, final_config)
    if not success:
        raise HTTPException(status_code=500, detail="添加MCP服务器失败")
    
    return ResultUtil.success(
        data={"server_id": server.server_id}, 
        message="MCP服务器添加成功" 
    )

# 获取所有MCP服务器列表
@router.get("/mcp/servers")
def list_mcp_servers():
    """ 获取所有已配置的MCP服务器 """
    servers = mongodb_client.get_all_mcp_servers()
    result = []
    
    for server in servers:
        # 直接构建通用响应模型
        response = MCPServerResponse(
            server_id=server.get("server_id"),
            config=server.get("config", {}), # 直接返回原始 config
            created_at=server.get("created_at"),
            updated_at=server.get("updated_at")
        )
        result.append(response)
    
    return ResultUtil.success(data=result, message="获取MCP服务器列表成功")

# 获取单个MCP服务器配置
@router.get("/mcp/server/{server_id}")
def get_mcp_server(server_id: str):
    """ 获取指定MCP服务器配置 """
    server = mongodb_client.get_mcp_server(server_id)
    if not server:
        raise HTTPException(status_code=404, detail=f"MCP服务器 {server_id} 不存在")
    
    response = MCPServerResponse(
        server_id=server.get("server_id"),
        config=server.get("config", {}),
        created_at=server.get("created_at"),
        updated_at=server.get("updated_at")
    )
    
    return ResultUtil.success(data=response, message="获取MCP服务器配置成功")

# 删除MCP服务器
@router.delete("/mcp/server/{server_id}")
async def remove_mcp_server(server_id: str):
    """ 删除MCP服务器配置 """
    server = mongodb_client.get_mcp_server(server_id)
    if not server:
        raise HTTPException(status_code=404, detail=f"MCP服务器 {server_id} 不存在")
    
    success = await tool_manager.remove_mcp_server(server_id)
    if not success:
        raise HTTPException(status_code=500, detail="删除MCP服务器失败")
    
    return ResultUtil.success(message=f"MCP服务器 {server_id} 删除成功")

# 刷新动态工具列表
@router.post("/mcp/refresh")
async def refresh_mcp_tools():
    """ 刷新从MCP服务器获取的动态工具 """
    await tool_manager.refresh_mcp_tools()
    tools = tool_manager.get_all_registered_tools()
    mcp_tools = [t for t in tools if t["source"] == "mcp"]
    
    return ResultUtil.success(
        data={"count": len(mcp_tools), "tools": mcp_tools},
        message=f"刷新成功，当前有 {len(mcp_tools)} 个动态工具"
    )