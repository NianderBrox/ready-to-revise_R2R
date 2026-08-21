"""
Historical feature engineering.

Precondition:
    DataFrame must already be sorted by
    user_id, question_id and review_time.

Assumption:
    The input DataFrame is already sorted by:
        user_id -> question_id -> review_time

Sorting is intentionally NOT performed inside individual functions.
The caller (feature_builder.py) is responsible for sorting once before
invoking any feature builders.

All historical features use only information available before the
current review to avoid target leakage.

"""
import numpy as np
import pandas as pd


def sort_reviews(df: pd.DataFrame) -> pd.DataFrame:
    return df.sort_values(by=["user_id", "question_id", "review_time"]).copy()


GROUP_COLUMNS = ["user_id", "question_id"]


def running_average(
    df: pd.DataFrame,
    column: str,
    ) -> pd.Series:

    grouped = grouped_reviews(df)

    previous_sum = (
        grouped[column]
        .transform(lambda x: x.shift().cumsum())
        .astype(float)
    )

    previous_count = _safe_previous_count(grouped)

    return previous_sum / previous_count


def _safe_previous_count(grouped):
    """
    Number of reviews BEFORE the current one per group.

    The first review of a group has count 0, which becomes NaN
    so that downstream divisions produce NaN instead of raising
    ZeroDivisionError (0/0 has no defined rate).
    """

    return grouped.cumcount().replace(0, np.nan)


def grouped_reviews(df: pd.DataFrame):
    """
    Returns reviews grouped by user and question.
    """

    return df.groupby(GROUP_COLUMNS)


def add_total_revisions(df: pd.DataFrame) -> pd.DataFrame:
    """
    Number of previous reviews for the same user and question.
    """
    df["total_revisions"] = grouped_reviews(df).cumcount()

    return df


def add_previous_correct(df: pd.DataFrame) -> pd.DataFrame:
    """
    Previous review correctness.
    """
    df["last_review_correct"] = (
        grouped_reviews(df)["correct"]
        .shift(1)
    )


    return df


def add_previous_confidence_score(df: pd.DataFrame) -> pd.DataFrame:
    """
    Previous confidence score.
    """

    df["last_review_confidence_score"] = (
        grouped_reviews(df)["confidence_score"]
        .shift(1)
    )

    return df


def add_previous_response_time(df: pd.DataFrame) -> pd.DataFrame:
    """
    Previous response time.
    """


    df["last_review_response_time"] = df.groupby(["user_id", "question_id"])[
        "response_time_seconds"
    ].shift(1)

    return df


def add_previous_hesitation(df: pd.DataFrame) -> pd.DataFrame:
    """
    Previous hesitation.
    """


    df["last_review_hesitation"] = df.groupby(["user_id", "question_id"])[
        "hesitation_seconds"
    ].shift(1)

    return df


def add_success_rate(df: pd.DataFrame) -> pd.DataFrame:
    """
    Running success rate before the current review.
    """


    grouped = grouped_reviews(df)

    previous_correct = (
        grouped["correct"]
        .transform(lambda x: x.shift().cumsum())
        .astype(float)
    )

    previous_reviews = _safe_previous_count(grouped)

    df["success_rate"] = previous_correct / previous_reviews

    return df


def add_average_confidence(df: pd.DataFrame) -> pd.DataFrame:
    """
    Running average confidence before the current review.
    """


    df["average_confidence"] = running_average(
        df,
        "confidence_score",
    )

    return df


def add_average_response_time(df: pd.DataFrame) -> pd.DataFrame:
    """
    Running average response time before the current review.
    """


    df["average_response_time"] = running_average(
        df,
        "response_time_seconds",
    )

    return df


def add_average_hesitation(df: pd.DataFrame) -> pd.DataFrame:
    """
    Running average hesitation before the current review.
    """

    df["average_hesitation"] = running_average(
        df,
        "hesitation_seconds",
    )

    return df
