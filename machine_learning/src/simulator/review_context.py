from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class ReviewContext:

    # Historical information available BEFORE a review starts.


    success_rate: float

    average_confidence: float

    average_response_time: float

    average_hesitation: float

    review_interval_days: float

    repetition_number: int

