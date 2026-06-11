"""
风控模型 — Give Me Some Credit 真实数据训练

用法:
    cd ml-risk-model
    PYTHONIOENCODING=utf-8 python train_real.py

数据源:
    cs-training.csv — Give Me Some Credit (Kaggle)，放 data/ 目录下
    Kaggle: https://www.kaggle.com/competitions/GiveMeSomeCredit/data
    (已通过国内镜像下载)

特征对齐:
    只使用 Java DB 中存在的字段 → 可直接用于生产环境的模型
"""
import os, sys, json, io, warnings
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.metrics import roc_auc_score, precision_score, recall_score, f1_score, confusion_matrix
from sklearn.preprocessing import StandardScaler
import xgboost as xgb

warnings.filterwarnings("ignore")
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8") if hasattr(sys.stdout, "buffer") else sys.stdout

DATA_DIR = os.path.join(os.path.dirname(__file__), "data")
MODEL_DIR = os.path.join(os.path.dirname(__file__), "model")
os.makedirs(DATA_DIR, exist_ok=True)
os.makedirs(MODEL_DIR, exist_ok=True)

# ═══════════════════════════════════════════════════════
#  特征对齐: 真实数据集 ↔ Java DB
# ═══════════════════════════════════════════════════════

# Give Me Some Credit → Java DB 映射
# 只保留两边都有的特征
GMS2JAVA = {
    "age": "age",                                          # ← users.id_card 推算
    "MonthlyIncome": "monthly_income",                     # ← (暂无，需加字段)
    "DebtRatio": "dti_ratio",                              # ← orders 聚合计算
    "NumberOfOpenCreditLinesAndLoans": "active_loan_count", # ← orders COUNT
    "NumberRealEstateLoansOrLines": "has_mortgage",        # ← immovables_cert
    "NumberOfTimes90DaysLate": "max_overdue_days",         # ← orders.overdue_days
    "NumberOfTime30-59DaysPastDueNotWorse": "overdue_count_2y", # ← repayment_schedule
    "NumberOfTime60-89DaysPastDueNotWorse": None,          # ← 用不到，合并到上面
    "RevolvingUtilizationOfUnsecuredLines": "credit_card_usage_pct",  # ← (暂无，需加字段)
    "NumberOfDependents": None,                            # ← Java DB 没有
}
TARGET_COL = "SeriousDlqin2yrs"

# ═══════════════════════════════════════════════════════
#  Step 1: 加载数据
# ═══════════════════════════════════════════════════════

def load_give_me_some_credit():
    """尝试加载 Give Me Some Credit 数据集"""
    path = os.path.join(DATA_DIR, "cs-training.csv")
    if not os.path.exists(path):
        return None

    df = pd.read_csv(path)
    # Kaggle 版本第一列是 Unnamed: 0 (行号), 删掉
    if "Unnamed: 0" in df.columns:
        df = df.drop(columns=["Unnamed: 0"])
    print(f"  ✅ 加载 Give Me Some Credit: {len(df):,} 行, {len(df.columns)} 列")
    return df


