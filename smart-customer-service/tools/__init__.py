# smart-customer-service/tools/__init__.py

# 导入工具模块，确保工具被注册
from tools import backend_tools
from tools import search_tools
from tools.tool_manager import tool_manager, ToolManager

# 导出工具管理器和所有工具
__all__ = ['tool_manager', 'ToolManager']
