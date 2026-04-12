# smart-customer-service/tools/java_api_client.py
import httpx
from typing import Optional, Dict, Any
import os
import math

class JavaApiClient:
    def __init__(self, base_url: Optional[str] = None):
        self.base_url = base_url or os.getenv("JAVA_API_BASE_URL", "http://localhost:8080/api")

    async def get_application_status(self, token: str) -> Dict[str, Any]:
        print(f"实际传递的 token: {token}")  # 添加这行来验证
        headers = {}
        if token:
            headers["Authorization"] = f"Bearer {token}"
        
        try:
            async with httpx.AsyncClient() as client:
                response = await client.get(
                    f"{self.base_url}/loan-applications/my",
                    headers=headers
                )
                response.raise_for_status()
                data = response.json()
                applications = data.get("data", [])
                if applications:
                    latest_application = applications[0]
                    return {
                        "status": latest_application.get("status", "未知")
                    }
                return {"status": "无申请记录"}
        except Exception as e:
            print(f"API调用失败: {str(e)}")
            return {"status": "查询失败", "error": str(e)}

    async def calculate_repayment(self, order_id: int, token: str) -> Dict[str, Any]:
        print(f"实际传递的 token: {token}")  
        headers = {}
        if token:
            headers["Authorization"] = f"Bearer {token}"
        
        try:
            async with httpx.AsyncClient() as client:
                response = await client.get(
                    f"{self.base_url}/orders/{order_id}/repayment-plan",
                    headers=headers
                )
                response.raise_for_status()
                data = response.json()
                repayment_plan = data.get("data", [])
                
                if repayment_plan:
                    monthly_payment = repayment_plan[0].get("total", 0)
                    total_payment = sum(item.get("total", 0) for item in repayment_plan)
                    return {
                        "monthlyPayment": monthly_payment,
                        "totalPayment": total_payment,
                        "repaymentPlan": repayment_plan
                    }
                return {"monthlyPayment": 0, "totalPayment": 0, "repaymentPlan": []}
        except Exception as e:
            print(f"API调用失败: {str(e)}")
            return {"monthlyPayment": 0, "totalPayment": 0, "repaymentPlan": [], "error": str(e)}