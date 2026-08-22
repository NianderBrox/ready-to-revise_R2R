

from __future__ import annotations

import pandas as pd

from src.database.connection import engine


def load_fsrs_predictions() -> pd.DataFrame:
    return pd.read_sql(SCHEDULES_QUERY, engine)


def attach_fsrs(
    df: pd.DataFrame,
    review_id_column: str = "id",
) -> pd.DataFrame:


    schedules = load_fsrs_predictions()

    schedules["review_id"] = schedules["review_id"].astype(str)

    merged = df.copy()

    merged[review_id_column] = merged[review_id_column].astype(str)

    return merged.merge(
        schedules,
        left_on=review_id_column,
        right_on="review_id",
        how="left",
    )
