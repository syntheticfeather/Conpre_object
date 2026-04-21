# smart-customer-service/tools/backend_tools.py
from langchain_core.tools import tool
from tools.java_api_client import JavaApiClient
from utils.context import get_token
from utils.repayment_calculator import RepaymentCalculator
from tools.tool_manager import tool_manager
from decimal import Decimal

client = JavaApiClient()

@tool
async def query_application_status(application_id: int) -> str:
    """查询用户的贷款申请状态。当用户询问申请进度、申请状态时使用。"""
    try:
        token = get_token()

        if not token:
            return "抱歉，未提供认证信息"

        print("【开始调用查询申请状态接口】")

        result = await client.get_application_status(application_id, token)
        if result is None:
            return "抱歉，查询申请状态失败"
        status = result.get("status", "未知")
        return f"您的贷款申请状态：{status}"
    except Exception as e:
        return f"抱歉，无法查询申请状态：{str(e)}"

@tool
async def calculate_repayment(repaid_type: str, loan_amount: float, loan_period: int, interest_rate: float) -> str:
    """计算还款计划。当用户询问月还款额、还款金额、还款计划时使用。

    参数说明：
    - repaid_type: 还款方式，支持"等额本息"、"等额本金"、"先息后本"、"一次性还本付息"
    - loan_amount: 贷款金额，单位元
    - loan_period: 贷款期限，单位月
    - interest_rate: 年利率，如0.12表示12%
    """
    try:
        loan_amount_decimal = Decimal(str(loan_amount))
        interest_rate_decimal = Decimal(str(interest_rate))

        print(f"【开始计算还款计划】还款方式:{repaid_type}, 贷款金额:{loan_amount}, 贷款期限:{loan_period}, 年利率:{interest_rate}")

        result = RepaymentCalculator.calculate(repaid_type, loan_amount_decimal, loan_period, interest_rate_decimal)

        monthly_payment = result.get("monthlyPayment", "0")
        total_payment = result.get("totalPayment", "0")
        total_interest = result.get("totalInterest", "0")
        repayment_plan = result.get("repaymentPlan", [])

        response = f"还款计划详情：\n"
        response += f"还款方式：{repaid_type}\n"
        response += f"贷款金额：{loan_amount}元\n"
        response += f"贷款期限：{loan_period}期\n"
        response += f"年利率：{interest_rate}\n"
        response += f"月还款额：{monthly_payment}元\n"
        response += f"总还款额：{total_payment}元\n"
        response += f"总利息：{total_interest}元\n"

        if repayment_plan:
            response += "\n每期还款明细：\n"
            for item in repayment_plan:
                term = item.get("term", 0)
                principal = item.get("principal", 0)
                interest = item.get("interest", 0)
                total = item.get("total", 0)
                response += f"第 {term} 期：本金 {principal} 元，利息 {interest} 元，总计 {total} 元\n"

        return response
    except ValueError as e:
        return f"抱歉，计算还款计划失败：{str(e)}"
    except Exception as e:
        return f"抱歉，无法计算还款计划：{str(e)}"

tool_manager.register_tool(query_application_status)
tool_manager.register_tool(calculate_repayment)
