

from __future__ import annotations

import json
from datetime import UTC, datetime
from time import time

import joblib
import numpy as np
from scipy.stats import randint, uniform
from sklearn.calibration import CalibratedClassifierCV
from sklearn.ensemble import (
    GradientBoostingClassifier,
    HistGradientBoostingClassifier,
    RandomForestClassifier,
)
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (
    accuracy_score,
    confusion_matrix,
    f1_score,
    log_loss,
    precision_score,
    recall_score,
    roc_auc_score,
)
from sklearn.model_selection import (
    RandomizedSearchCV,
    StratifiedKFold,
    cross_val_predict,
    cross_val_score,
    train_test_split,
)
from sklearn.pipeline import Pipeline

from src.training.dataset import load_dataset, split_xy
from src.training.preprocessing import build_preprocessor
from src.utils.config import (
    FEATURE_DATA_DIR,
    MODEL_DIR,
    RANDOM_SEED,
    TEST_SIZE,
)

DATASET_PATH = FEATURE_DATA_DIR / "training_dataset.csv"

CV_FOLDS = 5

TUNE_ITER = 10

TUNE_FOLDS = 3

TUNE_SUBSAMPLE = 100_000


def _logistic_regression() -> Pipeline:
    return Pipeline(
        steps=[
            ("preprocess", build_preprocessor(scale_numerical=True)),
            (
                "model",
                LogisticRegression(
                    max_iter=2000,
                    C=0.5,
                    random_state=RANDOM_SEED,
                ),
            ),
        ]
    )


def _random_forest(**overrides) -> Pipeline:
    config = {
        "n_estimators": 300,
        "min_samples_leaf": 5,
        "n_jobs": -1,
        "random_state": RANDOM_SEED,
    }

    config.update(overrides)

    return Pipeline(
        steps=[
            ("preprocess", build_preprocessor(scale_numerical=False)),
            ("model", RandomForestClassifier(**config)),
        ]
    )


def _gradient_boosting(**overrides) -> Pipeline:
    config = {
        "n_estimators": 300,
        "learning_rate": 0.05,
        "max_depth": 3,
        "subsample": 0.9,
        "random_state": RANDOM_SEED,
    }

    config.update(overrides)

    return Pipeline(
        steps=[
            ("preprocess", build_preprocessor(scale_numerical=False)),
            ("model", GradientBoostingClassifier(**config)),
        ]
    )


def _hist_gradient_boosting(**overrides) -> Pipeline:
    config = {
        "max_iter": 400,
        "learning_rate": 0.06,
        "max_leaf_nodes": 31,
        "l2_regularization": 1.0,
        "early_stopping": False,
        "random_state": RANDOM_SEED,
    }

    config.update(overrides)

    return Pipeline(
        steps=[
            ("preprocess", build_preprocessor(scale_numerical=False)),
            ("model", HistGradientBoostingClassifier(**config)),
        ]
    )


MODEL_FACTORIES = {
    "logistic_regression": _logistic_regression,
    "random_forest": _random_forest,
    "gradient_boosting": _gradient_boosting,
    "hist_gradient_boosting": _hist_gradient_boosting,
}

PARAM_DISTRIBUTIONS = {
    "hist_gradient_boosting": {
        "model__learning_rate": uniform(0.03, 0.12),
        "model__max_iter": randint(250, 600),
        "model__max_leaf_nodes": randint(15, 63),
        "model__min_samples_leaf": randint(10, 60),
        "model__l2_regularization": uniform(0.01, 5.0),
    },
}


def evaluate_predictions(
    y_true,
    y_pred,
    y_proba,
) -> dict:
    return {
        "accuracy": round(float(accuracy_score(y_true, y_pred)), 4),
        "precision": round(float(precision_score(y_true, y_pred)), 4),
        "recall": round(float(recall_score(y_true, y_pred)), 4),
        "f1": round(float(f1_score(y_true, y_pred)), 4),
        "roc_auc": round(float(roc_auc_score(y_true, y_proba)), 4),
        "log_loss": round(float(log_loss(y_true, y_proba)), 4),
        "confusion_matrix": confusion_matrix(
            y_true,
            y_pred,
        ).tolist(),
    }


def stratified_subsample(
    X,
    y,
    size: int,
):
    if len(X) <= size:
        return X, y

    frac = size / len(X)

    _, X_sub, _, y_sub = train_test_split(
        X,
        y,
        test_size=frac,
        stratify=y,
        random_state=RANDOM_SEED,
    )

    return X_sub, y_sub


def tune_model(
    name: str,
    X_train,
    y_train,
):


    X_sub, y_sub = stratified_subsample(
        X_train,
        y_train,
        TUNE_SUBSAMPLE,
    )

    print(
        f"Tuning {name} on {len(X_sub)} rows "
        f"({TUNE_ITER} iter x {TUNE_FOLDS} folds)"
    )

    search = RandomizedSearchCV(
        estimator=MODEL_FACTORIES[name](),
        param_distributions=PARAM_DISTRIBUTIONS[name],
        n_iter=TUNE_ITER,
        cv=StratifiedKFold(
            n_splits=TUNE_FOLDS,
            shuffle=True,
            random_state=RANDOM_SEED,
        ),
        scoring="roc_auc",
        n_jobs=-1,
        random_state=RANDOM_SEED,
        refit=False,
    )

    started = time()

    search.fit(X_sub, y_sub)

    print(
        f"Best CV AUC {search.best_score_:.4f} | "
        f"params {search.best_params_} | "
        f"{round(time() - started, 1)}s"
    )

    clean_params = {
        key.replace("model__", ""): value
        for key, value in search.best_params_.items()
    }

    return MODEL_FACTORIES[name](**clean_params), clean_params


