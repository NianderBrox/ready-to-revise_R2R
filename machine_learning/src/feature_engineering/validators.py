import pandas as pd

from src.feature_engineering.feature_manifest import (
    ALL_FEATURES,
    TARGET,
)


def validate_features(df: pd.DataFrame):

    missing = set(ALL_FEATURES + [TARGET]) - set(df.columns)

    if missing:
        raise ValueError(
            f"Missing features: {missing}"
        )

    return True