def generate_gms_like(n_samples=20000, seed=42):
    """
    按 Give Me Some Credit 的真实统计分布生成数据。
    统计数据来源: Kaggle GMS 竞赛 README (150k样本).
    """
    rng = np.random.default_rng(seed)

    # 特征分布参数 (mean, std) 来自真实数据
    dists = {
        "age": ("normal", 52, 15, 21, 90),
        "MonthlyIncome": ("lognormal", np.log(5500), 0.8, 500, 100000),
        "DebtRatio": ("lognormal", np.log(15), 1.5, 0, 200),
        "NumberOfOpenCreditLinesAndLoans": ("poisson", 8, 0, 0, 50),
        "NumberRealEstateLoansOrLines": ("poisson", 1, 0, 0, 10),
        "NumberOfTimes90DaysLate": ("poisson", 0.3, 0, 0, 15),
        "NumberOfTime30-59DaysPastDueNotWorse": ("poisson", 0.4, 0, 0, 15),
        "NumberOfTime60-89DaysPastDueNotWorse": ("poisson", 0.25, 0, 0, 15),
        "RevolvingUtilizationOfUnsecuredLines": ("beta", 0.15, 0, 0, 2),
        "NumberOfDependents": ("poisson", 0.8, 0, 0, 10),
    }

    data = {}
    for col, (dist, *params) in dists.items():
        if dist == "normal":
            mu, sigma, lo, hi = params
            data[col] = rng.normal(mu, sigma, n_samples).clip(lo, hi).astype(int if col == "age" else float)
        elif dist == "lognormal":
            mu, sigma, lo, hi = params
            data[col] = np.exp(rng.normal(mu, sigma, n_samples)).clip(lo, hi)
        elif dist == "poisson":
            lam, _, lo, hi = params
            data[col] = rng.poisson(lam, n_samples).clip(lo, hi)
        elif dist == "beta":
            a, _, lo, hi = params
            data[col] = rng.beta(a, 2, n_samples) * 100

    df = pd.DataFrame(data)

    # 生成标签: 基于特征逻辑
    logit = (
        -2.5
        + (data["NumberOfTimes90DaysLate"] >= 1) * 2.0
        + (data["NumberOfTime60-89DaysPastDueNotWorse"] >= 1) * 1.5
        + (data["NumberOfTime30-59DaysPastDueNotWorse"] >= 2) * 1.0
        + (data["DebtRatio"] > 40) * 0.8
        + (data["RevolvingUtilizationOfUnsecuredLines"] > 80) * 0.8
        + (df["age"] < 25) * 0.5
        + (data["MonthlyIncome"] < 3000) * 0.6
        + rng.normal(0, 0.5, n_samples)
    )
    df[TARGET_COL] = rng.binomial(1, 1 / (1 + np.exp(-logit)), n_samples)

    df["source"] = "synthetic_gms"
    print(f"  ✅ 生成 GMS-like 合成数据: {len(df):,} 行, 违约率={df[TARGET_COL].mean():.2%}")
    return df


# ═══════════════════════════════════════════════════════
#  Step 2: 特征对齐 → 只保留 Java DB 能提供的特征
# ═══════════════════════════════════════════════════════

def align_to_java_features(df):
    """Give Me Some Credit → Java DB 特征映射"""
    df = df.copy()

    col_map = {
        "age": "age",
        "MonthlyIncome": "monthly_income",
        "DebtRatio": "dti_ratio",
        "NumberOfOpenCreditLinesAndLoans": "active_loan_count",
        "NumberRealEstateLoansOrLines": "has_mortgage",
        "NumberOfTimes90DaysLate": "max_overdue_days_90",
        "NumberOfTime30-59DaysPastDueNotWorse": "overdue_30_59_count",
        "NumberOfTime60-89DaysPastDueNotWorse": "overdue_60_89_count",
        "RevolvingUtilizationOfUnsecuredLines": "credit_card_usage_pct",
    }

    if "SeriousDlqin2yrs" in df.columns:
        df["is_default"] = df["SeriousDlqin2yrs"].astype(int)
        df = df.drop(columns=["SeriousDlqin2yrs"])

    rename = {k: v for k, v in col_map.items() if k in df.columns}
    df = df.rename(columns=rename)

    # 删掉行号列
    df = df.drop(columns=[c for c in df.columns if c.startswith("Unnamed")], errors="ignore")

    # 保留映射后的特征 + 标签
    keep = list(rename.values()) + ["is_default"]
    keep = [c for c in keep if c in df.columns]
    seen = set()
    keep = [c for c in keep if not (c in seen or seen.add(c))]
    df = df[[c for c in keep if c in df.columns]]

    return df


# ═══════════════════════════════════════════════════════
#  Step 3: 特征工程 + 训练
# ═══════════════════════════════════════════════════════

def feature_engineering(df, is_training=True, scaler=None):
    """简化的特征工程 — 处理缺失值 + 标准化"""
    df = df.copy()

    # 数值列填充中位数
    numeric_cols = df.select_dtypes(include=[np.number]).columns.tolist()
    for col in numeric_cols:
        if col in ["is_default", "source"]:
            continue
        df[col] = df[col].fillna(df[col].median() if len(df[col].dropna()) > 0 else 0)

    # 分离标签
    y = df["is_default"].values if "is_default" in df.columns else None
    feature_cols = [c for c in df.columns if c not in ["is_default", "source"]]
    X = df[feature_cols].copy()

    # 标准化
    if is_training:
        scaler = StandardScaler()
        X_scaled = scaler.fit_transform(X)
    else:
        X_scaled = scaler.transform(X)

    return X_scaled.astype(np.float32), y, scaler, feature_cols


