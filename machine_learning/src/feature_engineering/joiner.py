import pandas as pd


def join_tables(
    reviews: pd.DataFrame,
    questions: pd.DataFrame,
    sessions: pd.DataFrame,
    attempts: pd.DataFrame,
) -> pd.DataFrame:
    """
    Merge all review-related tables into a single DataFrame.
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

    return df