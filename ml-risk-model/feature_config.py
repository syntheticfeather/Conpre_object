"""
风控模型 — 完整特征配置 (v2.0)

定义了模型需要的所有特征及其处理规则。
同时服务于: Python 训练脚本 和 Java MlTrainingLogServiceImpl 采集逻辑。

特征来源:
  [GMS]    = Give Me Some Credit 数据集提供（9个）
  [Java]   = Java DB 可直接计算（48个）
  [Derived]= 训练时衍生（4个）
"""
from dataclasses import dataclass
from typing import List, Dict, Any, Optional


@dataclass
class FeatureDef:
    name: str
    display_name: str
    dtype: str              # numeric / category
    source: str             # identity / income / credit / asset / behavior / application / derived
    default_value: Any
    description: str
    availability: str = ""  # GMS / Java / Both


# ═══════════════════════════════════════════════════════
#  特征定义 — 6 组共 54 个
# ═══════════════════════════════════════════════════════

FEATURES: List[FeatureDef] = [

    # ── A. 身份特征 (7个) ──
    FeatureDef("age", "年龄", "numeric", "identity", 30,
               "从身份证提取出生日期计算", "Both"),
    FeatureDef("gender", "性别", "category", "identity", 0,
               "0=女 1=男，身份证第17位", "Java"),
    FeatureDef("region_code", "地区编码", "category", "identity", "00",
               "身份证前2位省份码", "Java"),
    FeatureDef("education_level", "学历等级", "numeric", "identity", 3,
               "1=初中 2=高中 3=大专 4=本科 5=硕士+（暂缺，需加字段）", "None"),
    FeatureDef("work_years", "工作年限", "numeric", "identity", 2,
               "在当前单位工作年数（暂缺，需加字段）", "None"),
    FeatureDef("has_bank_card", "是否绑卡", "category", "identity", 0,
               "user_certification.bank_card_id 是否非空", "Java"),
    FeatureDef("info_completeness", "信息完整度(%)", "numeric", "identity", 80,
               "已上传证书数/总证书类型数×100", "Both"),

    # ── B. 收入/工作特征 (5个) ──
    FeatureDef("monthly_income", "月收入(元)", "numeric", "income", 5000,
               "税后月收入（暂缺，需加字段）", "GMS"),
    FeatureDef("has_employment_cert", "是否有在职证明", "category", "income", 0,
               "work_cert.employment_cert_path 是否非空", "Java"),
    FeatureDef("has_salary_cert", "是否有收入证明", "category", "income", 0,
               "work_cert.salary_cert_path 是否非空", "Java"),
    FeatureDef("has_social_security", "是否有社保证明", "category", "income", 0,
               "tri_cert.social_security_path 是否非空", "Java"),
    FeatureDef("has_credit_report", "是否有征信报告", "category", "income", 0,
               "tri_cert.credit_report_path 是否非空", "Java"),

    # ── C. 信用/逾期特征 (14个) — 最核心 ──
    FeatureDef("credit_score", "平台信用分", "numeric", "credit", 0,
               "user_certification.credit_score（0-100）", "Java"),
    FeatureDef("is_blacklisted", "是否黑名单", "category", "credit", 0,
               "black_list 表是否有活跃记录", "Java"),
    FeatureDef("black_level", "黑名单等级", "numeric", "credit", 0,
               "black_list.black_level", "Java"),
    # -- orders 聚合 --
    FeatureDef("active_loan_count", "当前未结清贷款数", "numeric", "credit", 0,
               "orders COUNT where status != '已完成'", "Both"),
    FeatureDef("total_loan_count", "历史总贷款数", "numeric", "credit", 0,
               "orders COUNT all", "Java"),
    FeatureDef("max_overdue_days", "最大逾期天数", "numeric", "credit", 0,
               "orders MAX(overdue_days)", "Both"),
    FeatureDef("total_overdue_days", "累计逾期天数", "numeric", "credit", 0,
               "orders SUM(overdue_days)", "Java"),
    FeatureDef("overdue_order_count", "逾期订单数", "numeric", "credit", 0,
               "orders COUNT where status='已逾期'", "Java"),
    FeatureDef("credit_card_usage_pct", "信用卡使用率(%)", "numeric", "credit", 30,
               "已用/总额度×100（暂缺，需外部数据）", "GMS"),
    FeatureDef("credit_inquiries_3m", "近3月征信查询次数", "numeric", "credit", 1,
               "机构查询记录数（暂缺，需外部数据）", "None"),
    # -- repayment_schedule 聚合 --
    FeatureDef("on_time_payment_count", "按期还款次数", "numeric", "credit", 0,
               "repayment_schedule COUNT 按时还款", "Java"),
    FeatureDef("late_payment_count", "延期还款次数", "numeric", "credit", 0,
               "repayment_schedule COUNT 实际>应还日", "Java"),
    FeatureDef("overdue_schedule_count", "逾期未还期数", "numeric", "credit", 0,
               "repayment_schedule COUNT status='逾期'", "Java"),
    FeatureDef("total_schedule_count", "总还款期数", "numeric", "credit", 0,
               "repayment_schedule COUNT all", "Java"),

    # ── D. 资产特征 (7个) ──
    FeatureDef("has_house", "是否有房产", "category", "asset", 0,
               "immovables_cert.property_cert_path 是否非空", "Java"),
    FeatureDef("has_car", "是否有车产", "category", "asset", 0,
               "immovables_cert.car_cert_path 是否非空", "Java"),
    FeatureDef("total_asset_value", "总资产值(万)", "numeric", "asset", 0,
               "immovables_cert.total_value", "Java"),
    FeatureDef("has_mortgage", "是否有房贷", "category", "asset", 0,
               "有房且 total_value>0 视为有房贷", "Both"),
    FeatureDef("max_loan_amount_single", "单笔最大贷款额", "numeric", "asset", 0,
               "orders MAX(loan_amount) — 历史最大授信", "Java"),
    FeatureDef("total_loan_amount", "累计借贷总额", "numeric", "asset", 0,
               "orders SUM(loan_amount)", "Java"),
    FeatureDef("total_repaid_amount", "累计已还总额", "numeric", "asset", 0,
               "orders SUM(repaid_amount)", "Java"),

    # ── E. 行为特征 (13个) ──
    FeatureDef("dti_ratio", "负债收入比(%)", "numeric", "behavior", 30,
               "月度负债/月收入×100", "Both"),
    FeatureDef("repayment_ratio", "还款完成比", "numeric", "behavior", 0,
               "SUM(repaid)/SUM(loan) — 越接近1越好", "Java"),
    FeatureDef("on_time_payment_ratio", "按期还款率", "numeric", "behavior", 0,
               "按期次数/总期数", "Java"),
    FeatureDef("overdue_schedule_ratio", "逾期期数占比", "numeric", "behavior", 0,
               "逾期期数/总期数", "Java"),
    FeatureDef("avg_interest_rate", "历史平均利率", "numeric", "behavior", 0,
               "orders AVG(interest_rate)", "Java"),
    FeatureDef("completed_order_count", "已完成订单数", "numeric", "behavior", 0,
               "orders COUNT status='已完成'", "Java"),
    FeatureDef("avg_days_past_due", "平均逾期天数", "numeric", "behavior", 0,
               "AVG(actual-due) for late payments", "Java"),
    FeatureDef("max_days_past_due", "最大逾期天数(计划)", "numeric", "behavior", 0,
               "MAX(actual-due) for late payments", "Java"),
    FeatureDef("has_postpone_history", "是否有展期史", "category", "behavior", 0,
               "postpone_request 表是否有记录", "Java"),
    FeatureDef("postpone_count", "展期申请次数", "numeric", "behavior", 0,
               "postpone_request COUNT", "Java"),
    FeatureDef("approved_postpone_count", "展期通过次数", "numeric", "behavior", 0,
               "postpone_request COUNT status='已通过'", "Java"),
    FeatureDef("last_application_days_ago", "距上次申请天数", "numeric", "behavior", 0,
               "当前日期 - 上次申请日期", "Java"),
    FeatureDef("avg_review_time_minutes", "平均审核时长(分)", "numeric", "behavior", 0,
               "AVG(review_time - apply_time)", "Java"),

    # ── F. 本次申请特征 (8个) ──
    FeatureDef("applied_amount", "申请金额", "numeric", "application", 0,
               "本次申请的 loan_amount", "Java"),
    FeatureDef("applied_loan_period", "申请期限(年)", "numeric", "application", 0,
               "本次申请的 loan_period", "Java"),
    FeatureDef("applied_term", "申请期数(月)", "numeric", "application", 0,
               "本次申请的 term", "Java"),
    FeatureDef("product_type", "产品类型", "category", "application", 0,
               "本次申请的 product_id", "Java"),
    FeatureDef("repaid_type", "还款方式", "category", "application", "unknown",
               "等额本息/等额本金/先息后本/一次性还本付息", "Java"),
    FeatureDef("application_hour", "申请时间(小时)", "numeric", "application", 12,
               "apply_time 的小时数", "Both"),
    FeatureDef("is_late_night_apply", "是否凌晨申请", "category", "application", 0,
               "application_hour 在 0-5 之间", "Java"),
    FeatureDef("total_application_count", "累计申请次数", "numeric", "application", 0,
               "loan_applications COUNT", "Java"),
    FeatureDef("rejected_application_count", "被拒次数", "numeric", "application", 0,
               "应用 COUNT status=拒绝", "Java"),
    FeatureDef("rejection_rate", "被拒率", "numeric", "application", 0,
               "拒绝次数/总申请次数", "Java"),
    FeatureDef("approved_application_count", "通过次数", "numeric", "application", 0,
               "应用 COUNT status=通过", "Java"),
    FeatureDef("avg_application_amount", "历史平均申请金额", "numeric", "application", 0,
               "AVG(loan_amount) across applications", "Java"),
]

