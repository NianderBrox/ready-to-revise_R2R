
from __future__ import annotations

import numpy as np
from sklearn.metrics import (
    accuracy_score,
    f1_score,
    log_loss,
    precision_score,
    recall_score,
    roc_auc_score,
)


def compute_metrics(
    y_true,
    y_proba,
) -> dict:


    y_true = np.asarray(y_true).astype(bool)

    y_proba = np.asarray(y_proba, dtype=float)

    y_pred = (y_proba >= 0.5).astype(bool)

    return {
        "accuracy": round(float(accuracy_score(y_true, y_pred)), 4),
        "precision": round(float(precision_score(y_true, y_pred)), 4),
        "recall": round(float(recall_score(y_true, y_pred)), 4),
        "f1": round(float(f1_score(y_true, y_pred)), 4),
        "roc_auc": round(float(roc_auc_score(y_true, y_proba)), 4),
        "log_loss": round(float(log_loss(y_true, y_proba)), 4),
        "brier": round(
            float(np.mean((y_proba - y_true.astype(int)) ** 2)),
            4,
        ),
        "rmse": round(
            float(np.sqrt(np.mean((y_proba - y_true) ** 2))),
            4,
        ),
        "n": len(y_true),
    }
