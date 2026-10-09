"""
提示词精细化 Phase 0 — 提示词评测基准框架 (2026-09-22)

用法:
    cd smart-customer-service
    PYTHONIOENCODING=utf-8 python test/benchmark_prompt.py
    PYTHONIOENCODING=utf-8 python test/benchmark_prompt.py --groups route
    PYTHONIOENCODING=utf-8 python test/benchmark_prompt.py --groups tool,answer

前置条件:
    - route 组: Python 服务在 AGENT_URL(默认 http://localhost:8000) 运行
    - tool/answer 组: MongoDB 可用（知识库 + MCP 配置）；MCP 工具类用例额外要求
      Java MCP 服务在线，否则对应 case 记为 skipped（不算失败）

设计说明:
    - route 组走真实 HTTP /api/route（非 mock）
    - tool/answer 组进程内直调真实 Agent 链（react 模式，真实 LLM + 真实工具），
      原因：/api/chat/stream 的 SSE 层过滤了 tool_call 事件（main.py 124-128 行），
      HTTP 无法观察实际工具调用；进程内直调可收到 ReactAgent 原始 tool_call chunk
    - 每条会话使用独立 bench-* session_id + 专用评测 user_id(999999)，不污染真实数据
    - LLM temperature=0.7，结果具有随机性，报告记录单次基线（Phase 2 起作为回归对照）

指标:
    route : 意图准确率（总体/分意图/分难度）、分流正确率、置信度分数段校准表
    tool  : 工具命中率、误调用率、skip 数（期望工具未加载时）
    answer: 要点覆盖率、通过率（覆盖≥0.66 + 长度 + 无禁词 + 无错误）、禁词违规
"""
import os
import sys
import io
import json
import time
import socket
import asyncio
import argparse
import urllib.request
import urllib.error
from datetime import datetime, timedelta

# 项目根入 sys.path
_PROJECT_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
if _PROJECT_ROOT not in sys.path:
    sys.path.insert(0, _PROJECT_ROOT)

if hasattr(sys.stdout, "buffer"):
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")

# ── 配置 ──────────────────────────────────────────────
AGENT_URL = os.environ.get("AGENT_URL", "http://localhost:8000")
CASES_PATH = os.path.join(os.path.dirname(__file__), "eval_cases.json")
RESULTS_DIR = os.path.join(os.path.dirname(__file__), "benchmark_results")

AGENT_MODE = "react"                 # 经确认：tool/answer 基线用 react 模式
BENCH_USER_ID = "999999"             # 专用评测用户，避免污染真实用户历史
CASE_TIMEOUT = 180                   # 单条对话超时（秒）
PASS_COVERAGE = 0.66                # answer 组要点覆盖率通过线
CONF_BINS = [(0.0, 0.5), (0.5, 0.6), (0.6, 0.7), (0.7, 0.8), (0.8, 0.9), (0.9, 1.01)]


# ── 工具函数 ──────────────────────────────────────────

def port_open(host_port: str) -> bool:
    """检测 host:port 是否可连"""
    try:
        host, port = host_port.replace("http://", "").replace("https://", "").split(":")
        s = socket.socket()
        s.settimeout(1.5)
        ok = s.connect_ex((host, int(port))) == 0
        s.close()
        return ok
    except Exception:
        return False


def http_post_json(url: str, body: dict, timeout: int = 30, retries: int = 2):
    """POST JSON，对连接重置/瞬时网络错误重试（LLM 服务偶发 reset）"""
    last_exc = None
    for attempt in range(retries + 1):
        data = json.dumps(body, ensure_ascii=False).encode("utf-8")
        req = urllib.request.Request(url, data=data, headers={"Content-Type": "application/json"})
        try:
            with urllib.request.urlopen(req, timeout=timeout) as resp:
                return resp.status, json.loads(resp.read().decode("utf-8"))
        except urllib.error.HTTPError as e:
            try:
                return e.code, json.loads(e.read().decode("utf-8"))
            except Exception:
                return e.code, None
        except (urllib.error.URLError, ConnectionError, OSError) as e:
            last_exc = e
            if attempt < retries:
                time.sleep(2)
    raise last_exc


def make_bench_token() -> str:
    """用与 Java 对齐的共享密钥签发评测专用 JWT（userId=999999）"""
    import jwt
    from utils.token_utils import _resolve_secret_key
    payload = {
        "userId": BENCH_USER_ID,
        "sub": "benchmark",
        "exp": datetime.utcnow() + timedelta(hours=2),
    }
    return jwt.encode(payload, _resolve_secret_key(), algorithm="HS256")