# ═══════════════════════════════════════════════════════
#  便捷变量
# ═══════════════════════════════════════════════════════

FEATURE_NAMES: List[str] = [f.name for f in FEATURES]

CATEGORICAL_FEATURES: List[str] = [f.name for f in FEATURES if f.dtype == "category"]

NUMERIC_FEATURES: List[str] = [f.name for f in FEATURES if f.dtype == "numeric"]

# GMS 数据集可提供的特征（用于 train_real.py 对齐）
GMS_FEATURES: List[str] = [f.name for f in FEATURES if f.availability in ("GMS", "Both")]

# Java DB 可提供的特征（用于 MlTrainingLogServiceImpl 采集）
JAVA_FEATURES: List[str] = [f.name for f in FEATURES if f.availability in ("Java", "Both")]

# ═══════════════════════════════════════════════════════
#  Java 推理时的 JSON 输入 Schema
# ═══════════════════════════════════════════════════════

def get_java_input_schema() -> Dict[str, Any]:
    """生成 Java 端可用的输入特征 Schema"""
    properties = {}
    for f in FEATURES:
        if f.availability == "None":
            continue  # 暂不可用的跳过
        if f.dtype == "numeric":
            properties[f.name] = {
                "type": "number",
                "default": f.default_value,
                "description": f.display_name,
                "source": f.source,
            }
        else:
            properties[f.name] = {
                "type": "integer",
                "enum": [0, 1],
                "default": f.default_value,
                "description": f.display_name,
                "source": f.source,
            }

    return {
        "schema_version": "2.0",
        "total_defined": len(FEATURES),
        "available": len(properties),
        "gms_count": len(GMS_FEATURES),
        "java_count": len(JAVA_FEATURES),
        "features": properties,
    }


if __name__ == "__main__":
    import json
    print(json.dumps(get_java_input_schema(), ensure_ascii=False, indent=2))
