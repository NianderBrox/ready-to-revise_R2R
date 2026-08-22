from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class ConfidenceContext:


    correct: bool

    response_time_seconds: float

    hesitation_seconds: float

    answer_changes: int