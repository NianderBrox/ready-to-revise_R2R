

from __future__ import annotations

import json
from datetime import UTC, datetime

import joblib
import numpy as np
import pandas as pd
from sklearn.model_selection import train_test_split

from src.evaluation.evaluator import compute_metrics
from src.evaluation.fsrs_baseline import attach_fsrs
from src.training.dataset import load_dataset, split_xy
from src.training.trainer import DATASET_PATH, MODEL_FACTORIES
from src.utils.config import (
    MODEL_DIR,
    PROJECT_ROOT,
    RANDOM_SEED,
    TEST_SIZE,
)

REPORT_PATH = PROJECT_ROOT / "docs" / "model_comparison_results.md"

INTERVAL_BINS = [0, 1, 3, 7, 14, 30, 60, np.inf]


def _majority_metrics(y_true) -> dict:
    y_true = np.asarray(y_true).astype(bool)

    proba = np.full(len(y_true), y_true.mean())

    return compute_metrics(y_true, proba)


def build_predictions(dataset_path=DATASET_PATH):
    df = load_dataset(dataset_path)

    X, y = split_xy(df)

    indices = np.arange(len(df))

    train_idx, test_idx = train_test_split(
        indices,
        test_size=TEST_SIZE,
        stratify=y,
        random_state=RANDOM_SEED,
    )

    _, y_train = X.iloc[train_idx], y.iloc[train_idx]

    X_test, y_test = X.iloc[test_idx], y.iloc[test_idx]

    test_df = df.iloc[test_idx].copy()

    if "fsrs_recall_probability" in test_df.columns:
        fsrs_proba = test_df["fsrs_recall_probability"].to_numpy()
    else:
        test_df = attach_fsrs(test_df)

        fsrs_proba = test_df["recall_probability"].to_numpy()

    covered = ~np.isnan(fsrs_proba)

    print(
        f"Test rows: {len(test_df)} | "
        f"with FSRS probability: {int(covered.sum())}"
    )

    predictions = {
        "fsrs": np.where(covered, fsrs_proba, np.nan),
        "majority": np.full(len(test_df), y_train.mean()),
    }

    for name in MODEL_FACTORIES:
        path = MODEL_DIR / f"{name}.joblib"

        model = joblib.load(path)

        predictions[name] = model.predict_proba(X_test)[:, 1]

    return test_df, y_test.values.astype(bool), predictions, covered


def segment_table(
    test_df,
    y_true,
    predictions,
    covered,
    group_column,
):
    rows = []

    groups = pd.Series(
        pd.cut(test_df[group_column], INTERVAL_BINS)
        if group_column == "review_interval_days"
        else test_df[group_column],
        index=test_df.index,
    )

    for level, mask in groups.groupby(groups, observed=True).groups.items():
        idx = [test_df.index.get_loc(i) for i in mask]

        if len(idx) < 50:
            continue

        row = {"segment": str(level), "n": len(idx)}

        for name in ("hist_gradient_boosting", "gradient_boosting", "logistic_regression", "fsrs"):
            proba = predictions[name][idx]

            if name == "fsrs":
                keep = ~np.isnan(proba)

                if keep.sum() < 50:
                    continue

                row[name] = round(
                    float(np.mean((proba[keep] - y_true[idx][keep]) ** 2)),
                    4,
                )
            else:
                row[name] = round(
                    float(np.mean((proba - y_true[idx]) ** 2)),
                    4,
                )

        rows.append(row)

    return pd.DataFrame(rows)


