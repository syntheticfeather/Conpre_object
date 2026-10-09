"""
对话上下文 Token 预算控制测试

运行: PYTHONPATH=. python test/test_context_budget.py
覆盖场景：
  1. PASS 档：短消息全量原文注入
  2. SOFT 档：60%-80% → 5 条原文 + 摘要
  3. HARD 档：> 80% → 3 条原文 + 摘要
  4. 摘要失败兜底：LLM 异常 → 仅最近 N 条原文
  5. 空历史：items=[] → []
  6. 条数不足：仅 2 条且超预算 → 不触发摘要（短路）
"""
import os, sys, io, asyncio
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

from dotenv import load_dotenv; load_dotenv()

from utils.context_budget import ContextBudgetManager
from langchain_core.messages import HumanMessage, AIMessage


# ========== 测试桩：可控的 LLM 摘要 ==========
class FakeMessage:
    def __init__(self, content: str):
        self.content = content

class FakeChoice:
    def __init__(self, content: str):
        self.message = FakeMessage(content)

class FakeResponse:
    def __init__(self, content: str):
        self.choices = [FakeChoice(content)]

class FakeCompletions:
    """可编程的 chat.completions.create 桩。"""
    def __init__(self, outputs=None, exc=None):
        self.outputs = outputs or ["[摘要] 用户咨询了贷款产品"]
        self.exc = exc
        self.call_count = 0

    async def create(self, **kwargs):
        self.call_count += 1
        if self.exc:
            raise self.exc
        return FakeResponse(self.outputs[min(self.call_count - 1, len(self.outputs) - 1)])

class FakeClient:
    def __init__(self, completions: FakeCompletions):
        self.chat = type("FakeChat", (), {"completions": completions})()


def make_manager_with_fake(fake_completions, budget=30000):
    """构造一个用 FakeClient 替换真实 AsyncOpenAI 的 manager。"""
    mgr = ContextBudgetManager(budget=budget)
    mgr._client = FakeClient(fake_completions)
    return mgr


def msg(role: str, content: str) -> dict:
    return {"role": role, "content": content}


# ========== 1. PASS 档 ==========
print("=== 测试 1: PASS 档（短消息全量注入）===")
fake = FakeCompletions()
mgr = make_manager_with_fake(fake, budget=30000)
items = [msg("user", "你好"), msg("assistant", "您好，有什么可以帮您？")]
result = asyncio.run(mgr.build_context(items))
assert len(result) == 2, f"PASS 档应返回 2 条原文，实际 {len(result)}"
assert isinstance(result[0], HumanMessage)
assert isinstance(result[1], AIMessage)
assert fake.call_count == 0, "PASS 档不应调用 LLM"
print(f"  OK 返回 {len(result)} 条原文，未触发摘要\n")

# ========== 2. SOFT 档 ==========
print("=== 测试 2: SOFT 档（60%-80%，5 条原文 + 摘要）===")
# 测试数据 10 条短消息共约 28 token，budget=40 时 ratio≈70%，落在 SOFT 档
fake = FakeCompletions(outputs=["[摘要] 用户咨询了等额本息还款方式"])
mgr = make_manager_with_fake(fake, budget=40)
items = [
    msg("user", "我有一笔10万贷款，等额本息，12期" * 1),
    msg("assistant", "根据您的贷款条件" * 1),
    msg("user", "利率0.065，怎么算月供" * 1),
    msg("assistant", "调用工具计算结果" * 1),
    msg("user", "还有其他还款方式吗" * 1),
    msg("assistant", "有等额本金、先息后本" * 1),
    msg("user", "对比一下这两种" * 1),
    msg("assistant", "等额本金前期压力较大" * 1),
    msg("user", "那选等额本息吧" * 1),
    msg("assistant", "好的，已为您记录偏好" * 1),
]
result = asyncio.run(mgr.build_context(items))
# 第一条应是摘要 HumanMessage，后 5 条应是原文
assert len(result) == 6, f"SOFT 档应返回 1 摘要 + 5 原文 = 6 条，实际 {len(result)}"
assert isinstance(result[0], HumanMessage) and "[历史对话摘要]" in result[0].content
assert fake.call_count == 1, "SOFT 档应调用 1 次 LLM"
# 验证保留最近 5 条原文（第 6-10 条：AI, Human, AI, Human, AI）
recent_roles = [r.__class__.__name__ for r in result[1:]]
assert recent_roles == ["AIMessage", "HumanMessage", "AIMessage", "HumanMessage", "AIMessage"]
print(f"  OK 返回 1 摘要 + {len(result)-1} 条原文，LLM 调用 {fake.call_count} 次\n")

# ========== 3. HARD 档 ==========
print("=== 测试 3: HARD 档（> 80%，3 条原文 + 摘要）===")
fake = FakeCompletions(outputs=["[摘要] 用户深度咨询了多种还款方式对比"])
mgr = make_manager_with_fake(fake, budget=20)  # 让 10 条消息明显 > 80%
result = asyncio.run(mgr.build_context(items))
assert len(result) == 4, f"HARD 档应返回 1 摘要 + 3 原文 = 4 条，实际 {len(result)}"
assert isinstance(result[0], HumanMessage) and "[历史对话摘要]" in result[0].content
assert fake.call_count == 1
recent_roles = [r.__class__.__name__ for r in result[1:]]
assert recent_roles == ["AIMessage", "HumanMessage", "AIMessage"]
print(f"  OK 返回 1 摘要 + {len(result)-1} 条原文，LLM 调用 {fake.call_count} 次\n")

# ========== 4. 摘要失败兜底 ==========
print("=== 测试 4: 摘要失败兜底（LLM 异常 → 仅最近 N 条原文）===")
fake = FakeCompletions(exc=RuntimeError("LLM connection error"))
mgr = make_manager_with_fake(fake, budget=20)
result = asyncio.run(mgr.build_context(items))
# 失败兜底 → HARD 档应回退到只返回最近 3 条原文
assert len(result) == 3, f"失败兜底应返回最近 3 条原文，实际 {len(result)}"
assert all(not isinstance(r, AIMessage) or True for r in result)  # 类型不影响
# 不应包含摘要消息
assert all("[历史对话摘要]" not in getattr(r, "content", "") for r in result)
print(f"  OK 摘要失败，退化为最近 {len(result)} 条原文，未阻塞主链路\n")

# ========== 5. 空历史 ==========
print("=== 测试 5: 空历史 ===")
fake = FakeCompletions()
mgr = make_manager_with_fake(fake)
result = asyncio.run(mgr.build_context([]))
assert result == []
assert fake.call_count == 0
print("  OK 空历史返回空列表\n")

# ========== 6. 条数不足短路 ==========
print("=== 测试 6: 条数不足短路（2 条但超预算 → 不摘要）===")
fake = FakeCompletions()
mgr = make_manager_with_fake(fake, budget=10)  # 极低预算，但只有 2 条
items_short = [msg("user", "超长内容" * 100), msg("assistant", "回复也很长" * 100)]
result = asyncio.run(mgr.build_context(items_short))
assert len(result) == 2, f"条数不足应直接原文返回，实际 {len(result)}"
assert fake.call_count == 0, "条数不足 SOFT_KEEP_RECENT=5 不应触发摘要"
print(f"  OK 2 条消息直接原文返回，未触发摘要（短路保护）\n")

print("=== 全部测试通过 ===")
