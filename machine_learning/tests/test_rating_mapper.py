"""
Rating mapping tests (behavioral outcome -> FSRS / DB enums).
"""

from __future__ import annotations

import pytest
from fsrs import Rating

from src.database.enums import SchedulerRating
from src.scheduler.rating_mapper import (
    map_to_fsrs_rating,
    map_to_scheduler_rating,
)


@pytest.mark.parametrize(
    ("correct", "confidence", "expected"),
    [
        (False, "HIGH", Rating.Again),
        (False, "LOW", Rating.Again),
        (True, "LOW", Rating.Hard),
        (True, "MEDIUM", Rating.Good),
        (True, "HIGH", Rating.Easy),
    ],
)
def test_map_to_fsrs_rating(correct, confidence, expected):
    assert map_to_fsrs_rating(correct, confidence) is expected


@pytest.mark.parametrize(
    ("correct", "confidence", "expected"),
    [
        (False, "high", SchedulerRating.AGAIN),
        (True, "low", SchedulerRating.HARD),
        (True, "medium", SchedulerRating.GOOD),
        (True, "High", SchedulerRating.EASY),
    ],
)
def test_map_to_scheduler_rating(correct, confidence, expected):
    assert map_to_scheduler_rating(correct, confidence) is expected


def test_incorrect_always_maps_to_again():
    for confidence in ("LOW", "MEDIUM", "HIGH"):
        assert (
            map_to_scheduler_rating(False, confidence)
            is SchedulerRating.AGAIN
        )


def test_unknown_confidence_raises():
    with pytest.raises(ValueError):
        map_to_fsrs_rating(True, "PERFECT")

    with pytest.raises(ValueError):
        map_to_scheduler_rating(True, "PERFECT")
