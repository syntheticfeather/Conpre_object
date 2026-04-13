# smart-customer-service/tools/mcp_tools.py
from langchain_core.tools import tool
from typing import Optional
from tools.java_api_client import JavaApiClient

client = JavaApiClient()

@tool
async def query_application_status(config: Optional[dict] = None) -> str:
    """查询用户的贷款申请状态。当用户询问申请进度、申请状态时使用。"""
    try:
        # 从配置中获取token
        token = None
        if config and isinstance(config, dict):
            token = config.get("configurable", {}).get("token")
        
        print(f"【mcp_tools 1.2.x 获取到的 token】: {token}")

        if not token:
            return "抱歉，未提供认证信息"
        
        print("【开始调用mcp_tools 查询申请状态接口】")

        result = await client.get_application_status(token)
        if result is None:
            return "抱歉，查询申请状态失败"
        status = result.get("status", "未知")
        return f"您的贷款申请状态：{status}"
    except Exception as e:
        return f"抱歉，无法查询申请状态：{str(e)}"

@tool
async def calculate_repayment(order_id: int, config: Optional[dict] = None) -> str:
    """计算还款计划。当用户询问月还款额、还款金额、还款计划时使用。"""
    try:
        # 从配置中获取token
        token = None
        if config and isinstance(config, dict):
            token = config.get("configurable", {}).get("token")
        
        if not token:
            return "抱歉，未提供认证信息"
        
        # 调用API
        print("【开始调用mcp_tools 计算还款计划接口】")
        result = await client.calculate_repayment(order_id, token)
        monthly_payment = result.get("monthlyPayment", 0)
        total_payment = result.get("totalPayment", 0)
        repayment_plan = result.get("repaymentPlan", [])
        
        response = f"订单 {order_id} 的还款计划：\n"
        response += f"月还款额：{monthly_payment}元\n"
        response += f"总还款额：{total_payment}元\n"
        
        if repayment_plan:
            response += "\n每期还款明细：\n"
            for item in repayment_plan:
                term = item.get("term", 0)
                principal = item.get("principal", 0)
                interest = item.get("interest", 0)
                total = item.get("total", 0)
                response += f"第 {term} 期：本金 {principal} 元，利息 {interest} 元，总计 {total} 元\n"
        
        return response
    except Exception as e:
        return f"抱歉，无法计算还款计划：{str(e)}"

TOOLS = [query_application_status, calculate_repayment]