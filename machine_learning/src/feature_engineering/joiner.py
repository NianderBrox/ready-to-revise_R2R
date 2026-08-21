import pandas as pd


def join_tables(
    reviews: pd.DataFrame,
    questions: pd.DataFrame,
    sessions: pd.DataFrame,
    attempts: pd.DataFrame,
    schedules: pd.DataFrame | None = None,
) -> pd.DataFrame:
    """
    Merge all review-related tables into a single DataFrame.

    When `schedules` is provided, the pre-review FSRS
    retrievability (`recall_probability`) is attached per
    review. Join keys are normalized to strings because the DB
    driver returns uuid.UUID objects.
    """

    # Review + Question metadata
    df = reviews.merge(
        questions,
        left_on="question_id",
        right_on="id",
        suffixes=("", "_question"),
        how="left",
    )

    # Review + Session metadata
    df = df.merge(
        sessions,
        left_on="session_id",
        right_on="id",
        suffixes=("", "_session"),
        how="left",
    )

    # Review + Attempt metadata
    df = df.merge(
        attempts,
        left_on="id",
        right_on="review_id",
        suffixes=("", "_attempt"),
        how="left",
    )

    if schedules is not None:
        df = df.drop(columns=["review_id"])

        schedules = schedules.copy()

        schedules["review_id"] = schedules["review_id"].astype(str)

        df["id"] = df["id"].astype(str)

        df = df.merge(
            schedules[["review_id", "recall_probability"]],
            left_on="id",
            right_on="review_id",
            how="left",
        )

        df = df.drop(columns=["review_id"])

    return df
