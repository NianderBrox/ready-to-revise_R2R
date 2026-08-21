"""
Leakage and shift-correctness tests for historical features.

The core invariant: every historical feature for a review may
only use STRICTLY EARLIER reviews of the same (user, question)
pair. The first review must therefore yield NaN, never 0.
"""

from __future__ import annotations

import numpy as np
import pandas as pd
import pytest

from src.feature_engineering.historical_features import (
    add_average_confidence,
    add_average_hesitation,
    add_average_response_time,
    add_success_rate,
    add_total_revisions,
    sort_reviews,
)


@pytest.fixture()
def reviews():
    """
    One user, two questions:
      q1: three reviews with known values
      q2: a single review (cold start edge case)
    """

    return pd.DataFrame(
        {
            "user_id": ["u1"] * 4,
            "question_id": ["q1", "q1", "q1", "q2"],
            "review_time": pd.to_datetime(
                [
                    "2026-01-01",
                    "2026-01-03",
                    "2026-01-07",
                    "2026-01-02",
                ]
            ),
            "correct": [True, True, False, True],
            "confidence_score": [0.25, 0.90, 0.60, 0.60],
            "response_time_seconds": [10.0, 20.0, 30.0, 5.0],
            "hesitation_seconds": [2.0, 4.0, 8.0, 1.0],
        }
    )


def _prepared(reviews):
    df = add_total_revisions(sort_reviews(reviews))

    df = add_success_rate(df)

    return df


def test_first_review_has_nan_success_rate(reviews):
    result = _prepared(reviews)

    first_rows = result[result["total_revisions"] == 0]

    assert first_rows["success_rate"].isna().all()


def test_first_review_does_not_raise_zero_division(reviews):
    _prepared(reviews)


def test_success_rate_uses_only_previous_reviews(reviews):
    result = _prepared(reviews)

    by_q = result.set_index(["question_id", "total_revisions"])

    assert by_q.loc[("q1", 1), "success_rate"] == pytest.approx(1.0)

    assert by_q.loc[("q1", 2), "success_rate"] == pytest.approx(1.0)

    assert np.isnan(by_q.loc[("q2", 0), "success_rate"])


def test_total_revisions_is_zero_based_cumcount(reviews):
    result = _prepared(reviews)

    q1_counts = sorted(
        result[result["question_id"] == "q1"]["total_revisions"]
    )

    assert q1_counts == [0, 1, 2]


def test_running_averages_shift_by_one():
    df = pd.DataFrame(
        {
            "user_id": ["u"] * 3,
            "question_id": ["q"] * 3,
            "review_time": pd.to_datetime(
                ["2026-01-01", "2026-01-02", "2026-01-03"]
            ),
            "confidence_score": [0.2, 0.6, 1.0],
            "response_time_seconds": [4.0, 8.0, 16.0],
            "hesitation_seconds": [1.0, 3.0, 5.0],
        }
    )

    df = add_total_revisions(sort_reviews(df))

    df = add_average_confidence(df)

    df = add_average_response_time(df)

    df = add_average_hesitation(df)

    row_two = df[df["total_revisions"] == 1].iloc[0]

    assert row_two["average_confidence"] == pytest.approx(0.2)

    assert row_two["average_response_time"] == pytest.approx(4.0)

    assert row_two["average_hesitation"] == pytest.approx(1.0)

    row_three = df[df["total_revisions"] == 2].iloc[0]

    assert row_three["average_confidence"] == pytest.approx(0.4)

    assert row_three["average_response_time"] == pytest.approx(6.0)

    assert row_three["average_hesitation"] == pytest.approx(2.0)