def confidence_bin(c: float) -> str:
    for lo, hi in CONF_BINS:
        if lo <= c < hi:
            return f"{lo:.1f}-{hi if hi <= 1 else 1.0:.1f}"
    return "unknown"


# ── route 组：真实 HTTP ───────────────────────────────

def run_route_group(cases: list) -> list:
    print("\n" + "=" * 70)
    print("  [route] 意图分类基线（真实 HTTP /api/route）")
    print("=" * 70)

    if not port_open(AGENT_URL.split("//", 1)[-1]):
        print(f"  [SKIP] {AGENT_URL} 不可达，跳过 route 组（请先启动 Python 服务）")
        return []

    details = []
    for case in cases:
        t0 = time.perf_counter()
        try:
            status, result = http_post_json(f"{AGENT_URL}/api/route", {"message": case["input"]})
        except Exception as e:
            latency = (time.perf_counter() - t0) * 1000
            details.append({**_base(case), "status": "error",
                            "error": f"{type(e).__name__}: {e}", "latency_ms": round(latency)})
            print(f"  ✗ {case['case_id']} 网络异常: {type(e).__name__}")
            time.sleep(2)
            continue
        latency = (time.perf_counter() - t0) * 1000

        if status != 200 or not isinstance(result, dict):
            detail = {**_base(case), "status": "error", "http_status": status, "latency_ms": round(latency)}
            print(f"  ✗ {case['case_id']} HTTP {status}")
            details.append(detail)
            continue

        got_intent = result.get("intent", "")
        got_conf = float(result.get("confidence", 0.0))
        got_action = result.get("action", "")
        intent_ok = got_intent == case["expected"]["intent"]
        expected_action = case["expected"].get("action")
        action_ok = (got_action == expected_action) if expected_action else None

        detail = {
            **_base(case),
            "status": "pass" if intent_ok else "fail",
            "got_intent": got_intent,
            "got_confidence": got_conf,
            "got_action": got_action,
            "intent_correct": intent_ok,
            "action_correct": action_ok,
            "confidence_bin": confidence_bin(got_conf),
            "latency_ms": round(latency),
        }
        details.append(detail)
        mark = "✓" if intent_ok else "✗"
        act_str = f" action={got_action}" + ("" if action_ok is None or action_ok else "!")
        print(f"  {mark} {case['case_id']} [{case['difficulty']:>11}] "
              f"{got_intent:<14} c={got_conf:.2f}{act_str}  ({latency/1000:.1f}s)  \"{case['input'][:28]}\"")
    return details


# ── tool/answer 组：进程内真实 Agent 链 ───────────────