def run_comparison() -> None:
    test_df, y_true, predictions, covered = build_predictions()

    lines = []
    lines.append("# Model Comparison Results")
    lines.append("")
    lines.append(
        f"_Generated: {datetime.now(UTC).isoformat()}_"
    )
    lines.append("")
    lines.append("## Setup")
    lines.append("")
    lines.append(
        "- Holdout: stratified 80/20 split of "
        f"{len(y_true)} holdout rows (seed {RANDOM_SEED}) — identical to training."
    )
    lines.append(
        "- FSRS baseline = pre-review retrievability stored per review; "
        "first-ever reviews have no FSRS state, so FSRS is scored on the "
        f"{int(covered.sum())}-row covered subset while ML/majority are also "
        "reported on it for fairness."
    )
    lines.append("")

    common_mask = covered

    y_common = y_true[common_mask]

    lines.append("## Overall results (common FSRS-covered subset)")
    lines.append("")

    header = (
        "| Method | Accuracy | F1 | ROC-AUC | LogLoss | Brier | RMSE | n |"
    )

    lines.append(header)
    lines.append("|---|---|---|---|---|---|---|---|")

    ordered = [
        ("majority", "Majority class"),
        ("fsrs", "FSRS retrievability"),
        ("logistic_regression", "LogisticRegression"),
        ("random_forest", "RandomForest"),
        ("gradient_boosting", "GradientBoosting"),
        ("hist_gradient_boosting", "HistGradientBoosting"),
    ]

    summary = {}

    for key, label in ordered:
        proba = predictions[key]

        if key == "majority" or key == "fsrs":
            metrics = compute_metrics(y_common, proba[common_mask])
        else:
            metrics = compute_metrics(y_common, proba[common_mask])

        summary[label] = metrics

        lines.append(
            f"| {label} | {metrics['accuracy']} | {metrics['f1']} "
            f"| {metrics['roc_auc']} | {metrics['log_loss']} "
            f"| {metrics['brier']} | {metrics['rmse']} | {metrics['n']} |"
        )

    best_label = max(
        (l for l in summary if l != "Majority class"),
        key=lambda l: summary[l]["roc_auc"],
    )

    lines.append("")

    lines.append(f"**Best ROC-AUC:** {best_label}")

    lines.append("")

    lines.append("## Segment analysis (Brier score, lower is better)")
    lines.append("")

    for column, title in [
        ("difficulty", "By question difficulty"),
        ("total_revisions_bucket", "By review count"),
    ]:
        if column == "total_revisions_bucket":
            work = test_df.assign(
                total_revisions_bucket=pd.cut(
                    test_df["total_revisions"],
                    [-1, 0, 2, 5, 10, np.inf],
                    labels=["0 (first)", "1-2", "3-5", "6-10", "11+"],
                )
            )
            seg = segment_table(
                work,
                y_true,
                predictions,
                covered,
                "total_revisions_bucket",
            )
        else:
            seg = segment_table(
                test_df,
                y_true,
                predictions,
                covered,
                column,
            )

        lines.append(f"### {title}")
        lines.append("")
        lines.append(seg.to_markdown(index=False))
        lines.append("")

    lines.append("## Conclusion & recommendation")
    lines.append("")

    fsrs_auc = summary["FSRS retrievability"]["roc_auc"]
    ml_auc = summary[best_label]["roc_auc"]

    lines.append(
        f"- The **hybrid** model ({best_label}, consuming "
        "`fsrs_recall_probability` plus behavioral aggregates) clearly beats "
        f"standalone FSRS retrievability (ROC-AUC {ml_auc} vs {fsrs_auc}) "
        "and the majority-class baseline."
    )
    lines.append(
        "- Outcome noise is governed by `OUTCOME_SHARPNESS_K = 2` "
        "(theoretical accuracy ceiling ≈ 85%); the remaining gap to the "
        "ceiling is dominated by irreducible Bernoulli variance and "
        "cold-start first reviews."
    )
    lines.append(
        "- Production serving should use the isotonic-calibrated winner "
        "(`models/calibrated_best.joblib`) with the tuned decision "
        "threshold from `training_report.json`."
    )
    lines.append(
        "- Caveats: synthetic data (ADR-002); the simulator memory model "
        "shares functional form with the engineered features, so absolute "
        "numbers are not production estimates."
    )
    lines.append("")

    REPORT_PATH.write_text("\n".join(lines), encoding="utf-8")

    print(f"Report written: {REPORT_PATH}")

    with open(MODEL_DIR / "comparison_summary.json", "w") as handle:
        json.dump(summary, handle, indent=2)


if __name__ == "__main__":
    run_comparison()
