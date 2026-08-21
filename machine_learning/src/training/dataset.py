from __future__ import annotations

import pandas as pd

from src.feature_engineering.feature_manifest import ALL_FEATURES, TARGET


def load_dataset(
    path,
) -> pd.DataFrame:
    """
    Loads the exported training dataset.
    """

    df = pd.read_csv(path)

    missing = set(ALL_FEATURES + [TARGET]) - set(df.columns)

    if missing:
        raise ValueError(
            f"Dataset is missing required columns: {missing}"
        )

    return df


def split_xy(
    df: pd.DataFrame,
):
    return df[ALL_FEATURES].copy(), df[TARGET].astype(bool)
