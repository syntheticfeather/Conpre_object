"""
风控模型 — 完整训练流水线

用法:
    cd ml-risk-model
    PYTHONIOENCODING=utf-8 python train.py

流程:
    1. 生成合成数据（模拟真实信贷数据分布）
    2. 特征工程（缺失值/分箱/编码）
    3. 训练 XGBoost 分类器
    4. 评估（AUC, KS, 混淆矩阵, 特征重要性）
    5. 导出模型（PKL + JSON 双格式）
"""
import os, sys, json, io, warnings
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.metrics import (
    roc_auc_score, f1_score, precision_score, recall_score,
    confusion_matrix, classification_report, roc_curve
)
from sklearn.preprocessing import StandardScaler
import xgboost as xgb

warnings.filterwarnings("ignore")

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8") \
    if hasattr(sys.stdout, "buffer") else sys.stdout

# 项目路径
MODEL_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "model")
os.makedirs(MODEL_DIR, exist_ok=True)

from feature_config import (
    FEATURES, FEATURE_NAMES, CATEGORICAL_FEATURES, NUMERIC_FEATURES
)

# ═══════════════════════════════════════════════════════
#  Step 1: 生成合成数据
# ═══════════════════════════════════════════════════════

def generate_synthetic_data(n_samples: int = 10000, random_seed: int = 42) -> pd.DataFrame:
    """
    生成模拟信贷数据集。

    数据分布设计参考：
      - 行业公开数据集 (LendingClub, Kaggle Give Me Some Credit)
      - 银保监会公布的消费贷违约率统计 (行业平均 ~3-8%)
      - 项目知识库 03_风控规则.md 中的审核标准

    违约率目标: ~7%（接近个人消费贷实际水平）
    """
    rng = np.random.default_rng(random_seed)
    n = n_samples

    # ===== 身份特征 =====
    age = rng.normal(35, 8, n).clip(22, 55).astype(int)
    edu_probs = [0.15, 0.30, 0.30, 0.20, 0.05]  # 初中/高中/大专/本科/硕士
    education_level = rng.choice([1, 2, 3, 4, 5], size=n, p=edu_probs)
    work_years = (age - 22 - (education_level - 1) * 2.5 + rng.normal(0, 2, n)).clip(0, 35)

    # ===== 收入特征 =====
    income_base = 3000 + education_level * 1500 + work_years * 300
    monthly_income = (income_base * np.exp(rng.normal(0, 0.4, n))).clip(3000, 80000).astype(int)
    has_social_security = rng.binomial(1, 0.6 + education_level * 0.08, n).clip(0, 1)
    has_fund = rng.binomial(1, 0.3 + education_level * 0.1, n).clip(0, 1)

    # ===== 信用特征 =====
    # 泊松分布建模逾期次数（大部分人 0 次）
    overdue_count = rng.poisson(0.8, n).clip(0, 10)
    # 逾期0次的，最大逾期天数=0；有逾期的，按比例设置天数
    max_overdue_days = np.where(
        overdue_count == 0, 0,
        rng.choice([30, 60, 90, 120], size=n, p=[0.4, 0.3, 0.2, 0.1])
    )
    max_overdue_days = np.where(overdue_count > 0, max_overdue_days, 0)

    credit_card_usage = rng.beta(2, 5, n) * 100  # 右偏分布，大部分 < 40%
    credit_inquiries = rng.poisson(2.5, n).clip(0, 15)
    multi_loan_count = rng.poisson(1.5, n).clip(0, 8)
    has_mortgage = rng.binomial(1, 0.2 + (age > 30) * 0.15, n).clip(0, 1)

    # ===== 行为特征 =====
    total_debt = (multi_loan_count * 2000 + overdue_count * 5000 +
                  (credit_card_usage / 100) * 15000)
    dti_ratio = (total_debt / (monthly_income + 1)) * 100
    dti_ratio = dti_ratio.clip(0, 100)

    # 凌晨申请 (0-6点) 与欺诈相关
    application_hour = np.where(
        rng.random(n) < 0.08,  # 8%的人在凌晨申请
        rng.integers(0, 6, n),
        rng.integers(8, 22, n)
    )
    info_completeness = rng.beta(7, 1.5, n) * 100

    # ===== 标签: 是否违约 =====
    # 根据特征计算违约概率（逻辑函数）
    # 目标违约率: ~7%（接近个人消费贷行业平均水平）
    logit = (
        -4.0  # 基础偏移（压低整体违约率至~8%）
        + (overdue_count >= 2) * 2.0      # 逾期≥2次 → 强高风险信号
        + (max_overdue_days >= 60) * 1.8  # 严重逾期 → 强高风险
        + (credit_card_usage > 70) * 1.2  # 信用卡刷爆
        + (credit_inquiries >= 6) * 1.0   # 频繁查征信
        + (multi_loan_count >= 3) * 0.8   # 多头借贷
        + (dti_ratio > 55) * 1.5          # 负债过高
        + (age < 25) * 0.6                # 太年轻
        + (monthly_income < 4000) * 0.8   # 收入太低
        - has_fund * 0.6                   # 有公积金 → 较大幅度降风险
        - has_mortgage * 0.4               # 有房贷 → 较稳定
        - (education_level >= 4) * 0.5     # 高学历
        - (info_completeness > 80) * 0.4   # 信息完整
        + (application_hour < 6) * 0.8     # 凌晨申请
        + rng.normal(0, 0.5, n)            # 随机噪声
    )
    prob_default = 1 / (1 + np.exp(-logit))  # sigmoid

    # 根据概率采样标签
    is_default = rng.binomial(1, prob_default, n)

    # ===== 添加缺失值（模拟真实数据）=====
    masks = {}
    masks["monthly_income"] = rng.random(n) < 0.03  # 3% 收入缺失
    masks["credit_card_usage_pct"] = rng.random(n) < 0.05
    masks["dti_ratio"] = rng.random(n) < 0.04
    masks["application_hour"] = rng.random(n) < 0.02

    # ===== 组装 DataFrame =====
    df = pd.DataFrame({
        "age": age,
        "education_level": education_level,
        "work_years": work_years.astype(int),
        "monthly_income": monthly_income,
        "has_social_security": has_social_security,
        "has_fund": has_fund,
        "overdue_count_2y": overdue_count,
        "max_overdue_days": max_overdue_days,
        "credit_card_usage_pct": credit_card_usage.round(1),
        "credit_inquiries_3m": credit_inquiries,
        "multi_loan_count": multi_loan_count,
        "has_mortgage": has_mortgage,
        "dti_ratio": dti_ratio.round(1),
        "application_hour": application_hour,
        "info_completeness": info_completeness.round(1),
        "is_default": is_default,  # 标签列
    })

    # 应用缺失
    for col, mask in masks.items():
        df.loc[mask, col] = np.nan

    return df


