# smart-customer-service/agent/plan_execute_agent.py
"""Plan-and-Execute Agent — 先拆步骤，逐步执行，最终汇总"""

import json, os, logging
from typing import AsyncGenerator, List
from datetime import datetime

from dotenv import load_dotenv
from openai import OpenAI

from agent.base_agent import BaseAgent

load_dotenv()
logger = logging.getLogger(__name__)


class PlanExecuteAgent(BaseAgent):
    """包装任意 BaseAgent，复杂问题先规划再执行。"""

    MAX_STEPS = 6

    def __init__(self, inner: BaseAgent):
        self.inner = inner

    async def chat(
        self, message: str, session_id: str, user_id=None, token=None
    ) -> AsyncGenerator[str, None]:
        # 既然被包装了 PlanExecuteAgent，就始终走规划流程
        # 复杂度判断由上层负责（Java Router + ChatAgent 工厂选 Agent 时已决定）

        # 1. 规划
        plan = self._create_plan(message)
        if not plan:
            async for chunk in self.inner.chat(message, session_id, user_id, token):
                yield chunk
            return

        yield json.dumps({
            "type": "plan", "steps": plan,
        }, ensure_ascii=False)

        # 3. 逐步执行
        context = ""
        for i, step in enumerate(plan):
            step_msg = self._step_message(message, plan, context, step, i + 1, len(plan))
            step_result = ""
            async for chunk in self.inner.chat(step_msg, session_id, user_id, token):
                try:
                    evt = json.loads(chunk)
                    if evt.get("type") == "message":
                        step_result += evt.get("content", "")
                    elif evt.get("event") == "done":
                        pass
                    else:
                        yield chunk
                except json.JSONDecodeError:
                    pass

            context += f"\n## 步骤{i+1}完成: {step}\n结果: {step_result[:500]}"
            yield json.dumps({
                "type": "step_done", "step": i + 1, "total": len(plan), "result": step_result[:200],
            }, ensure_ascii=False)

        # 4. 汇总
        synt_msg = f"""原始问题: {message}

执行过程:
{context}

请将所有步骤的结果整合成一个完整、连贯的答案回复用户。"""
        async for chunk in self.inner.chat(synt_msg, session_id, user_id, token):
            yield chunk

    # ========== 辅助 ==========

    def _create_plan(self, message: str) -> List[str]:
        """LLM 拆解步骤"""
        try:
            client = OpenAI(
                api_key=os.getenv("LLM_API_KEY"),
                base_url=os.getenv("LLM_BASE_URL", "https://aihubmix.com").rstrip("/") + "/v1",
            )
            response = client.chat.completions.create(
                model="gpt-4.1-mini-free",
                messages=[{"role": "user", "content": f"""将以下用户问题拆解为 {self.MAX_STEPS} 步以内的执行计划。
每步一行，不要编号，不要多余解释。

问题: {message}"""}],
                temperature=0.0, max_tokens=300,
            )
            lines = [l.strip("- 123456789. ") for l in response.choices[0].message.content.strip().split("\n")]
            return [l for l in lines if l and len(l) > 3][:self.MAX_STEPS]
        except Exception as e:
            logger.warning(f"规划失败: {e}")
            return []

    def _step_message(self, original: str, plan: List[str], context: str, step: str, idx: int, total: int) -> str:
        return f"""你在执行一个多步骤计划来回答用户问题。

原始问题: {original}
完整计划: {plan}
已完成步骤的结果:
{context}

当前是第 {idx}/{total} 步，请你完成这一步: {step}
只输出这一步的结果，不需要输出整个问题的最终答案。"""