async def run_chat_groups(cases: list, groups: set, intent_source: str = "none") -> list:
    """进程内直调 ChatAgent(react)，收集 message / tool_call / error 三类原始 chunk。

    intent_source="route" 时，每条先调真实 /api/route 取得 intent/confidence 再透传，
    复刻生产 Java 链路（Phase 2 起用于观测意图片段命中率）。
    """
    from tools.tool_manager import tool_manager
    from agent.chat_agent import get_chat_agent
    from agent.prompts import resolve_category

    print("\n" + "=" * 70)
    print("  [tool/answer] 进程内真实 Agent 链（react）— 初始化")
    print("=" * 70)

    # 复刻 main.py startup：从 DB 加载 MCP 服务器配置与动态工具
    try:
        await tool_manager.load_mcp_servers_from_db()
        print("  MCP 配置加载完成")
    except Exception as e:
        print(f"  [WARN] MCP 配置加载失败（动态工具将缺失）: {e}")

    loaded_tools = [t.name for t in tool_manager.get_all_tools()]
    print(f"  当前已加载工具({len(loaded_tools)}): {loaded_tools}")

    token = make_bench_token()
    agent = get_chat_agent(AGENT_MODE)   # 必须在 MCP 加载之后构造（工具在构造期绑定）
    run_ts = datetime.now().strftime("%H%M%S")

    details = []
    for case in cases:
        if case["group"] not in groups:
            continue

        expected = case["expected"]
        expected_tools = expected.get("tools", [])
        # 期望工具未加载 → skip（如 Java MCP 离线时的 calculate_repayment 用例）
        missing = [t for t in expected_tools if t not in loaded_tools]
        if missing:
            detail = {**_base(case), "status": "skipped",
                      "reason": f"期望工具未加载: {missing}", "called_tools": []}
            details.append(detail)
            print(f"  ⊘ {case['case_id']} SKIP（{', '.join(missing)} 未加载）")
            continue

        session_id = f"bench-{case['case_id']}-{run_ts}"
        answer_parts, called_tools, errors = [], [], []

        # 生产链路：先真实分类，再把 intent/confidence 原样透传给 Agent
        intent, confidence, category = None, 1.0, "general"
        if intent_source == "route":
            try:
                _, route_result = http_post_json(
                    f"{AGENT_URL}/api/route", {"message": case["input"]}
                )
                intent = route_result.get("intent")
                confidence = float(route_result.get("confidence", 0.0))
                category = resolve_category(intent, confidence)
            except Exception as e:
                print(f"      [WARN] {case['case_id']} route 预分类失败，按缺省透传: {e}")

        t0 = time.perf_counter()
        try:
            async for chunk in _consume(agent.chat(
                case["input"], session_id, BENCH_USER_ID, token,
                intent=intent, confidence=confidence,
            )):
                try:
                    evt = json.loads(chunk)
                except json.JSONDecodeError:
                    answer_parts.append(chunk)
                    continue
                etype = evt.get("type") or evt.get("event")
                if etype == "message":
                    answer_parts.append(evt.get("content", ""))
                elif etype == "tool_call":
                    called_tools.append(evt.get("tool_name", ""))
                elif etype == "error":
                    errors.append(evt.get("message", "unknown"))
        except asyncio.TimeoutError:
            errors.append(f"超过 {CASE_TIMEOUT}s 超时")
        except Exception as e:
            errors.append(f"{type(e).__name__}: {e}")
        latency = (time.perf_counter() - t0) * 1000
        answer = "".join(answer_parts)

        if case["group"] == "tool":
            detail = _score_tool(case, called_tools, answer, errors, latency)
        else:
            detail = _score_answer(case, answer, called_tools, errors, latency)
        if intent_source == "route":
            detail.update({"routed_intent": intent, "routed_confidence": round(confidence, 4),
                           "prompt_category": category})
        details.append(detail)

        icon = {"pass": "✓", "fail": "✗", "skipped": "⊘"}.get(detail["status"], "?")
        extra = f"tools={called_tools}" if case["group"] == "tool" else f"len={len(answer)}"
        print(f"  {icon} {case['case_id']} [{case['difficulty']:>11}] {extra}  "
              f"({latency/1000:.1f}s)  \"{case['input'][:26]}\"")
        if detail["status"] == "fail":
            print(f"      → {detail.get('fail_reasons')}")
    return details


async def _consume(agen, timeout=CASE_TIMEOUT):
    """带超时的 async generator 消费"""
    while True:
        try:
            chunk = await asyncio.wait_for(agen.__anext__(), timeout=timeout)
        except StopAsyncIteration:
            break
        yield chunk


def _base(case: dict) -> dict:
    return {"case_id": case["case_id"], "group": case["group"],
            "difficulty": case["difficulty"], "input": case["input"], "note": case.get("note", "")}


def _score_tool(case, called_tools, answer, errors, latency) -> dict:
    expected = case["expected"]
    wanted = expected.get("tools", [])
    forbidden = expected.get("forbidden_tools", [])
    hit = any(t in called_tools for t in wanted)
    misused = [t for t in forbidden if t in called_tools]
    reasons = []
    if errors:
        reasons.append(f"执行错误: {errors[0][:80]}")
    if not hit:
        reasons.append(f"未调用期望工具 {wanted}，实际 {called_tools}")
    if misused:
        reasons.append(f"误用禁用工具 {misused}")
    return {
        **_base(case), "status": "fail" if reasons else "pass",
        "called_tools": called_tools, "hit": hit and not errors,
        "misused_tools": misused, "fail_reasons": reasons,
        "answer_preview": answer[:200], "latency_ms": round(latency),
    }


def _score_answer(case, answer, called_tools, errors, latency, global_forbidden=None) -> dict:
    expected = case["expected"]
    must = expected.get("must_contain", [])
    must_not = list(expected.get("must_not_contain", []))
    if global_forbidden:
        must_not = must_not + list(global_forbidden)
    match_mode = expected.get("match", "all")
    min_len = expected.get("min_length", 15)

    matched = [k for k in must if k in answer]
    coverage = len(matched) / len(must) if must else 1.0
    violations = [k for k in must_not if k in answer]
    reasons = []
    if errors:
        reasons.append(f"执行错误: {errors[0][:80]}")
    if len(answer) < min_len:
        reasons.append(f"回复过短 {len(answer)}<{min_len}")
    if must and not ((any(k in answer for k in must) if match_mode == "any" else coverage >= PASS_COVERAGE)):
        reasons.append(f"要点覆盖不足 {matched}/{must}")
    if violations:
        reasons.append(f"禁词违规: {violations}")

    return {
        **_base(case), "status": "fail" if reasons else "pass",
        "coverage": round(coverage, 3), "matched_keywords": matched,
        "forbidden_hits": violations, "called_tools": called_tools,
        "fail_reasons": reasons, "answer": answer, "latency_ms": round(latency),
    }


