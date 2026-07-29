import pandas as pd
from src.feature_engineering.cleaning import (
    convert_datetime_columns,
)
from src.feature_engineering.historical_features import (
    add_total_revisions,
    add_previous_correct,
    add_previous_confidence,
    add_previous_response_time,
    add_previous_hesitation,
    add_success_rate,
    add_average_confidence,
    add_average_response_time,
    add_average_hesitation,
)

from src.feature_engineering.temporal_features import (
    add_review_interval,
    add_hour_of_day,
    add_day_of_week,
    add_session_duration,
    add_days_since_last_session,
)

from src.feature_engineering.validators import validate_features


def build_features(df: pd.DataFrame) -> pd.DataFrame:
    """
    Complete R2R feature engineering pipeline.

    Input:
        Raw joined review dataframe

    Output:
        ML-ready dataframe
    """
    df = convert_datetime_columns(df)
    # -------------------------
    # Historical features
    # -------------------------

    df = add_total_revisions(df)

    df = add_previous_correct(df)

    df = add_previous_confidence(df)

    df = add_previous_response_time(df)

    df = add_previous_hesitation(df)

    df = add_success_rate(df)

    df = add_average_confidence(df)

    df = add_average_response_time(df)

    df = add_average_hesitation(df)

    # -------------------------
    # Temporal features
    # -------------------------

    df = add_review_interval(df)

    df = add_hour_of_day(df)

    df = add_day_of_week(df)

    df = add_session_duration(df)

    df = add_days_since_last_session(df)

    # -------------------------
    # Validation
    # -------------------------

    validate_features(df)

    return df
