# smart-customer-service/api/route_routes.py
"""
路由端点：LLM 意图分类（自报置信度）。

调用方式:
  POST /api/route  {"message": "帮我查一下申请进度"}
  → {"intent": "QUERY_STATUS", "confidence": 0.95, "action": "WORKFLOW"}
"""
import os, json, logging
from fastapi import APIRouter
from openai import OpenAI
from dotenv import load_dotenv
from api.models import RouteRequest, RouteResult, ExtractLoanParamsRequest, LoanParams

load_dotenv()
logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api", tags=["路由"])

# ========== 意图定义 ==========

INTENTS = {
    "QUERY_STATUS":    "查询贷款申请进度、审批状态、处理结果",
    "CALCULATE":       "计算月供、还款金额、利息、还款计划",
    "LIST_PRODUCTS":   "查看有哪些贷款产品、产品详情、利率说明",
    "APPLY_LOAN":      "申请贷款、提交贷款申请、我要贷款",
    "COMPLAINT":       "投诉、不满意、有问题要反馈、要投诉",
    "CONSULT":         "咨询、推荐、对比、评估、建议、哪个好、能不能贷",
}

# 这些意图不管置信度多少都走 Agent
AGENT_ONLY_INTENTS = {"COMPLAINT", "CONSULT"}

# 置信度阈值：低于此值降级到 Agent
CONFIDENCE_THRESHOLD = 0.8

# ========== LLM 客户端 ==========

_llm_client = None


def _get_llm():
    global _llm_client
    if _llm_client is None:
        api_key = os.getenv("LLM_API_KEY")
        base_url = os.getenv("LLM_BASE_URL", "https://aihubmix.com")
        if not base_url.endswith("/v1"):
            base_url = base_url.rstrip("/") + "/v1"
        _llm_client = OpenAI(api_key=api_key, base_url=base_url)
    return _llm_client


# ========== 路由端点 ==========


@router.post("/route")
async def classify_route(request: RouteRequest):
    """对用户消息做意图分类，返回 intent + confidence + action。"""
    message = request.message

    intents_desc = "\n".join(f"  {k}: {v}" for k, v in INTENTS.items())
    prompt = f"""你是一个路由分类器。将用户消息分类到以下意图之一，输出 JSON。

意图定义:
{intents_desc}

规则:
- 如果用户消息和多个意图相关，选最匹配的那个
- 如果都不匹配，用 CONSULT
- confidence 是你对分类的确定程度(0~1)，非常确定给0.9+，不太确定给0.5-0.7

输出格式: {{"intent": "QUERY_STATUS", "confidence": 0.95}}

用户消息: {message}"""

    try:
        client = _get_llm()
        response = client.chat.completions.create(
            model="gpt-4.1-mini-free",
            messages=[{"role": "user", "content": prompt}],
            temperature=0.0,
            max_tokens=100,
            response_format={"type": "json_object"},
        )

        intent, confidence = _parse_response(response.choices[0].message.content.strip())

        action = _decide_action(intent, confidence)

        logger.info(f"路由: '{message[:40]}...' → {intent} (c={confidence:.2f}, {action})")

        return RouteResult(intent=intent, confidence=round(confidence, 4), action=action)

    except Exception as e:
        logger.warning(f"路由分类失败，降级到 AGENT: {e}")
        return RouteResult(intent="UNKNOWN", confidence=0.0, action="AGENT")


# ========== 辅助函数 ==========


def _parse_response(content: str) -> tuple:
    """从 LLM 响应中解析 intent 和 confidence"""
    try:
        obj = json.loads(content)
        intent = obj.get("intent", "").upper().strip()
        confidence = float(obj.get("confidence", 0.5))

        # 精确匹配
        if intent in INTENTS:
            return intent, _clamp(confidence)

        # 模糊匹配
        for key in INTENTS:
            if key in intent or intent in key:
                return key, _clamp(confidence)

        return "CONSULT", _clamp(confidence)
    except (json.JSONDecodeError, AttributeError, ValueError):
        pass
    return "CONSULT", 0.5


def _clamp(val: float) -> float:
    return max(0.0, min(1.0, val))


def _decide_action(intent: str, confidence: float) -> str:
    if intent in AGENT_ONLY_INTENTS:
        return "AGENT"
    if confidence < CONFIDENCE_THRESHOLD:
        return "AGENT"
    return "WORKFLOW"


# ========== 贷款参数提取端点 ==========


@router.post("/extract-loan-params", response_model=LoanParams)
async def extract_loan_params(request: ExtractLoanParamsRequest):
    """
    从用户自然语言消息中提取结构化贷款参数。用于 Workflow 中替代正则。
    一次轻量 LLM 调用，temperature=0。
    """
    message = request.message
    products = request.available_products or []
    products_hint = ""
    if products:
        products_hint = f"\n可用产品列表: {', '.join(products)}"

    prompt = f"""从用户消息中提取贷款参数。未提供的填 null。

{products_hint}

输出 JSON:
{{"product_name": "产品名或null", "amount": 数字(元)或null, "months": 数字(月)或null, "repaid_type": "等额本息/等额本金/先息后本/一次性还本付息或null", "intent": "apply/calculate/modify/cancel/unknown"}}

规则:
- 单位转换: "50万"=500000, "3年"=36个月, "利率4.5%"→金额已包含百分号
- intent: 要提交申请→apply, 只是问月供→calculate, 要改参数→modify, 不申请了→cancel
- 只输出 JSON，不要解释

用户消息: {message}"""

    try:
        client = _get_llm()
        response = client.chat.completions.create(
            model="gpt-4.1-mini-free",
            messages=[{"role": "user", "content": prompt}],
            temperature=0.0,
            max_tokens=200,
            response_format={"type": "json_object"},
        )
        content = response.choices[0].message.content.strip()
        data = json.loads(content)
        return LoanParams(
            product_name=data.get("product_name"),
            amount=data.get("amount"),
            months=data.get("months"),
            repaid_type=data.get("repaid_type"),
            intent=data.get("intent"),
        )
    except Exception as e:
        logger.warning(f"参数提取失败: {e}")
        return LoanParams()  # 返回空，让 Workflow 走追问引导
