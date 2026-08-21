"""
Entry point for the training pipeline.

    1. Export features from PostgreSQL -> training_dataset.csv
       (run `python -m src.preprocessing.export_dataset` first,
        or pass --skip-export if the CSV is current)
    2. Train + compare models, persist artifacts to models/
"""

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

    args = parser.parse_args()

    report = run_training(args.dataset, tune=args.tune)

    print(report["best_model"])


if __name__ == "__main__":
    main()
