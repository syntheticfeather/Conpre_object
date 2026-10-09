# smart-customer-service/tools/recommend_tools.py
"""产品推荐工具：调 Java 后端推荐接口，基于用户信用分规则打分返回 top-3 产品。"""
import os
import logging
import httpx
from langchain_core.tools import tool
from utils.context import get_token
from tools.tool_manager import tool_manager

logger = logging.getLogger(__name__)

JAVA_URL = os.getenv("JAVA_URL", "http://localhost:8080")


@tool
async def recommend_products() -> str:
    """基于用户信用分推荐适合的贷款产品（3款），返回产品名称、额度、利率及推荐理由。

    何时使用：用户问"帮我推荐个产品""以我的条件能贷什么""哪个产品适合我"等需要个性化推荐的问题。
    何时不要使用：用户只是要看所有产品列表（用 search_knowledge 检索产品知识）；
                  计算具体月供（该走系统计算）；查自己的还款明细或信用分（系统自动处理）。
    失败处理：Java 接口不可用时告知用户暂时无法推荐，引导用户查看产品列表自行选择，严禁编造推荐结果。

    Returns:
        推荐产品列表文本，含产品名、额度、利率、推荐理由
    """
    token = get_token()
    if not token:
        return "推荐失败：无法获取用户身份信息，请重新登录后再试。"

    try:
        async with httpx.AsyncClient(timeout=10) as client:
            resp = await client.get(
                f"{JAVA_URL}/api/loan-products/recommend",
                headers={"Authorization": f"Bearer {token}"},
            )
            resp.raise_for_status()
            data = resp.json()

        products = data.get("data", [])
        if not products:
            return '暂时没有适合您的推荐产品，请稍后再来查看，或直接说"查看所有产品"浏览完整列表。'

        def fmt_amount(v):
            """金额：整数值去掉 .0 尾巴"""
            try:
                return str(int(float(v)))
            except (TypeError, ValueError):
                return "?"

        lines = ["根据您的信用状况，为您推荐以下产品：\n"]
        for i, p in enumerate(products, 1):
            lines.append(f"{i}. {p.get('productName', '未知产品')}")
            lines.append(f"   额度：{fmt_amount(p.get('minAmount'))}~{fmt_amount(p.get('maxAmount'))}元")
            if p.get("options"):
                rates = [opt.get("interestRate") for opt in p["options"] if opt.get("interestRate") is not None]
                if rates:
                    # interestRate 为小数（0.038=3.8%），乘 100 后保留两位小数
                    rates_pct = [round(r * 100, 2) for r in rates]
                    lines.append(f"   利率：{min(rates_pct)}%~{max(rates_pct)}%")
            if p.get("recommendReason"):
                lines.append(f"   推荐理由：{p['recommendReason']}")
            lines.append("")

        lines.append("注：推荐结果仅供参考，具体以系统审批为准。如需申请可直接告诉我。")
        return "\n".join(lines)

    except httpx.HTTPStatusError as e:
        logger.warning(f"recommend_products HTTP 错误: {e.response.status_code}")
        return '暂时无法获取推荐产品，请稍后重试，或直接说"查看所有产品"。'
    except Exception as e:
        logger.warning(f"recommend_products 调用失败: {e}")
        return "暂时无法获取推荐产品，请稍后重试。"


tool_manager.register_tool(recommend_products)