# ── 汇总与报告 ────────────────────────────────────────

def summarize(details: list, global_forbidden: list) -> dict:
    summary = {"generated_at": datetime.now().strftime("%Y-%m-%d %H:%M:%S"),
               "agent_url": AGENT_URL, "agent_mode": AGENT_MODE, "groups": {}}

    route = [d for d in details if d["group"] == "route"]
    if route:
        n = len(route)
        correct = [d for d in route if d.get("intent_correct")]
        per_intent, per_diff, bins = {}, {}, {}
        for d in route:
            exp_intent = next(c["expected"]["intent"] for c in RAW_CASES if c["case_id"] == d["case_id"])
            per_intent.setdefault(exp_intent, [0, 0])
            per_intent[exp_intent][1] += 1
            if d.get("intent_correct"):
                per_intent[exp_intent][0] += 1
            per_diff.setdefault(d["difficulty"], [0, 0])
            per_diff[d["difficulty"]][1] += 1
            if d.get("intent_correct"):
                per_diff[d["difficulty"]][0] += 1
            b = d.get("confidence_bin")
            if b:
                bins.setdefault(b, [0, 0])
                bins[b][1] += 1
                if d.get("intent_correct"):
                    bins[b][0] += 1
        action_total = [d for d in route if d.get("action_correct") is not None]
        summary["groups"]["route"] = {
            "total": n,
            "intent_accuracy": round(len(correct) / n, 4),
            "per_intent": {k: round(v[0] / v[1], 4) for k, v in sorted(per_intent.items())},
            "per_intent_counts": {k: {"correct": v[0], "total": v[1]} for k, v in sorted(per_intent.items())},
            "per_difficulty": {k: round(v[0] / v[1], 4) for k, v in sorted(per_diff.items())},
            "action_accuracy": round(sum(1 for d in action_total if d["action_correct"]) / len(action_total), 4)
            if action_total else None,
            "action_sample": len(action_total),
            "confidence_calibration": {
                b: {"sample": v[1], "actual_accuracy": round(v[0] / v[1], 4)}
                for b, v in sorted(bins.items())
            },
            "avg_latency_ms": round(sum(d["latency_ms"] for d in route) / n),
        }

    for grp in ("tool", "answer"):
        rows = [d for d in details if d["group"] == grp]
        if not rows:
            continue
        ran = [d for d in rows if d["status"] != "skipped"]
        skipped = [d for d in rows if d["status"] == "skipped"]
        if grp == "tool":
            hits = [d for d in ran if d.get("hit")]
            misuses = [d for d in ran if d.get("misused_tools")]
            summary["groups"]["tool"] = {
                "total": len(rows), "ran": len(ran), "skipped": len(skipped),
                "hit_rate": round(len(hits) / len(ran), 4) if ran else None,
                "misuse_cases": len(misuses),
                "skip_reasons": [{"case_id": d["case_id"], "reason": d["reason"]} for d in skipped],
                "avg_latency_ms": round(sum(d["latency_ms"] for d in ran) / len(ran)) if ran else None,
            }
        else:
            passed = [d for d in ran if d["status"] == "pass"]
            violations = [d for d in ran if d.get("forbidden_hits")]
            cov = [d["coverage"] for d in ran]
            summary["groups"]["answer"] = {
                "total": len(rows), "ran": len(ran), "skipped": len(skipped),
                "pass_rate": round(len(passed) / len(ran), 4) if ran else None,
                "avg_coverage": round(sum(cov) / len(cov), 4) if cov else None,
                "forbidden_violation_cases": len(violations),
                "global_forbidden": global_forbidden,
                "avg_latency_ms": round(sum(d["latency_ms"] for d in ran) / len(ran)) if ran else None,
            }

        # 意图片段命中统计（--intent-source route 时；复刻生产透传链路）
        routed = [d for d in ran if d.get("routed_intent")]
        if routed:
            specialized = [d for d in routed if d.get("prompt_category") != "general"]
            cat_counter = {}
            for d in routed:
                cat_counter[d["prompt_category"]] = cat_counter.get(d["prompt_category"], 0) + 1
            summary["groups"][grp]["intent_passthrough"] = {
                "routed": len(routed),
                "specialized": len(specialized),
                "specialized_rate": round(len(specialized) / len(routed), 4),
                "category_distribution": dict(sorted(cat_counter.items())),
            }
    return summary


