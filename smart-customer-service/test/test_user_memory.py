"""
长期记忆系统测试
运行: PYTHONPATH=. python test/test_user_memory.py
"""
import os, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

os.environ["CHROMADB_MODE"] = "local"
os.environ["CHROMADB_PERSIST_DIRECTORY"] = "./chromadb_test"

from dotenv import load_dotenv; load_dotenv()
from memory.user_memory import UserMemoryStore, DECAY_HALF_LIFE

store = UserMemoryStore()
TEST_USER = "test_user_001"

# ========== 1. 写记忆 ==========
print("=== 测试 1: 写入记忆 ===")

memories = [
    ("preference", "用户偏好等额本息还款方式", "repaid_type", 0.9),
    ("preference", "用户偏好低利率贷款产品", "rate_pref", 0.7),
    ("fact",       "用户已申请个人消费贷20万", "application", 0.8),
    ("fact",       "用户月收入15000元", "income", 0.6),
    ("habit",      "用户经常先查看产品再申请", "workflow", 0.5),
    ("preference", "用户喜欢短期贷款，不喜欢3年期以上的", "term_pref", 0.7),
]

for mem_type, content, key, imp in memories:
    ok = store.save(TEST_USER, content, mem_type, key, imp)
    print(f"  {'OK' if ok else 'FAIL'} [{mem_type}] {key}: {content}")

# ========== 2. 查记忆 ==========
print("\n=== 测试 2: 语义检索 ===")

queries = [
    ("算月供",     "应召回等额本息偏好"),
    ("帮我推荐产品", "应召回低利率+等额本息+短期偏好"),
    ("申请贷款",   "应召回已申请信息"),
    ("退款怎么退",  "不太相关，score 应偏低"),
]

for query, expect in queries:
    results = store.search(TEST_USER, query, top_k=3)
    if results:
        for i, r in enumerate(results):
            print(f"  [{i+1}] score={r['score']:.3f} | {r['document']}")
    else:
        print(f"  (无结果)")
    print()

# ========== 3. 实体匹配 ==========
print("=== 测试 3: 实体匹配 ===")
# 查询中带"等额本息" → 应该给含"等额本息"的记忆额外的实体加分
r1 = store.search(TEST_USER, "等额本息", top_k=3)
for r in r1:
    print(f"  score={r['score']:.3f} | entities={r['metadata'].get('entities','')} | {r['document']}")

# ========== 4. 冲突检测 (Zep式) ==========
print("\n=== 测试 4: Zep 冲突检测 ===")
store.save(TEST_USER, "用户最近从等额本息改为等额本金", "preference", "repaid_type", 0.9)
# 同 key 的旧记忆应该被标为过期
results = store.search(TEST_USER, "还款方式", top_k=5)
print("  更新后的 repaid_type 记忆:")
for r in results:
    if r['metadata'].get('key') == 'repaid_type':
        print(f"    score={r['score']:.3f} | valid_until={r['metadata'].get('valid_until','')[:16]} | {r['document']}")

# ========== 5. 统计 ==========
print("\n=== 测试 5: 统计 ===")
stats = store.stats(TEST_USER)
print(f"  total={stats['total']} active={stats['active']} expired={stats['expired']}")

# ========== 6. 时间衰减验证 ==========
print("\n=== 测试 6: 时间衰减 ===")
from datetime import datetime, timedelta
# 计算 90 天前的衰减值
now = datetime.now()
for days in [0, 30, 90, 180]:
    dt = (now - timedelta(days=days)).isoformat()
    decay = store._calc_decay(dt, now)
    print(f"  {days}天后衰减: {decay:.4f}")

# ========== 7. 清理 ==========
store.delete(TEST_USER)
print(f"\n清理后: {store.stats(TEST_USER)}")
print("\n=== 全部测试完成 ===")
