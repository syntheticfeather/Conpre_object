# test_chat_agent.py
import asyncio
from agent.chat_agent import ChatAgent

async def test_chat():
    print("=== 开始测试 ChatAgent ===")
    agent = ChatAgent()
    print("=== ChatAgent 初始化完成 ===")
    
    try:
        async for chunk in agent.chat("你好", "test-token"):
            print(f"收到: {chunk}")
    except Exception as e:
        print(f"测试错误: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    asyncio.run(test_chat())
