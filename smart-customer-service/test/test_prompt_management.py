# test_prompt_management.py
import sys
import os

# 添加项目根目录到 Python 路径
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from utils.mongodb_client import mongodb_client
from agent.prompts import get_system_prompt, get_prompt_config


def test_get_active_prompt():
    """测试获取激活的提示词"""
    print("=== 测试获取激活的提示词 ===")
    try:
        prompt = mongodb_client.get_active_prompt()
        if prompt:
            print(f"获取到激活的提示词: {prompt['name']}")
            print(f"版本: {prompt['version']}")
            print(f"内容: {prompt['content']['role_definition']}")
        else:
            print("未找到激活的提示词")
    except Exception as e:
        print(f"测试获取激活的提示词失败: {e}")
        import traceback
        traceback.print_exc()


def test_get_prompt_config():
    """测试获取提示词配置"""
    print("\n=== 测试获取提示词配置 ===")
    config = get_prompt_config()
    print(f"保护的工具: {[tool['name'] for tool in config['protected_tools']]}")
    print(f"变量: {config['variables']}")


def test_get_system_prompt():
    """测试获取系统提示词"""
    print("\n=== 测试获取系统提示词 ===")
    prompt = get_system_prompt()
    print(f"系统提示词长度: {len(prompt)}")
    print(f"提示词前100个字符: {prompt[:100]}...")


def test_create_prompt():
    """测试创建新的提示词"""
    print("\n=== 测试创建新的提示词 ===")
    new_prompt = {
        "name": "测试提示词",
        "category": "customer_service",
        "is_active": False,
        "version": "1.1",
        "content": {
            "role_definition": "你是一个测试用的贷款智能客服。",
            "business_rules": "1. 测试规则 1\n2. 测试规则 2",
            "tone_style": "测试语气风格"
        },
        "config": {
            "protected_tools": [
                {"name": "query_application_status", "description": "查询贷款申请状态"}
            ],
            "variables": ["current_date"]
        }
    }
    result = mongodb_client.create_prompt(new_prompt)
    print(f"创建提示词结果: {result}")


def test_update_prompt():
    """测试更新提示词"""
    print("\n=== 测试更新提示词 ===")
    # 先获取一个提示词
    prompt = mongodb_client.get_active_prompt()
    if prompt:
        update_data = {
            "name": prompt["name"] + " (更新)",
            "version": "1.2"
        }
        result = mongodb_client.update_prompt(prompt["prompt_id"], update_data)
        print(f"更新提示词结果: {result}")
    else:
        print("未找到提示词进行更新")


def test_deactivate_prompt():
    """测试停用提示词"""
    print("\n=== 测试停用提示词 ===")
    # 先获取一个提示词
    prompt = mongodb_client.get_active_prompt()
    if prompt:
        result = mongodb_client.deactivate_prompt(prompt["prompt_id"])
        print(f"停用提示词结果: {result}")
        # 重新激活它
        mongodb_client.update_prompt(prompt["prompt_id"], {"is_active": True})
        print("已重新激活提示词")
    else:
        print("未找到提示词进行停用")


if __name__ == "__main__":
    print("开始测试提示词管理功能\n")
    test_get_active_prompt()
    test_get_prompt_config()
    test_get_system_prompt()
    test_create_prompt()
    test_update_prompt()
    test_deactivate_prompt()
    print("\n测试完成！")
