from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class ConfidenceContext:
    """
    Observable behavioural signals from a single question review.

    This context is consumed by ConfidenceInference to estimate a
    continuous confidence score. It contains only values that are
    available immediately after a review is completed.
    """

    correct: bool

    response_time_seconds: float

    hesitation_seconds: float

    answer_changes: int