"""
FSRS theoretical baseline (roadmap Phase 8).

Uses the pre-review retrievability stored on every
review_schedules row as FSRS's prediction of `correct`.
"""

from __future__ import annotations

import pandas as pd

from src.database.connection import engine

SCHEDULES_QUERY = """
    SELECT review_id, recall_probability, scheduled_interval_days
    FROM review_schedules
"""


def load_fsrs_predictions() -> pd.DataFrame:
    return pd.read_sql(SCHEDULES_QUERY, engine)


def attach_fsrs(
    df: pd.DataFrame,
    review_id_column: str = "id",
) -> pd.DataFrame:
    """
    Merges FSRS retrievability onto a review-level DataFrame.

    Both join keys are normalized to strings because the DB
    driver returns uuid.UUID objects while the CSV export has
    plain strings.
    """

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
