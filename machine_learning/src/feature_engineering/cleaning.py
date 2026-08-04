import pandas as pd

DATETIME_COLUMNS = [
    "review_time",
    "previous_review_time",
    "started_at",
    "ended_at",
]


def convert_datetime_columns(df: pd.DataFrame) -> pd.DataFrame:
    """
    Convert timestamp columns to pandas datetime.
    """

    df = df.copy()

    for column in DATETIME_COLUMNS:
        if column in df.columns:
            df[column] = pd.to_datetime(df[column], errors="coerce")

    return df
