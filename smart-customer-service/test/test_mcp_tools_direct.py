# 直接测试 mcp_tools.py
from tools.mcp_tools import query_application_status, calculate_repayment

# 测试 query_application_status
print("测试 query_application_status...")
try:
    result = query_application_status.invoke({"token": "test_token"})
    print(f"结果: {result}")
except Exception as e:
    print(f"错误: {str(e)}")

# 测试 calculate_repayment
print("\n测试 calculate_repayment...")
try:
    result = calculate_repayment.invoke({"order_id": 1, "token": "test_token"})
    print(f"结果: {result}")
except Exception as e:
    print(f"错误: {str(e)}")
