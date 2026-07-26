import pandas as pd

def sort_reviews(df: pd.DataFrame) -> pd.DataFrame:
    return (
        df.sort_values(
            by=["user_id", "question_id", "review_time"]
        ).copy()
    )

GROUP_COLUMNS = ["user_id", "question_id"]
def running_average(
    df: pd.DataFrame,
    column: str,
) -> pd.Series:

    grouped = grouped_reviews(df)

    previous_sum = (
        grouped[column]
        .transform(lambda x: x.shift().cumsum())
    )

    previous_count = grouped.cumcount()

    return previous_sum / previous_count

def grouped_reviews(df: pd.DataFrame):
    """
    Returns reviews grouped by user and question.
    """

    return df.groupby(GROUP_COLUMNS)

def add_total_revisions(df: pd.DataFrame) -> pd.DataFrame:
    """
    Number of previous reviews for the same user and question.
    """

    df = df.sort_values(
        by=["user_id", "question_id", "review_time"]
    ).copy()

    df["total_revisions"] = (
        df.groupby(["user_id", "question_id"])
          .cumcount()
    )

    return df

def add_previous_correct(df: pd.DataFrame) -> pd.DataFrame:
    """
    Previous review correctness.
    """

    df = df.sort_values(
        by=["user_id", "question_id", "review_time"]
    ).copy()

    df["last_review_correct"] = (
        df.groupby(["user_id", "question_id"])["correct"]
          .shift(1)
    )

    return df

def add_previous_confidence(df: pd.DataFrame) -> pd.DataFrame:
    """
    Previous confidence score.
    """

    df = df.sort_values(
        by=["user_id", "question_id", "review_time"]
    ).copy()

    df["last_review_confidence"] = (
        df.groupby(["user_id", "question_id"])["confidence"]
          .shift(1)
    )

    return df

def add_previous_response_time(df: pd.DataFrame) -> pd.DataFrame:
    """
    Previous response time.
    """

    df = df.sort_values(
        by=["user_id", "question_id", "review_time"]
    ).copy()

    df["last_review_response_time"] = (
        df.groupby(["user_id", "question_id"])["response_time_seconds"]
          .shift(1)
    )

    return df

def add_previous_hesitation(df: pd.DataFrame) -> pd.DataFrame:
    """
    Previous hesitation.
    """

    df = df.sort_values(
        by=["user_id", "question_id", "review_time"]
    ).copy()

    df["last_review_hesitation"] = (
        df.groupby(["user_id", "question_id"])["hesitation_seconds"]
          .shift(1)
    )

    return df

def add_success_rate(df: pd.DataFrame) -> pd.DataFrame:
    """
    Running success rate before the current review.
    """

    df = sort_reviews(df)

    grouped = df.groupby(["user_id", "question_id"])

    previous_correct = (
        grouped["correct"]
        .transform(lambda x: x.shift().cumsum())
    )

    previous_reviews = grouped.cumcount()

    df["success_rate"] = (
        previous_correct / previous_reviews
    )

    return df

def add_average_confidence(df: pd.DataFrame) -> pd.DataFrame:
    """
    Running average confidence before the current review.
    """

    df = sort_reviews(df)

    df["average_confidence"] = running_average(
        df,
        "confidence",
    )

    return df


def add_average_response_time(df: pd.DataFrame) -> pd.DataFrame:
    """
    Running average response time before the current review.
    """

    df = sort_reviews(df)

    df["average_response_time"] = running_average(
        df,
        "response_time_seconds",
    )

    return df


def add_average_hesitation(df: pd.DataFrame) -> pd.DataFrame:
    """
    Running average hesitation before the current review.
    """

    df = sort_reviews(df)

    df["average_hesitation"] = running_average(
        df,
        "hesitation_seconds",
    )

    return df

