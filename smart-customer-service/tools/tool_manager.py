# smart-customer-service/tools/tool_manager.py
from typing import List, Dict, Any, Callable
from langchain_core.tools import BaseTool

class ToolManager:
    """工具管理器，用于注册和管理所有可用工具"""
    
    def __init__(self):
        self.tools: Dict[str, BaseTool] = {}
        self.enabled_tools: Dict[str, bool] = {}  # 跟踪工具的启用状态
    
    def register_tool(self, tool: BaseTool):
        """注册工具"""
        self.tools[tool.name] = tool
        self.enabled_tools[tool.name] = True  # 默认启用
    
    def get_all_tools(self) -> List[BaseTool]:
        """获取所有启用的工具"""
        return [tool for tool in self.tools.values() if self.enabled_tools.get(tool.name, True)]
    
    def get_all_registered_tools(self) -> List[dict]:
        """获取所有注册的工具，包括启用状态"""
        return [
            {
                "name": tool.name,
                "description": tool.description,
                "enabled": self.enabled_tools.get(tool.name, True)
            }
            for tool in self.tools.values()
        ]
    
    def get_tool(self, name: str) -> BaseTool:
        """根据名称获取工具"""
        return self.tools.get(name)
    
    # def get_tool_descriptions(self) -> str:
    #     """获取启用工具的描述，用于注入到提示词中"""
    #     descriptions = []
    #     for tool in self.tools.values():
    #         if self.enabled_tools.get(tool.name, True):
    #             descriptions.append(f"- {tool.name}: {tool.description}")
    #     return "\n".join(descriptions)
    
    def set_tool_status(self, name: str, enabled: bool) -> bool:
        """设置工具状态"""
        if name in self.tools:
            self.enabled_tools[name] = enabled
            return True
        return False

# 创建全局工具管理器实例
tool_manager = ToolManager()