def optimize_threshold(
    pipeline,
    X_train,
    y_train,
    cv,
) -> float:
=

    print("Optimizing decision threshold on OOF predictions")

    oof_proba = cross_val_predict(
        pipeline,
        X_train,
        y_train,
        cv=cv,
        method="predict_proba",
        n_jobs=-1,
    )[:, 1]

    thresholds = np.arange(0.05, 0.96, 0.01)

    accuracies = [
        ((oof_proba >= t).astype(bool) == y_train).mean()
        for t in thresholds
    ]

    best = float(thresholds[int(np.argmax(accuracies))])

    print(f"Best threshold {best:.2f}")

    return best


def run_training(
    dataset_path=DATASET_PATH,
    tune: bool = False,
) -> dict:
    print(f"Loading dataset: {dataset_path}")

    df = load_dataset(dataset_path)

    X, y = split_xy(df)

    print(f"Rows: {len(X)} | Features: {X.shape[1]}")

    X_train, X_test, y_train, y_test = train_test_split(
        X,
        y,
        test_size=TEST_SIZE,
        stratify=y,
        random_state=RANDOM_SEED,
    )

    print(
        f"Train: {len(X_train)} | Test: {len(X_test)} "
        f"| Positive rate: {y.mean():.3f}"
    )

    cv = StratifiedKFold(
        n_splits=CV_FOLDS,
        shuffle=True,
        random_state=RANDOM_SEED,
    )

    results = {}

    fitted_models = {}

    tuned_params = {}

    for name, factory in MODEL_FACTORIES.items():
        print(f"\n=== {name} ===")

        if tune and name in PARAM_DISTRIBUTIONS:
            pipeline, params = tune_model(name, X_train, y_train)

            tuned_params[name] = params
        else:
            pipeline = factory()

        started = time()

        scores = cross_val_score(
            pipeline,
            X_train,
            y_train,
            cv=cv,
            scoring="roc_auc",
            n_jobs=-1,
        )

        elapsed = round(time() - started, 1)

        print(
            f"CV ROC-AUC: {scores.mean():.4f} "
            f"(+/- {scores.std():.4f}) in {elapsed}s"
        )

        pipeline.fit(X_train, y_train)

        proba = pipeline.predict_proba(X_test)[:, 1]

        pred = (proba >= 0.5).astype(bool)

        metrics = evaluate_predictions(y_test, pred, proba)

        metrics["cv_roc_auc_mean"] = round(float(scores.mean()), 4)
        metrics["cv_roc_auc_std"] = round(float(scores.std()), 4)
        metrics["cv_seconds"] = elapsed

        print(metrics)

        results[name] = metrics

        fitted_models[name] = pipeline

    best_name = max(
        results,
        key=lambda name: results[name]["cv_roc_auc_mean"],
    )

    best_pipeline = fitted_models[best_name]

    threshold = optimize_threshold(best_pipeline, X_train, y_train, cv)

    print(f"\nCalibrating {best_name} (isotonic)")

    calibrated = CalibratedClassifierCV(
        estimator=best_pipeline,
        method="isotonic",
        cv=StratifiedKFold(
            n_splits=3,
            shuffle=True,
            random_state=RANDOM_SEED,
        ),
    )

    calibrated.fit(X_train, y_train)

    cal_proba = calibrated.predict_proba(X_test)[:, 1]

    cal_metrics = evaluate_predictions(
        y_test,
        (cal_proba >= threshold).astype(bool),
        cal_proba,
    )

    MODEL_DIR.mkdir(parents=True, exist_ok=True)

    for name, pipeline in fitted_models.items():
        path = MODEL_DIR / f"{name}.joblib"

        joblib.dump(pipeline, path)

        print(f"Saved {path}")

    calibrated_path = MODEL_DIR / "calibrated_best.joblib"

    joblib.dump(calibrated, calibrated_path)

    print(f"Saved {calibrated_path}")

    report = {
        "generated_at": datetime.now(UTC).isoformat(),
        "dataset": str(dataset_path),
        "rows": len(df),
        "train_rows": len(X_train),
        "test_rows": len(X_test),
        "positive_rate": round(float(y.mean()), 4),
        "tuned": tuned_params,
        "decision_threshold": round(threshold, 2),
        "models": results,
        "best_model": best_name,
        "calibrated_best": cal_metrics,
    }

    metrics_path = MODEL_DIR / "training_report.json"

    with open(metrics_path, "w") as handle:
        json.dump(report, handle, indent=2)

    print(f"\nBest model: {best_name} @ threshold {threshold:.2f}")

    print(f"Report saved: {metrics_path}")

    return report


def main(tune: bool = False) -> None:
    run_training(tune=tune)


if __name__ == "__main__":
    main()