# ═══════════════════════════════════════════════════════
#  Step 2: 特征工程
# ═══════════════════════════════════════════════════════

def feature_engineering(df: pd.DataFrame, is_training: bool = True,
                        scaler: StandardScaler = None) -> tuple:
    """
    特征工程流水线:
      1. 缺失值填充
      2. 衍生特征
      3. 数值特征标准化
      4. 类别特征确保为整数

    Args:
        df: 原始 DataFrame
        is_training: True=训练模式(拟合scaler), False=推理模式(用已有scaler)
        scaler: 已训练的 StandardScaler（推理时传入）

    Returns:
        X: 特征矩阵 (numpy array)
        y: 标签 (numpy array, 仅训练模式)
        scaler: StandardScaler 实例
    """
    df = df.copy()

    # ── 缺失值填充 ──
    fill_values = {
        "monthly_income": -1,           # -1 表示"缺失"，让模型自己学
        "credit_card_usage_pct": 0,     # 没信用卡 → 使用率0
        "dti_ratio": -1,                # -1 表示缺失
        "application_hour": -1,         # -1 表示缺失
        "overdue_count_2y": 0,
        "max_overdue_days": 0,
        "credit_inquiries_3m": 0,
        "multi_loan_count": 0,
        "info_completeness": 50,        # 中位数
    }
    for col, val in fill_values.items():
        if col in df.columns:
            df[col] = df[col].fillna(val)

    # ── 衍生特征 ──
    # 收入缺失标志（"没填收入"本身是信号）
    df["income_missing"] = (df["monthly_income"] == -1).astype(int)

    # 凌晨申请标志
    df["late_night_apply"] = (
        (df["application_hour"] >= 0) & (df["application_hour"] < 6)
    ).astype(int)

    # 高负债标志
    df["high_dti"] = (df["dti_ratio"] > 55).astype(int)

    # 严重逾期标志
    df["severe_overdue"] = (df["max_overdue_days"] >= 60).astype(int)

    # ── 确定使用的特征列 ──
    used_features = FEATURE_NAMES + [
        "income_missing", "late_night_apply", "high_dti", "severe_overdue"
    ]
    # 只保留 df 中存在的列
    used_features = [c for c in used_features if c in df.columns]

    X = df[used_features].copy()

    # ── 标准化数值特征 ──
    numeric_cols = [c for c in NUMERIC_FEATURES if c in X.columns
                    and X[c].dtype in ["float64", "int64", "int32"]]

    if is_training:
        scaler = StandardScaler()
        X[numeric_cols] = scaler.fit_transform(X[numeric_cols])
    else:
        if scaler is None:
            raise ValueError("推理模式下必须传入已训练的 scaler")
        X[numeric_cols] = scaler.transform(X[numeric_cols])

    # ── 提取标签 ──
    y = df["is_default"].values if "is_default" in df.columns else None

    return X.values.astype(np.float32), y, scaler, used_features


