from __future__ import annotations

import numpy as np
import pandas as pd
import pytest

from src.feature_engineering.feature_manifest import (
    ALL_FEATURES,
    TARGET,
)
from src.preprocessing.export_real_dataset import (
    build_real_dataset,
    validate_view,
)


def _mimicked_view() -> pd.DataFrame:


    rows = []

    review_times = pd.to_datetime([
        "2026-01-01T08:00:00",
        "2026-01-03T09:15:00",
        "2026-01-05T10:30:00",
    ])

    for user, question, session, difficulty in [
        ("u1", "q1", "s1", "EASY"),
        ("u1", "q2", "s2", "MEDIUM"),
        ("u2", "q1", "s3", "HARD"),
        ("u2", "q2", "s4", "EASY"),
    ]:
        for index, when in enumerate(review_times):
            rows.append({
                "id": f"{user}-{question}-{index}",
                "user_id": user,
                "question_id": question,
                "session_id": session,
                "review_time": when,
                "previous_review_time": (
                    review_times[index - 1]
                    if index > 0
                    else pd.NaT
                ),
                "correct": index % 2 == 0,
                "confidence": ("HIGH" if index % 2 == 0 else "MEDIUM"),
                "confidence_score": (0.8 if index % 2 == 0 else 0.6),
                "response_time_seconds": 8.0 + index,
                "hesitation_seconds": 2.0 + index,
                "answer_changes": index,
                "repetition_number": index,
                "subject": "math",
                "topic": "algebra",
                "word_count": 10 + index,
                "character_count": 50 + index,
                "recall_probability": (
                    np.nan if index == 0 else float(0.55 + 0.05 * index)
                ),
                "difficulty": difficulty,
                "question_position_in_session": index + 1,
                "session_duration_minutes": (
                    float(15.0 if index % 2 == 0 else 5.0)
                ),
                "input_rating": ("GOOD" if index % 2 == 0 else "AGAIN"),
                "scheduled_interval_days": 3.0 * (index + 1),
                "next_review_at": when + pd.Timedelta(days=3),
                "hour_of_day": 8,
                "day_of_week": 3,
            })

    return pd.DataFrame(rows)


def test_build_real_dataset_returns_valid_features():
    view = _mimicked_view()

    dataset = build_real_dataset(view)

    assert len(dataset) == len(view)

    assert set(ALL_FEATURES + [TARGET]).issubset(dataset.columns)


def test_build_real_dataset_preserves_session_duration():
    view = _mimicked_view()

    dataset = build_real_dataset(view)

    expected = view.set_index("id")["session_duration_minutes"].astype(float)

    actual = dataset.set_index("id")["session_duration_minutes"]

    assert np.allclose(actual.reindex(expected.index), expected), (
        "session_duration_minutes was recomputed instead of preserved"
    )


def test_build_real_dataset_derives_fsrs_estimate_flag():
    view = _mimicked_view()

    dataset = build_real_dataset(view)

    indexed = dataset.set_index("id")

    assert indexed["had_fsrs_estimate"].equals(
        view.set_index("id")["recall_probability"]
        .notna()
        .astype(int)
        .reindex(indexed.index)
    )

    assert set(dataset["had_fsrs_estimate"].unique()) == {0, 1}

    nan_ids = view.loc[view["recall_probability"].isna(), "id"]

    assert indexed.loc[nan_ids, "fsrs_recall_probability"].isna().all()


def test_validate_view_raises_when_extended_columns_missing():
    view = _mimicked_view().drop(columns=["difficulty"])

    with pytest.raises(ValueError, match="missing columns"):
        validate_view(view)