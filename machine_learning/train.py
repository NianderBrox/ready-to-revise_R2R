
from __future__ import annotations

import argparse

from src.training.trainer import DATASET_PATH, run_training


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Train R2R recall-prediction models.",
    )

    parser.add_argument(
        "--dataset",
        default=str(DATASET_PATH),
        help="Path to training_dataset.csv (default: data/features).",
    )

    parser.add_argument(
        "--tune",
        action="store_true",
        help="Run hyperparameter search for the boosting models.",
    )

    parser.add_argument(
        "--keep-incumbent",
        action="store_true",
        help=(
            "Preserve the currently serving calibrated_best.joblib as "
            "calibrated_best_incumbent.joblib before training overwrites "
            "it (required by the eval gate)."
        ),
    )

    args = parser.parse_args()

    report = run_training(
        args.dataset,
        tune=args.tune,
        keep_incumbent=args.keep_incumbent,
    )

    print(report["best_model"])


if __name__ == "__main__":
    main()
