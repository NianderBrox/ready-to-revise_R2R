"""
Leakage and correctness tests for aggregate features.

Invariants:
    - user-level aggregates use only EARLIER reviews of that
      user (chronological, across questions)
    - question hardness uses only EARLIER reviews by ANY user
    - rolling windows exclude the current review
    - streaks count consecutive corrects ending at the previous
      review and reset to 0 after a failure
"""

from __future__ import annotations

import numpy as np
import pandas as pd
import pytest

from src.feature_engineering.aggregate_features import (
    add_consecutive_correct,
    add_fsrs_probability,
    add_hesitation_response_ratio,
    add_question_global_success_rate,
    add_recent_success_rate,
    add_user_success_rate,
)


def _frame(rows: list[dict]) -> pd.DataFrame:
    df = pd.DataFrame(rows)

    df["review_time"] = pd.to_datetime(df["review_time"])

    return df


def test_user_success_rate_excludes_current_review():
    df = _frame(
        [
            {
                "user_id": "u",
                "question_id": f"q{i}",
                "review_time": f"2026-01-0{i + 1}",
                "correct": c,
            }
            for i, c in enumerate([True, True, False])
        ]
    )

    result = add_user_success_rate(df)

    assert result["user_success_rate"].isna().iloc[0]

    assert result["user_success_rate"].iloc[1] == pytest.approx(1.0)

    assert result["user_success_rate"].iloc[2] == pytest.approx(1.0)


def test_user_aggregates_are_chronological_across_questions():
    """
    Reviews arrive interleaved across questions; the expanding
    mean must follow review_time, not dataframe order.
    """

    df = _frame(
        [
            {
                "user_id": "u",
                "question_id": "q2",
                "review_time": "2026-01-02",
                "correct": False,
            },
            {
                "user_id": "u",
                "question_id": "q1",
                "review_time": "2026-01-01",
                "correct": True,
            },
        ]
    )

    result = add_user_success_rate(df)

    q2_row = result[result["question_id"] == "q2"].iloc[0]

    assert q2_row["user_success_rate"] == pytest.approx(1.0)


def test_question_hardness_uses_other_users_only_before_now():
    df = _frame(
        [
            {
                "user_id": "u1",
                "question_id": "q",
                "review_time": "2026-01-01",
                "correct": False,
            },
            {
                "user_id": "u2",
                "question_id": "q",
                "review_time": "2026-01-03",
                "correct": True,
            },
            {
                "user_id": "u3",
                "question_id": "q",
                "review_time": "2026-01-05",
                "correct": True,
            },
        ]
    )

    result = add_question_global_success_rate(df)

    values = result.sort_values("review_time")[
        "question_global_success_rate"
    ]

    assert values.isna().iloc[0]

    assert values.iloc[1] == pytest.approx(0.0)

    assert values.iloc[2] == pytest.approx(0.5)


def test_rolling_window_excludes_current_and_caps_at_five():
    rows = [
        {
            "user_id": "u",
            "question_id": "q",
            "review_time": f"2026-01-{d:02d}",
            "correct": c,
            "confidence_score": 1.0 if c else 0.0,
        }
        for d, c in enumerate([True] * 7 + [False], start=1)
    ]

    df = _frame(rows)

    result = add_recent_success_rate(df)

    rates = result["recent_success_rate_5"]

    assert rates.isna().iloc[0]

    assert rates.iloc[5] == pytest.approx(1.0)

    assert rates.iloc[7] == pytest.approx(1.0)


def test_streak_counts_then_resets_on_failure():
    df = _frame(
        [
            {
                "user_id": "u",
                "question_id": "q",
                "review_time": f"2026-01-0{i + 1}",
                "correct": c,
            }
            for i, c in enumerate([True, True, False, True, True])
        ]
    )

    result = add_consecutive_correct(df)

    streaks = result["consecutive_correct"].tolist()

    assert np.isnan(streaks[0])

    assert streaks[1] == 1

    assert streaks[2] == 2

    assert streaks[3] == 0

    assert streaks[4] == 1


def test_fsrs_flag_marks_missing_estimates():
    df = _frame(
        [
            {
                "review_time": "2026-01-01",
                "recall_probability": np.nan,
            },
            {
                "review_time": "2026-01-02",
                "recall_probability": 0.83,
            },
        ]
    )

    result = add_fsrs_probability(df)

    assert result["had_fsrs_estimate"].tolist() == [0, 1]

    assert result["fsrs_recall_probability"].iloc[1] == pytest.approx(
        0.83
    )


def test_hesitation_ratio_zero_guard():
    df = pd.DataFrame(
        {
            "average_response_time": [0.0, 10.0],
            "average_hesitation": [5.0, 2.0],
        }
    )

    result = add_hesitation_response_ratio(df)

    assert np.isnan(result["hesitation_response_ratio"].iloc[0])

    assert result["hesitation_response_ratio"].iloc[1] == pytest.approx(
        0.2
    )
