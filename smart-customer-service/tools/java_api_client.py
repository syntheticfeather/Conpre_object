# smart-customer-service/tools/java_api_client.py
import httpx
from typing import Optional, Dict, Any
import os
import math

class JavaApiClient:
    def __init__(self, base_url: Optional[str] = None):
        self.base_url = base_url or os.getenv("LOAN_SERVICE_URL", "http://localhost:8080/api")

    async def get_application_status(self, application_id: int, token: str) -> Dict[str, Any]:
        print(f"token: {token}, application_id: {application_id}")
        headers = {}
        if token:
            headers["Authorization"] = f"Bearer {token}"
        
        try:
            async with httpx.AsyncClient() as client:
                response = await client.get(
                    f"{self.base_url}/loan-applications/my/{application_id}",
                    headers=headers
                )
                response.raise_for_status()
                data = response.json()
                application = data.get("data", {})
                return {
                    "status": application.get("status", "未知"),
                    "amount": application.get("amount", 0),
                    "term": application.get("term", 0),
                    "purpose": application.get("purpose", "")
                }
        except Exception as e:
            print(f"查询贷款申请状态API调用失败: {str(e)}")
            return {"status": "查询失败", "error": str(e)}

    async def get_loan_products(self, token: str) -> Dict[str, Any]:
        """获取所有贷款产品列表"""
        headers = {}
        if token:
            headers["Authorization"] = f"Bearer {token}"
        try:
            async with httpx.AsyncClient() as client:
                response = await client.get(
                    f"{self.base_url}/loan-products/user",
                    headers=headers
                )
                response.raise_for_status()
                data = response.json()
                products = data.get("data", [])
                return {
                    "products": products,
                    "success": True
                }
        except Exception as e:
            print(f"获取贷款产品API调用失败: {str(e)}")
            return {"products": [], "success": False, "error": str(e)}