# ═══════════════════════════════════════════════════════
#  Step 3: 训练 + 评估
# ═══════════════════════════════════════════════════════

def train_and_evaluate(X, y, feature_names, test_size=0.2, random_seed=42):
    """训练 XGBoost 模型并输出评估报告"""
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=test_size, random_state=random_seed, stratify=y
    )

    # XGBoost 参数
    params = {
        "objective": "binary:logistic",
        "eval_metric": "auc",
        "max_depth": 5,
        "learning_rate": 0.05,
        "n_estimators": 200,
        "subsample": 0.8,
        "colsample_bytree": 0.8,
        "min_child_weight": 5,
        "gamma": 0.1,
        "reg_alpha": 0.1,
        "reg_lambda": 1.0,
        "random_state": random_seed,
        "n_jobs": -1,
    }

    model = xgb.XGBClassifier(**params)
    model.fit(
        X_train, y_train,
        eval_set=[(X_test, y_test)],
        verbose=False,
    )

    # ── 预测 ──
    y_pred = model.predict(X_test)
    y_prob = model.predict_proba(X_test)[:, 1]

    # ── 指标 ──
    auc = roc_auc_score(y_test, y_prob)
    ks = _calc_ks(y_test, y_prob)
    precision = precision_score(y_test, y_pred)
    recall = recall_score(y_test, y_pred)
    f1 = f1_score(y_test, y_pred)
    cm = confusion_matrix(y_test, y_pred)
    tn, fp, fn, tp = cm.ravel()

    # ── 打印报告 ──
    print("\n" + "=" * 60)
    print("  📊 模型评估报告")
    print("=" * 60)
    print(f"  训练集大小: {len(X_train):,}")
    print(f"  测试集大小: {len(X_test):,}")
    print(f"  测试集违约率: {y_test.mean():.2%}")
    print()
    print(f"  AUC (区分能力):        {auc:.4f}  (0.5=随机, 0.7+可用, 0.8+好, 0.9+优秀)")
    print(f"  KS  (区分度):          {ks:.4f}  (0.2+可用, 0.3+好, 0.4+优秀)")
    print(f"  Precision (精准率):    {precision:.4f}  (预测会违约的人中，实际违约的比例)")
    print(f"  Recall (召回率):       {recall:.4f}  (实际违约的人中，被模型抓出来的比例)")
    print(f"  F1 Score:              {f1:.4f}")
    print()
    print(f"  混淆矩阵:")
    print(f"                    预测不违约    预测违约")
    print(f"    实际不违约:       {tn:>5}         {fp:>5}")
    print(f"    实际违约:         {fn:>5}         {tp:>5}")
    print()
    print(f"  解读:")
    print(f"    · {tn} 个好用户被正确放过")
    print(f"    · {fp} 个好用户被误拒 (False Positive) → 误拒率 {fp/(fp+tn):.2%}")
    print(f"    · {fn} 个坏用户被漏过 (False Negative) → 漏过率 {fn/(fn+tp):.2%}")
    print(f"    · {tp} 个坏用户被正确拦截")

    # 特征重要性
    print(f"\n{'─' * 60}")
    print("  📈 Top-10 特征重要性")
    print(f"{'─' * 60}")
    importance = model.feature_importances_
    indices = np.argsort(importance)[::-1][:10]
    for rank, idx in enumerate(indices, 1):
        name = feature_names[idx]
        imp = importance[idx]
        bar = "█" * int(imp * 100)
        print(f"  {rank:>2}. {name:<28} {imp:.4f}  {bar}")

    return model, {
        "auc": auc, "ks": ks, "precision": precision,
        "recall": recall, "f1": f1,
        "confusion_matrix": {"tn": int(tn), "fp": int(fp),
                              "fn": int(fn), "tp": int(tp)},
        "feature_importance": {
            feature_names[i]: float(importance[i])
            for i in indices
        },
    }


def _calc_ks(y_true, y_prob):
    """计算 KS 统计量"""
    from sklearn.metrics import roc_curve
    fpr, tpr, _ = roc_curve(y_true, y_prob)
    return float(np.max(tpr - fpr))


# ═══════════════════════════════════════════════════════
#  Step 4: 导出模型
# ═══════════════════════════════════════════════════════

