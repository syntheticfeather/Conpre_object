# smart-customer-service/api/tool_routes.py
import sys
import os

# 添加项目根目录到 Python 搜索路径
sys.path.append(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from fastapi import APIRouter, HTTPException
from api.utils import ResultUtil
from tools.tool_manager import tool_manager
from api.models import MCPServerCreate, RemoteMCPServerResponse, LocalMCPServerResponse
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
    # 验证参数
    if not server.server_id:
        raise HTTPException(status_code=400, detail="server_id不能为空")

    # 根据传输方式验证不同参数
    if server.transport == "stdio":
        if not server.command:
            raise HTTPException(status_code=400, detail="stdio传输方式需要command参数")
    else:  # sse 或 websocket
        if not server.url:
            raise HTTPException(status_code=400, detail="需要url参数")
        if not server.api_key:
            raise HTTPException(status_code=400, detail="需要api_key参数")
    
    # 检查是否已存在
    existing_server = mongodb_client.get_mcp_server(server.server_id)
    if existing_server:
        raise HTTPException(status_code=409, detail=f"MCP服务器 {server.server_id} 已存在")
    
    # 构建配置（只包含非None的字段）
    config = {"transport": server.transport}
    
    if server.transport == "stdio":
        config["command"] = server.command
        if server.args:
            config["args"] = server.args
    else:
        config["url"] = server.url
        config["api_key"] = server.api_key
        if server.timeout:  #  只对 SSE 添加 timeout
            config["timeout"] = server.timeout
    
    success = await tool_manager.add_mcp_server(server.server_id, config)
    if not success:
        raise HTTPException(status_code=500, detail="添加MCP服务器失败")
    
    return ResultUtil.success(
        data={"server_id": server.server_id, "transport": server.transport},
        message="MCP服务器添加成功"
    )

# 获取所有MCP服务器列表
@router.get("/mcp/servers")
def list_mcp_servers():
    """ 获取所有已配置的MCP服务器 """
    servers = mongodb_client.get_all_mcp_servers()
    
    # 根据 transport 类型返回不同的响应结构
    result = []
    for server in servers:
        config = server.get("config", {})
        transport = config.get("transport", "sse")
        
        if transport == "stdio":
            # 本地服务器
            response = LocalMCPServerResponse(
                server_id=server.get("server_id"),
                transport=transport,
                command=config.get("command"),
                args=config.get("args"),
                created_at=server.get("created_at"),
                updated_at=server.get("updated_at")
            )
        else:
            # 远程服务器 (sse 或 websocket)
            response = RemoteMCPServerResponse(
                server_id=server.get("server_id"),
                transport=transport,
                url=config.get("url"),
                timeout=config.get("timeout", 30),
                created_at=server.get("created_at"),
                updated_at=server.get("updated_at")
            )
        result.append(response.dict())
    
    return ResultUtil.success(data=result, message="获取MCP服务器列表成功")

# 获取单个MCP服务器配置
@router.get("/mcp/server/{server_id}")
def get_mcp_server(server_id: str):
    """ 获取指定MCP服务器配置 """
    server = mongodb_client.get_mcp_server(server_id)
    if not server:
        raise HTTPException(status_code=404, detail=f"MCP服务器 {server_id} 不存在")
    
    config = server.get("config", {})
    transport = config.get("transport", "sse")
    
    if transport == "stdio":
        # 本地服务器
        response = LocalMCPServerResponse(
            server_id=server.get("server_id"),
            transport=transport,
            command=config.get("command"),
            args=config.get("args"),
            created_at=server.get("created_at"),
            updated_at=server.get("updated_at")
        )
    else:
        # 远程服务器 (sse 或 websocket)
        response = RemoteMCPServerResponse(
            server_id=server.get("server_id"),
            transport=transport,
            url=config.get("url"),
            timeout=config.get("timeout", 30),
            created_at=server.get("created_at"),
            updated_at=server.get("updated_at")
        )
    
    return ResultUtil.success(data=response.dict(), message="获取MCP服务器配置成功")

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