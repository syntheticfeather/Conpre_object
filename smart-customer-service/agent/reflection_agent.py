# smart-customer-service/agent/reflection_agent.py
"""Reflection Agent — 对关键回复做六维自我审查，仅高严重度问题触发修正（Phase 2）"""

import re
import json, os, logging
from typing import AsyncGenerator

from dotenv import load_dotenv
from openai import OpenAI

from agent.base_agent import BaseAgent
from agent.prompts import INTENT_PROMPT_THRESHOLD, get_refinement_constraints

load_dotenv()
logger = logging.getLogger(__name__)


class ReflectionAgent(BaseAgent):
    """包装任意 BaseAgent，对金融/推荐类回复做质量把关。"""

    SCORE_THRESHOLD = 0.7
    MAX_RETRIES = 2

    # 透传意图高置信命中即必反思（关键词仅在意图缺失/低置信时兜底）
    INTENT_TRIGGER = {"CALCULATE", "APPLY_LOAN", "CONSULT"}
    KEYWORD_TRIGGER = ["推荐", "评估", "计算", "还款", "利率", "建议", "方案", "月供"]

    # 六维审查（Phase 2：在原 4 维上新增引用忠实度、合规性）
    REVIEW_DIMENSIONS = """\
1. 数字准确性：月供、利息、利率、期限等数字是否正确（必须来自工具结果，不得心算）
2. 事实忠实：是否编造了产品、政策、数据
3. 引用忠实度：结论是否都有知识库检索结果或工具输出支撑，有无超出依据的发挥
4. 合规性：是否承诺放款结果/审批时效；利率是否为年化口径；推荐是否有"仅供参考"类表述
5. 逻辑完整：是否答其所问，有无关键信息遗漏
6. 表达恰当：措辞是否专业，有无绝对化、误导性表述"""

    def __init__(self, inner: BaseAgent):
        self.inner = inner

    async def chat(
        self, message: str, session_id: str, user_id=None, token=None,
        intent: str = None, confidence: float = 1.0,
    ) -> AsyncGenerator[str, None]:
        # 触发判断：透传意图优先（高置信），关键词兜底，增加intent, confidence2个参数
        if not self._needs_reflection(message, intent, confidence):
            async for chunk in self.inner.chat(
                message, session_id, user_id, token, intent=intent, confidence=confidence
            ):
                yield chunk
            return

        logger.info(
            f"Reflection 触发: intent={intent} confidence={confidence} msg='{message[:30]}'"
        )

        # 收集完整回复
        full = ""
        chunks = []
        async for chunk in self.inner.chat(
            message, session_id, user_id, token, intent=intent, confidence=confidence
        ):
            try:
                evt = json.loads(chunk)
                if evt.get("type") == "message":
                    full += evt.get("content", "")
                chunks.append(chunk)
            except json.JSONDecodeError:
                chunks.append(chunk)

        # 审查—修正循环：仅 high severity 问题触发 refine，low severity 放行
        refined = False
        for attempt in range(self.MAX_RETRIES):
            score, issues = self._review(message, full)
            high_issues = [i for i in issues if i.get("severity") == "high"]

            if score >= self.SCORE_THRESHOLD:
                logger.info(f"Reflection 审查通过: score={score} issues={len(issues)}")
                break
            if not high_issues:
                # 分数不高但只有低严重度问题 → 放行，避免过度反思拖延迟
                logger.info(f"Reflection 低严重度放行: score={score} issues={issues}")
                break

            logger.info(
                f"Reflection: 第{attempt+1}次审查不通过 (score={score}, "
                f"high={len(high_issues)})，修正中..."
            )
            full = self._refine(message, full, high_issues, intent, confidence)
            refined = True
            yield json.dumps({
                "type": "reflection",
                "attempt": attempt + 1,
                "score": score,
                "issues": high_issues,
            }, ensure_ascii=False)

        if not refined:
            # 未修正：原样透出内层全部事件（保持流式体感）
            for c in chunks:
                yield c
        else:
            # 修正过：先发修正后的完整文本，再补发工具调用等非 message 事件
            yield json.dumps({"type": "message", "content": full}, ensure_ascii=False)
            for c in chunks:
                try:
                    evt = json.loads(c)
                    if evt.get("type") != "message":
                        yield c
                except json.JSONDecodeError:
                    pass

        yield json.dumps({"event": "done", "data": "final"}, ensure_ascii=False)

    # ========== 辅助 ==========

    def _needs_reflection(self, message: str, intent: str = None, confidence: float = 1.0) -> bool:
        # 优先：透传意图明确且置信度达到意图门槛
        if (
            intent
            and confidence is not None
            and confidence >= INTENT_PROMPT_THRESHOLD
            and intent.upper() in self.INTENT_TRIGGER
        ):  
            return True
        # 兜底：意图缺失/低置信时沿用关键词（保持改造前覆盖）
        return any(k in message for k in self.KEYWORD_TRIGGER)

    def _review(self, message: str, response: str) -> tuple:
        """六维审查，返回 (score, issues[{dim,desc,severity}])；失败时放过。"""
        try:
            client = OpenAI(
                api_key=os.getenv("LLM_API_KEY"),
                base_url=os.getenv("LLM_BASE_URL", "https://aihubmix.com").rstrip("/") + "/v1",
            )
            prompt = f"""你是贷款风控审查员。按以下 6 个维度审查 AI 回复，打分 0.0~1.0。

审查维度:
{self.REVIEW_DIMENSIONS}

严重度判定:
- high：合规红线问题（承诺放款、利率口径错误等）、编造数据、关键数字错误、结论无工具/检索依据却以肯定语气给出
- low：不影响结论的信息补充、措辞或格式优化

用户问题: {message}
AI 回复: {response}

只输出 JSON，issues 必须是对象数组，严格遵守格式：
{{"score": 0.85, "issues": [{{"dim": "合规性", "desc": "问题描述", "severity": "high"}}]}}
无问题时 issues 输出空数组 []。"""

            # 所用模型带隐式推理链（reasoning_content 占输出预算），六维审查推理较长，
            # 预算不足时 finish_reason=length 且 content 为空——预算给足并带一次加量重试
            resp = client.chat.completions.create(
                model="deepseek-v4-flash",
                messages=[{"role": "user", "content": prompt}],
                temperature=0.0, max_tokens=2000,
                response_format={"type": "json_object"},
            )
            data = self._parse_review_json(resp.choices[0].message.content)
            if data is None:
                # 偶发空返回/推理超预算：加量到 3000 并去掉 json_object 重试一次
                logger.warning("Reflection review 解析失败，加量 3000 重试一次")
                resp = client.chat.completions.create(
                    model="deepseek-v4-flash",
                    messages=[{"role": "user", "content": prompt}],
                    temperature=0.0, max_tokens=3000,
                )
                data = self._parse_review_json(resp.choices[0].message.content)
            if data is None:
                return 1.0, []  # 仍失败 → 放过，不阻断对话

            score = float(data.get("score", 0.5))
            issues = self._normalize_issues(data.get("issues", []))
            return score, issues
        except Exception as e:
            logger.warning(f"Reflection review 失败: {e}")
            return 1.0, []  # 审查失败 → 放过

    @staticmethod
    def _parse_review_json(content):
        """容错解析审查结果：去 ```json 代码块包裹，截取首个 JSON 对象。"""
        if not content:
            return None
        text = content.strip()
        text = re.sub(r"^```(?:json)?|```$", "", text, flags=re.IGNORECASE).strip()
        try:
            return json.loads(text)
        except json.JSONDecodeError:
            match = re.search(r"\{.*\}", text, flags=re.DOTALL)
            if match:
                try:
                    return json.loads(match.group(0))
                except json.JSONDecodeError:
                    return None
        return None

    @staticmethod
    def _normalize_issues(raw_issues) -> list:
        """issues 归一化：接受 [{dim,desc,severity}]，也容错模型常返回的字符串数组。"""
        # 字符串问题没有 severity：高分低改原则下，按关键词判定，默认 high
        # （能被打出低分又列出的问题通常是实质问题；最终仍受 score<0.7 双闸门约束）
        low_keywords = ("补充", "建议", "措辞", "格式", "优化", "更完整", "更清晰")

        def _severity_of(text: str) -> str:
            return "low" if any(k in text for k in low_keywords) else "high"

        issues = []
        if isinstance(raw_issues, list):
            for item in raw_issues:
                if isinstance(item, dict):
                    desc = str(item.get("desc", "")).strip()
                    if not desc:
                        continue
                    severity = str(item.get("severity", "")).strip().lower()
                    if severity not in ("high", "low"):
                        severity = _severity_of(desc)
                    issues.append({
                        "dim": str(item.get("dim", "")).strip() or "其他",
                        "desc": desc,
                        "severity": severity,
                    })
                elif isinstance(item, str) and item.strip() and item.strip() != "无":
                    desc = item.strip()
                    issues.append({"dim": "其他", "desc": desc, "severity": _severity_of(desc)})
        elif isinstance(raw_issues, str) and raw_issues.strip() and raw_issues.strip() != "无":
            desc = raw_issues.strip()
            issues.append({"dim": "其他", "desc": desc, "severity": _severity_of(desc)})
        return issues

    def _refine(self, message: str, original: str, high_issues: list,
                intent: str = None, confidence: float = 1.0) -> str:
        """带 L0 红线 + 当前意图片段约束修正，修正不得引入新违规。"""
        try:
            client = OpenAI(
                api_key=os.getenv("LLM_API_KEY"),
                base_url=os.getenv("LLM_BASE_URL", "https://aihubmix.com").rstrip("/") + "/v1",
            )
            constraints = get_refinement_constraints(intent, confidence)
            issue_lines = "\n".join(f"- [{i['dim']}] {i['desc']}" for i in high_issues)
            prompt = f"""你在修正一条贷款客服回复。只修正下列高严重度问题，保留其余正确内容，输出修正后的完整回复。

{constraints}

修正要求:
- 不得违反上述安全合规红线与业务规则，修正本身不得引入新的违规
- 数字类内容若无工具结果支撑，删除该数字或改为引导用户使用页面测算工具，严禁自行估算
- 缺乏依据的结论改为有依据的表述，或明确告知暂无相关信息
- 不要输出审查过程、标注或解释，只输出给用户看的回复正文

用户问题: {message}
原始回复:
{original}

需修正的高严重度问题:
{issue_lines}"""

            # max_tokens=2000：隐式推理链 + 完整修正回复的总预算
            resp = client.chat.completions.create(
                model="deepseek-v4-flash",
                messages=[{"role": "user", "content": prompt}],
                temperature=0.3, max_tokens=2000,
            )
            return resp.choices[0].message.content.strip() or original
        except Exception as e:
            logger.warning(f"Reflection refine 失败: {e}")
            return original
