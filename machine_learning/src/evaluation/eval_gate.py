from __future__ import annotations

import argparse
import json
from datetime import UTC, datetime

import joblib
import numpy as np
from sklearn.base import clone
from sklearn.metrics import log_loss, roc_auc_score
from sklearn.model_selection import StratifiedKFold

from src.training.dataset import load_dataset, split_xy
from src.utils.config import FEATURE_DATA_DIR, MODEL_DIR, RANDOM_SEED

DEFAULT_DATASET = FEATURE_DATA_DIR / "hybrid.csv"

DEFAULT_CANDIDATE = MODEL_DIR / "calibrated_best.joblib"

DEFAULT_INCUMBENT = MODEL_DIR / "calibrated_best_incumbent.joblib"

DEFAULT_FOLDS = 5

REPORT_PATH = MODEL_DIR / "eval_gate_report.json"


def _fold_metrics(y_true, proba) -> dict:
    y = np.asarray(y_true, dtype=bool)

    metrics = {
        "n": len(y),
        "log_loss": round(float(log_loss(y, proba)), 4),
    }

    if len(np.unique(y)) == 2:
        metrics["roc_auc"] = round(
            float(roc_auc_score(y, proba)),
            4,
        )
    else:
        metrics["roc_auc"] = None

    return metrics


def _aggregate(fold_metrics: list[dict]) -> dict:
    aucs = [
        metrics["roc_auc"]
        for metrics in fold_metrics
        if metrics["roc_auc"] is not None
    ]

    total = sum(metrics["n"] for metrics in fold_metrics)

    weighted_log_loss = (
        sum(
            metrics["log_loss"] * metrics["n"]
            for metrics in fold_metrics
        )
        / total
        if total
        else float("nan")
    )

    return {
        "roc_auc_mean": (
            round(float(np.mean(aucs)), 4) if aucs else None
        ),
        "roc_auc_folds_used": len(aucs),
        "log_loss_weighted_mean": round(
            float(weighted_log_loss),
            4,
        ),
        "rows": total,
    }


def _decide(candidate: dict, incumbent: dict) -> tuple[str, str]:
    cand_auc = candidate["roc_auc_mean"]
    inc_auc = incumbent["roc_auc_mean"]

    if cand_auc is None or inc_auc is None:
        return (
            "requires_manual_review",
            "not enough class balance in test folds to compute ROC AUC",
        )

    if cand_auc > inc_auc:
        return (
            "ship",
            f"candidate CV ROC-AUC {cand_auc} > incumbent {inc_auc}",
        )

    if cand_auc < inc_auc:
        return (
            "reject",
            f"candidate CV ROC-AUC {cand_auc} < incumbent {inc_auc}",
        )

    cand_ll = candidate["log_loss_weighted_mean"]
    inc_ll = incumbent["log_loss_weighted_mean"]

    if cand_ll < inc_ll:
        return (
            "ship",
            (
                f"ROC-AUC tie ({cand_auc}); candidate log loss {cand_ll} "
                f"< incumbent {inc_ll}"
            ),
        )

    return (
        "reject",
        (
            f"ROC-AUC tie ({cand_auc}); candidate log loss {cand_ll} "
            f">= incumbent {inc_ll}"
        ),
    )


def run_gate(
    dataset_path=DEFAULT_DATASET,
    candidate_path=DEFAULT_CANDIDATE,
    incumbent_path=DEFAULT_INCUMBENT,
    folds=DEFAULT_FOLDS,
    seed=RANDOM_SEED,
) -> dict:

    df = load_dataset(dataset_path)

    X, y = split_xy(df)

    candidate_model = joblib.load(candidate_path)

    incumbent_model = joblib.load(incumbent_path)

    cv = StratifiedKFold(
        n_splits=folds,
        shuffle=True,
        random_state=seed,
    )

    candidate_folds = []
    incumbent_folds = []

    for train_idx, test_idx in cv.split(X, y):
        X_train = X.iloc[train_idx]
        X_test = X.iloc[test_idx]

        y_train = y.iloc[train_idx]
        y_test = y.iloc[test_idx]

        candidate_fit = clone(candidate_model).fit(
            X_train,
            y_train,
        )

        candidate_proba = candidate_fit.predict_proba(X_test)[:, 1]

        incumbent_proba = incumbent_model.predict_proba(X_test)[:, 1]

        candidate_folds.append(
            _fold_metrics(y_test, candidate_proba)
        )

        incumbent_folds.append(
            _fold_metrics(y_test, incumbent_proba)
        )

    candidate_summary = _aggregate(candidate_folds)

    incumbent_summary = _aggregate(incumbent_folds)

    verdict, reason = _decide(
        candidate_summary,
        incumbent_summary,
    )

    print(
        f"Evaluated {len(df)} rows across {folds} stratified folds "
        f"(seed {seed})."
    )

    print(
        "Metric (primary): mean CV ROC-AUC on shared folds; "
        "tie-break: weighted log loss"
    )

    print(
        f"Candidate {candidate_path}: "
        f"ROC-AUC {candidate_summary['roc_auc_mean']} "
        f"(folds {candidate_summary['roc_auc_folds_used']}) | "
        f"log loss {candidate_summary['log_loss_weighted_mean']}"
    )

    print(
        f"Incumbent {incumbent_path}: "
        f"ROC-AUC {incumbent_summary['roc_auc_mean']} "
        f"(folds {incumbent_summary['roc_auc_folds_used']}) | "
        f"log loss {incumbent_summary['log_loss_weighted_mean']}"
    )

    print(f"\nVERDICT: {verdict} - {reason}")

    MODEL_DIR.mkdir(parents=True, exist_ok=True)

    report = {
        "generated_at": datetime.now(UTC).isoformat(),
        "dataset": str(dataset_path),
        "candidate_path": str(candidate_path),
        "incumbent_path": str(incumbent_path),
        "folds": folds,
        "seed": seed,
        "primary_metric": "mean_cv_roc_auc",
        "tiebreak_metric": "weighted_log_loss",
        "decision_rule": (
            "ship if candidate ROC-AUC >= incumbent; "
            "tie-break with log loss"
        ),
        "candidate": candidate_summary,
        "incumbent": incumbent_summary,
        "verdict": verdict,
        "reason": reason,
    }

    with open(REPORT_PATH, "w", encoding="utf-8") as handle:
        json.dump(report, handle, indent=2)

    print(f"Report saved: {REPORT_PATH}")

    return report


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Eval gate: candidate vs incumbent on shared CV folds.",
    )

    parser.add_argument(
        "--dataset",
        default=str(DEFAULT_DATASET),
        help="Path to the hybrid CSV (default: data/features/hybrid.csv).",
    )

    parser.add_argument(
        "--candidate",
        default=str(DEFAULT_CANDIDATE),
        help="Candidate model .joblib (default: calibrated_best.joblib).",
    )

    parser.add_argument(
        "--incumbent",
        default=str(DEFAULT_INCUMBENT),
        help="Incumbent model .joblib (default: calibrated_best_incumbent.joblib).",
    )

    parser.add_argument(
        "--folds",
        type=int,
        default=DEFAULT_FOLDS,
        help="Number of stratified CV folds (default: 5).",
    )

    parser.add_argument(
        "--seed",
        type=int,
        default=RANDOM_SEED,
        help="Random seed for the CV split (default: 42).",
    )

    args = parser.parse_args()

    run_gate(
        dataset_path=args.dataset,
        candidate_path=args.candidate,
        incumbent_path=args.incumbent,
        folds=args.folds,
        seed=args.seed,
    )


if __name__ == "__main__":
    main()