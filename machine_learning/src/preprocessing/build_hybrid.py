from __future__ import annotations

import argparse
import json
from datetime import UTC, datetime

import pandas as pd

from src.utils.config import FEATURE_DATA_DIR, RANDOM_SEED

DEFAULT_SYNTHETIC = FEATURE_DATA_DIR / "training_dataset.csv"

DEFAULT_REAL = FEATURE_DATA_DIR / "real_dataset.csv"

DEFAULT_OUTPUT = FEATURE_DATA_DIR / "hybrid.csv"

DEFAULT_META = FEATURE_DATA_DIR / "hybrid_meta.json"

DEFAULT_SYNTHETIC_SAMPLE = 5000


def build_hybrid(
    synthetic_path=DEFAULT_SYNTHETIC,
    real_path=DEFAULT_REAL,
    output_path=DEFAULT_OUTPUT,
    meta_path=DEFAULT_META,
    sample_size=DEFAULT_SYNTHETIC_SAMPLE,
    seed=RANDOM_SEED,
) -> pd.DataFrame:

    synthetic = pd.read_csv(synthetic_path)

    real = pd.read_csv(real_path)

    sample_size = min(sample_size, len(synthetic))

    synthetic_sample = synthetic.sample(
        n=sample_size,
        random_state=seed,
    )

    hybrid = pd.concat(
        [synthetic_sample, real],
        ignore_index=True,
    )

    FEATURE_DATA_DIR.mkdir(parents=True, exist_ok=True)

    hybrid.to_csv(output_path, index=False)

    meta = {
        "generated_at": datetime.now(UTC).isoformat(),
        "synthetic_source": str(synthetic_path),
        "real_source": str(real_path),
        "synthetic_rows": len(synthetic),
        "synthetic_sampled": sample_size,
        "real_rows": len(real),
        "total_rows": len(hybrid),
        "real_ratio": round(len(real) / len(hybrid), 4),
        "sample_seed": seed,
    }

    with open(meta_path, "w", encoding="utf-8") as handle:
        json.dump(meta, handle, indent=2)

    print(
        f"Saved {output_path}: {len(hybrid)} rows "
        f"(synthetic sampled {sample_size} of {len(synthetic)} "
        f"+ real {len(real)}, real_ratio {meta['real_ratio']})"
    )

    print(f"Metadata saved: {meta_path}")

    return hybrid


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Build the hybrid real+synthetic training dataset.",
    )

    parser.add_argument(
        "--synthetic",
        default=str(DEFAULT_SYNTHETIC),
        help="Path to the synthetic training CSV (default: data/features).",
    )

    parser.add_argument(
        "--real",
        default=str(DEFAULT_REAL),
        help="Path to the real-data CSV produced by export_real_dataset.",
    )

    parser.add_argument(
        "--output",
        default=str(DEFAULT_OUTPUT),
        help="Output hybrid CSV path.",
    )

    parser.add_argument(
        "--meta",
        default=str(DEFAULT_META),
        help="Output blend-metadata JSON path.",
    )

    parser.add_argument(
        "--sample-size",
        type=int,
        default=DEFAULT_SYNTHETIC_SAMPLE,
        help="Number of synthetic rows to sample (default: 5000).",
    )

    parser.add_argument(
        "--seed",
        type=int,
        default=RANDOM_SEED,
        help="Random seed for the synthetic subsample (default: 42).",
    )

    args = parser.parse_args()

    build_hybrid(
        synthetic_path=args.synthetic,
        real_path=args.real,
        output_path=args.output,
        meta_path=args.meta,
        sample_size=args.sample_size,
        seed=args.seed,
    )


if __name__ == "__main__":
    main()