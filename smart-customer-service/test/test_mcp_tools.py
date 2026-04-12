# smart-customer-service/tests/test_mcp_tools.py
import pytest
from unittest.mock import AsyncMock, patch
from tools.mcp_tools import query_application_status, calculate_repayment

@pytest.mark.asyncio
async def test_query_application_status():
    with patch("tools.java_api_client.JavaApiClient.get_application_status") as mock:
        mock.return_value = {"status": "审批中", "stage": "风控审核"}
        result = await query_application_status.ainvoke({"token": "test_token"})
        assert "审批中" in result

@pytest.mark.asyncio
async def test_calculate_repayment():
    with patch("tools.java_api_client.JavaApiClient.calculate_repayment") as mock:
        mock.return_value = {"monthlyPayment": 5000, "totalPayment": 60000}
        result = await calculate_repayment.ainvoke({"order_id": 1, "token": "test_token"})
        assert "5000" in result