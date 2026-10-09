# smart-customer-service/agent/plan_execute_agent.py
"""Plan-and-Execute Agent — 先拆步骤，逐步执行，最终汇总"""

import json, os, logging
from typing import AsyncGenerator, List
from datetime import datetime

from dotenv import load_dotenv
from openai import OpenAI

from agent.base_agent import BaseAgent
from tools.tool_manager import tool_manager

load_dotenv()
logger = logging.getLogger(__name__)


class PlanExecuteAgent(BaseAgent):
    """包装任意 BaseAgent，复杂问题先规划再执行。"""

    MAX_STEPS = 6

    def __init__(self, inner: BaseAgent):
        self.inner = inner

    async def chat(
        self, message: str, session_id: str, user_id=None, token=None,
        intent: str = None, confidence: float = 1.0,
    ) -> AsyncGenerator[str, None]:
        # 既然被包装了 PlanExecuteAgent，就始终走规划流程
        # 复杂度判断由上层负责（Java Router + ChatAgent 工厂选 Agent 时已决定）

        # 1. 规划（注入当前可用工具清单，避免拆出无工具支撑的空中楼阁步骤）
        plan = self._create_plan(message, self._render_available_tools())
        if not plan:
            async for chunk in self.inner.chat(
                message, session_id, user_id, token, intent=intent, confidence=confidence
            ):
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
            async for chunk in self.inner.chat(
                step_msg, session_id, user_id, token, intent=intent, confidence=confidence
            ):
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
        async for chunk in self.inner.chat(
            synt_msg, session_id, user_id, token, intent=intent, confidence=confidence
        ):
            yield chunk

    # ========== 辅助 ==========

    @staticmethod
    def _render_available_tools() -> str:
        """渲染当前可用工具清单（名称 + 一句话能力），供规划器约束步骤落点。"""
        tools = tool_manager.get_all_tools()
        if not tools:
            return "（当前无可用工具，步骤只能基于已有信息直接作答）"
        lines = []
        for t in tools:
            # description 四要素较长，规划器只需"做什么"——取首行
            first_line = (t.description or "").strip().split("\n", 1)[0].strip()
            lines.append(f"- `{t.name}`：{first_line}")
        return "\n".join(lines)

    def _create_plan(self, message: str, tools_description: str) -> List[str]:
        """LLM 拆解步骤（输入侧增强：工具清单 + 步骤格式约束；输出协议保持按行文本不变）"""
        try:
            client = OpenAI(
                api_key=os.getenv("LLM_API_KEY"),
                base_url=os.getenv("LLM_BASE_URL", "https://aihubmix.com").rstrip("/") + "/v1",
            )
            prompt = f"""将以下用户问题拆解为 {self.MAX_STEPS} 步以内的执行计划。

可用工具（每一步必须能由这些工具完成，或基于对话中已有的信息直接作答）:
{tools_description}

步骤要求:
- 每步一行，不要编号，不要多余解释
- 每步以动词开头，并明确落到上面某个具体工具（如"用 search_knowledge 检索提前还款违约金规则"）或明确的信息来源
- 禁止规划没有工具支撑的步骤（如"查询用户征信""联系风控部门"）
- 无需工具即可回答的问题只输出一步

问题: {message}"""
            # 隐式推理链（reasoning_content）占输出预算，预算不足时
            # finish_reason=length 且 content 为空或只吐半截——2000 首调，空结果加量重试
            response = client.chat.completions.create(
                model="deepseek-v4-flash",
                messages=[{"role": "user", "content": prompt}],
                temperature=0.0, max_tokens=2000,
            )
            choice = response.choices[0]
            content = (choice.message.content or "").strip()
            if not content or (choice.finish_reason == "length" and content.count("\n") == 0):
                logger.warning("规划返回为空或被截断，加量 3000 重试一次")
                response = client.chat.completions.create(
                    model="deepseek-v4-flash",
                    messages=[{"role": "user", "content": prompt}],
                    temperature=0.0, max_tokens=3000,
                )
                content = (response.choices[0].message.content or "").strip()
            lines = [l.strip("- 123456789. ") for l in content.split("\n")]
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