def export_model(model, scaler, feature_names, metrics):
    """
    导出模型为双格式:
      1. PKL  (Python 直接加载)
      2. JSON (XGBoost 原生格式，Java 通过 XGBoost4j 加载)
    """
    import pickle

    # ── PKL 格式 ──
    pkl_path = os.path.join(MODEL_DIR, "risk_model.pkl")
    with open(pkl_path, "wb") as f:
        pickle.dump({
            "model": model,
            "scaler": scaler,
            "feature_names": feature_names,
            "metrics": metrics,
        }, f)
    print(f"\n  ✅ PKL 模型已保存: {pkl_path}")

    # ── JSON 格式 (Java 兼容) ──
    json_path = os.path.join(MODEL_DIR, "risk_model.json")
    model.get_booster().save_model(json_path)
    print(f"  ✅ JSON 模型已保存: {json_path}  (Java XGBoost4j 可直接加载)")

    # ── 特征配置 (JSON, 供 Java 推理时参考) ──
    config_path = os.path.join(MODEL_DIR, "feature_names.json")
    with open(config_path, "w", encoding="utf-8") as f:
        json.dump({
            "feature_names": feature_names,
            "feature_count": len(feature_names),
            "model_type": "XGBoost",
            "metrics": {k: round(v, 4) if isinstance(v, float) else v
                        for k, v in metrics.items()
                        if k not in ["feature_importance"]},
        }, f, ensure_ascii=False, indent=2)
    print(f"  ✅ 特征配置已保存: {config_path}")

    return pkl_path, json_path


# ═══════════════════════════════════════════════════════
#  推理示例（模拟 Java 调用流程）
# ═══════════════════════════════════════════════════════

def inference_demo():
    """演示加载模型并对单条申请打分。模拟 Java 端调用流程。"""
    import pickle

    pkl_path = os.path.join(MODEL_DIR, "risk_model.pkl")
    if not os.path.exists(pkl_path):
        print("  ⚠️ 模型文件不存在，跳过推理演示")
        return

    with open(pkl_path, "rb") as f:
        bundle = pickle.load(f)
    model = bundle["model"]
    scaler = bundle["scaler"]
    feature_names = bundle["feature_names"]

    # 模拟一个新申请
    new_application = pd.DataFrame([{
        "age": 28,
        "education_level": 3,       # 大专
        "work_years": 3,
        "monthly_income": 8000,
        "has_social_security": 1,
        "has_fund": 1,
        "overdue_count_2y": 1,      # 有过1次逾期
        "max_overdue_days": 30,
        "credit_card_usage_pct": 45.0,
        "credit_inquiries_3m": 2,
        "multi_loan_count": 1,
        "has_mortgage": 0,
        "dti_ratio": 35.0,
        "application_hour": 14,
        "info_completeness": 85.0,
    }])

    X, _, _, _ = feature_engineering(new_application, is_training=False, scaler=scaler)

    prob = model.predict_proba(X)[0, 1]

    print(f"\n{'─' * 60}")
    print("  🧪 推理演示（单条申请）")
    print(f"{'─' * 60}")
    print(f"  申请人: 28岁 大专 月入8000  1次逾期")
    print(f"  违约概率: {prob:.2%}")
    print(f"  风控建议: ", end="")
    if prob < 0.10:
        print("✅ 低风险 — 建议通过，正常利率")
    elif prob < 0.25:
        print("⚠️ 中风险 — 建议通过，利率上浮或降额")
    else:
        print("❌ 高风险 — 建议拒绝或要求增信")


# ═══════════════════════════════════════════════════════
#  Main
# ═══════════════════════════════════════════════════════

def main():
    print("=" * 60)
    print("  风控模型训练流水线 — XGBoost Credit Scoring")
    print("=" * 60)

    # Step 1: 生成数据
    print("\n[Step 1] 生成合成训练数据...")
    df = generate_synthetic_data(n_samples=20000)
    default_rate = df["is_default"].mean()
    print(f"  样本数: {len(df):,}")
    print(f"  违约率: {default_rate:.2%}")
    print(f"  特征数: {len(df.columns) - 1}")

    # Step 2: 特征工程
    print("\n[Step 2] 特征工程...")
    X, y, scaler, feature_names = feature_engineering(df, is_training=True)
    print(f"  处理后特征数: {len(feature_names)}")
    print(f"  特征列表: {feature_names}")

    # Step 3: 训练 + 评估
    print("\n[Step 3] 训练 XGBoost...")
    model, metrics = train_and_evaluate(X, y, feature_names)

    # Step 4: 导出
    print("\n[Step 4] 导出模型...")
    export_model(model, scaler, feature_names, metrics)

    # 推理演示
    inference_demo()

    print(f"\n{'=' * 60}")
    print("  训练完成！模型已保存到 ml_risk/model/")
    print(f"{'=' * 60}")


if __name__ == "__main__":
    main()
