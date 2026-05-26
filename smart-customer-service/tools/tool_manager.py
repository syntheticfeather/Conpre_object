from typing import List, Dict, Any, Optional
from langchain_core.tools import BaseTool
from langchain_mcp_adapters.client import MultiServerMCPClient
from utils.mongodb_client import mongodb_client
from utils.crypto_utils import crypto_utils
import logging
import inspect

logger = logging.getLogger(__name__)

# # 查看类的所有方法
# print(dir(MultiServerMCPClient))

# # 查看方法签名
# print(inspect.signature(MultiServerMCPClient.get_tools))

class ToolManager:
    """工具管理器，用于注册和管理所有可用工具（静态工具 + 动态MCP工具）"""
    
    def __init__(self):
        self.tools: Dict[str, BaseTool] = {}  # 静态工具
        self.enabled_tools: Dict[str, bool] = {}  # 跟踪工具的启用状态
        
        self.mcp_tools: List[BaseTool] = []  # 从MCP加载的动态工具
        self.mcp_server_configs: Dict[str, dict] = {}  # MCP服务器配置 id,config
        self.mcp_client: Optional[MultiServerMCPClient] = None  # MCP客户端  # MCP多服务器客户端
        
    def _init_mcp_client(self):
        """初始化MCP客户端"""
        if self.mcp_server_configs:
            mcp_config = {}
            for server_id, config in self.mcp_server_configs.items():
                # 1. 自动推断 Transport 类型：如果包含 command，视为 Stdio；否则视为 SSE
                if "command" in config:
                    # Stdio 模式：直接透传 command, args, env
                    # 这样无论用户配置里有没有 args 或 env，都能自动支持
                    server_cfg = {
                        "transport": "stdio",
                        "command": config["command"],
                        # get 方法提供了很好的默认值
                        "args": config.get("args", []), 
                        "env": config.get("env", {}) 
                    }
                else:
                    # SSE 模式：处理 HTTP 相关配置
                    server_cfg = {
                        "transport": "sse",
                        "url": config["url"],
                        "headers": {
                            "Authorization": f"Bearer {config['api_key']}"
                        }
                    }
                    # 可选参数
                    if config.get("timeout"):
                        server_cfg["timeout"] = config["timeout"]

                mcp_config[server_id] = server_cfg

            self.mcp_client = MultiServerMCPClient(mcp_config)
        else:
            self.mcp_client = MultiServerMCPClient({})

    def register_tool(self, tool: BaseTool):
        """注册静态工具"""
        self.tools[tool.name] = tool
        self.enabled_tools[tool.name] = True  # 默认启用
    
    def get_all_tools(self) -> List[BaseTool]:
        """获取所有启用的工具（静态 + 动态）"""
        static_tools = []
        for tool in self.tools.values():
            # 如果 enabled_tools 字典里没有这个工具的名字，get 方法会默认返回 True
            if self.enabled_tools.get(tool.name, True):
                static_tools.append(tool)
        return static_tools + self.mcp_tools
    
    def get_all_registered_tools(self) -> List[dict]:
        """获取所有注册的工具信息（包括启用状态）"""
        result = []
        
        # 静态工具
        for tool in self.tools.values():
            result.append({
                "name": tool.name,
                "description": tool.description,
                "enabled": self.enabled_tools.get(tool.name, True),
                "source": "static"
            })
        
        # 动态工具
        for tool in self.mcp_tools:
            result.append({
                "name": tool.name,
                "description": tool.description,
                "enabled": True,
                "source": "mcp"
            })
        
        return result
    
    def get_tool(self, name: str) -> Optional[BaseTool]:
        """根据名称获取工具"""
        # 先查找静态工具
        if name in self.tools:
            return self.tools[name]
        
        # 再查找动态工具
        for tool in self.mcp_tools:
            if tool.name == name:
                return tool
        
        return None
    
    def set_tool_status(self, name: str, enabled: bool) -> bool:
        """设置工具状态（仅对静态工具有效）"""
        if name in self.tools:
            self.enabled_tools[name] = enabled
            return True
        return False
    
    async def add_mcp_server(self, server_id: str, config: dict) -> bool:
        """添加MCP服务器配置并刷新工具列表"""
        try:
            # 2. 加密 API Key（仅当存在时，通常只在 SSE 中存在）
            if config.get("api_key"):
                encrypted_api_key = crypto_utils.encrypt(config["api_key"])
                stored_config = config.copy()
                stored_config["api_key"] = encrypted_api_key
            else:
                stored_config = config.copy()

            # 3. 保存到数据库
            mongodb_client.save_mcp_server(server_id, stored_config)

            # 4. 添加到内存（解密后的版本用于运行时）
            # 注意：这里存储的是原始 config，_init_mcp_client 会负责解析
            self.mcp_server_configs[server_id] = config.copy()

            # 5. 重新初始化客户端并刷新工具
            self._init_mcp_client()
            await self.refresh_mcp_tools()
            logger.info(f"MCP服务器 {server_id} 添加成功")
            return True
            
        except Exception as e:
            logger.error(f"添加MCP服务器失败: {str(e)}")
            return False
    
    async def remove_mcp_server(self, server_id: str) -> bool:
        """断开并移除MCP服务器"""
        try:
            # 从配置中移除
            if server_id in self.mcp_server_configs:
                del self.mcp_server_configs[server_id]
            
            # 从数据库删除
            mongodb_client.delete_mcp_server(server_id)

            # 需要先关闭现有连接
            # if self.mcp_client:
            #     await self.mcp_client.__aexit__(None, None, None)
            #     self.mcp_client = None
            
            # 重新初始化MCP客户端
            self._init_mcp_client()
            
            # 刷新MCP工具列表
            await self.refresh_mcp_tools()
            
            logger.info(f"MCP服务器 {server_id} 移除成功")
            return True
            
        except Exception as e:
            logger.error(f"移除MCP服务器失败: {str(e)}")
            return False
    
    async def refresh_mcp_tools(self):
        """从当前连接的MCP服务器重新获取所有工具"""
        try:
            self.mcp_tools = []
            
            if self.mcp_client:
                # 异步调用获取工具
                tools = await self.mcp_client.get_tools()
                self.mcp_tools = tools
                logger.info(f"从MCP服务器加载了 {len(tools)} 个工具")
                # 打印每个工具的详细信息
                for tool in tools:
                    logger.debug(f"  - {tool.name}: {tool.description}")
            else:
                logger.info("没有连接的MCP服务器")
                
        except Exception as e:
            logger.error(f"刷新MCP工具失败: {str(e)}")
            self.mcp_tools = []
    
    async def load_mcp_servers_from_db(self):
        """从数据库加载已保存的MCP服务器配置"""
        try:
            servers = mongodb_client.get_all_mcp_servers()
            self.mcp_server_configs = {}
            
            for server in servers:
                server_id = server.get("server_id")
                config = server.get("config", {})
                
                if config.get("api_key"):
                    try:
                        config["api_key"] = crypto_utils.decrypt(config["api_key"])
                    except Exception as e:
                        logger.error(f"解密MCP服务器 {server_id} 的API Key失败: {str(e)}")
                        continue
                
                self.mcp_server_configs[server_id] = config
            
            # 初始化客户端
            self._init_mcp_client()

            # 2. 【关键修改】手动触发异步上下文管理器的 __aenter__ 来建立真实连接
            # if self.mcp_client:
            #     await self.mcp_client.__aenter__()
            
            # 刷新工具（正确等待异步方法）
            await self.refresh_mcp_tools()
            
        except Exception as e:
            logger.error(f"从数据库加载MCP服务器配置失败: {str(e)}")

# 创建全局工具管理器实例
tool_manager = ToolManager()