def print_summary(summary: dict):
    print("\n" + "=" * 70)
    print("  📊 基线汇总")
    print("=" * 70)
    g = summary["groups"]
    if "route" in g:
        r = g["route"]
        print(f"\n[route] 意图准确率: {r['intent_accuracy']:.1%} ({r['total']} 条)  "
              f"分流正确率: {r['action_accuracy']:.1%} ({r['action_sample']} 条有期望动作)")
        print("  分意图:")
        for k, v in r["per_intent_counts"].items():
            print(f"    {k:<14} {v['correct']}/{v['total']}  → {r['per_intent'][k]:.0%}")
        print("  分难度: " + "  ".join(f"{k}={v:.0%}" for k, v in r["per_difficulty"].items()))
        print("  置信度校准（自报分数段 → 该段实际准确率）:")
        for b, v in r["confidence_calibration"].items():
            print(f"    {b}: n={v['sample']:<3} 实际={v['actual_accuracy']:.0%}")
    if "tool" in g:
        t = g["tool"]
        hit_str = "-" if t["hit_rate"] is None else f"{t['hit_rate']:.1%}"
        print(f"\n[tool] 命中率: {hit_str}  "
              f"执行 {t['ran']} 条 / skip {t['skipped']} 条 / 误用 {t['misuse_cases']} 条")
        ip = t.get("intent_passthrough")
        if ip:
            print(f"  意图透传: 特化 {ip['specialized']}/{ip['routed']} "
                  f"({ip['specialized_rate']:.0%}) 分布={ip['category_distribution']}")
    if "answer" in g:
        a = g["answer"]
        pass_str = "-" if a["pass_rate"] is None else f"{a['pass_rate']:.1%}"
        cov_str = "-" if a["avg_coverage"] is None else f"{a['avg_coverage']:.0%}"
        print(f"\n[answer] 通过率: {pass_str}  "
              f"平均要点覆盖: {cov_str}  "
              f"执行 {a['ran']} 条 / 禁词违规 {a['forbidden_violation_cases']} 条")
        ip = a.get("intent_passthrough")
        if ip:
            print(f"  意图透传: 特化 {ip['specialized']}/{ip['routed']} "
                  f"({ip['specialized_rate']:.0%}) 分布={ip['category_distribution']}")
    print("=" * 70)


RAW_CASES = []


async def main_async(request_groups: set, intent_source: str = "none"):
    global RAW_CASES
    with open(CASES_PATH, "r", encoding="utf-8") as f:
        data = json.load(f)
    RAW_CASES = data["cases"]
    global_forbidden = data.get("global_forbidden", [])
    print(f"加载用例 {len(RAW_CASES)} 条: "
          f"{sum(1 for c in RAW_CASES if c['group']=='route')} route / "
          f"{sum(1 for c in RAW_CASES if c['group']=='tool')} tool / "
          f"{sum(1 for c in RAW_CASES if c['group']=='answer')} answer")

    details = []
    if "route" in request_groups:
        details.extend(await asyncio.to_thread(run_route_group, [c for c in RAW_CASES if c["group"] == "route"]))
    if request_groups & {"tool", "answer"}:
        details.extend(await run_chat_groups(
            RAW_CASES, request_groups & {"tool", "answer"}, intent_source=intent_source
        ))

    summary = summarize(details, global_forbidden)
    print_summary(summary)

    os.makedirs(RESULTS_DIR, exist_ok=True)
    stamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    out_path = os.path.join(RESULTS_DIR, f"prompt_eval_report_{stamp}.json")
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump({"summary": summary, "details": details}, f, ensure_ascii=False, indent=2)
    print(f"\n报告已保存: {out_path}")
    return out_path


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--groups", default="route,tool,answer",
                        help="跑分分组，逗号分隔：route / tool / answer（默认全跑）")
    parser.add_argument("--intent-source", default="none", choices=["none", "route"],
                        help="tool/answer 组的意图来源：none=不透传（同 Phase 0/1 基线）；"
                             "route=先调 /api/route 再透传（复刻生产链路，统计片段命中率）")
    args = parser.parse_args()
    groups = {g.strip() for g in args.groups.split(",") if g.strip()}
    asyncio.run(main_async(groups, intent_source=args.intent_source))


if __name__ == "__main__":
    main()