def train_report(X, y, feature_names):
    """训练 XGBoost 并输出报告"""
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y
    )

    model = xgb.XGBClassifier(
        objective="binary:logistic",
        eval_metric="auc",
        max_depth=4,
        learning_rate=0.05,
        n_estimators=150,
        subsample=0.8,
        colsample_bytree=0.8,
        random_state=42,
        n_jobs=-1,
    )
    model.fit(X_train, y_train, eval_set=[(X_test, y_test)], verbose=False)

    y_pred = model.predict(X_test)
    y_prob = model.predict_proba(X_test)[:, 1]
    auc = roc_auc_score(y_test, y_prob)
    cm = confusion_matrix(y_test, y_pred)
    tn, fp, fn, tp = cm.ravel()

    print(f"\n{'='*60}")
    print(f"  📊 评估报告 (测试集 {len(X_test):,} 条)")
    print(f"{'='*60}")
    print(f"  违约率: {y_test.mean():.2%}")
    print(f"  AUC:    {auc:.4f}")
    print(f"  精准率: {precision_score(y_test, y_pred):.4f}")
    print(f"  召回率: {recall_score(y_test, y_pred):.4f}")
    print(f"  F1:     {f1_score(y_test, y_pred):.4f}")
    print(f"  混淆矩阵: TN={tn}  FP={fp}  FN={fn}  TP={tp}")

    # 特征重要性
    print(f"\n  📈 特征重要性")
    imp = model.feature_importances_
    for rank, idx in enumerate(np.argsort(imp)[::-1], 1):
        bar = "█" * int(imp[idx] * 80)
        print(f"  {rank:>2}. {feature_names[idx]:<25} {imp[idx]:.4f}  {bar}")

    return model, auc


# ═══════════════════════════════════════════════════════
#  Main
# ═══════════════════════════════════════════════════════

def main():
    print("=" * 60)
    print("  风控模型 — 真实数据训练")
    print("=" * 60)

    # ── 加载数据 ──
    print("\n[Step 1] 加载数据...")
    df = load_give_me_some_credit()

    if df is None:
        print("  ⚠️ 未找到 cs-training.csv，使用合成数据（统计分布匹配 GMS）")
        df = generate_gms_like(20000)

    # ── 特征对齐 ──
    print("\n[Step 2] 特征对齐 → Java DB...")
    df = align_to_java_features(df)
    print(f"  对齐后特征 ({len(df.columns)-1}个): {[c for c in df.columns if c != 'is_default']}")

    # ── 特征工程 + 训练 ──
    print("\n[Step 3] 训练...")
    X, y, scaler, feature_names = feature_engineering(df)

    if y.sum() < 10:
        print("  ⚠️ 违约样本太少，无法训练")
        return

    model, auc = train_report(X, y, feature_names)

    # ── 导出 ──
    print(f"\n[Step 4] 导出模型...")
    import pickle
    pkl_path = os.path.join(MODEL_DIR, "risk_model_real.pkl")
    with open(pkl_path, "wb") as f:
        pickle.dump({"model": model, "scaler": scaler, "features": feature_names}, f)
    print(f"  ✅ {pkl_path}")

    json_path = os.path.join(MODEL_DIR, "risk_model_real.json")
    model.get_booster().save_model(json_path)
    print(f"  ✅ {json_path}  (Java XGBoost4j)")

    # ── 推理演示 ──
    print(f"\n{'─'*60}")
    print("  🧪 推理演示")
    print(f"{'─'*60}")
    demo = pd.DataFrame([{
        "age": 28, "monthly_income": 8000, "dti_ratio": 35,
        "active_loan_count": 1, "has_mortgage": 0,
        "max_overdue_days_90": 0, "overdue_30_59_count": 1,
        "overdue_60_89_count": 0, "credit_card_usage_pct": 45,
    }])
    demo_scaled = scaler.transform(demo).astype(np.float32)
    prob = model.predict_proba(demo_scaled)[0, 1]
    print(f"  28岁 月入8000 1次逾期 → 违约概率: {prob:.2%}")


if __name__ == "__main__":
    main()
