from __future__ import annotations

from dataclasses import dataclass

from simulator.enums import ConfidenceLevel


@dataclass(frozen=True, slots=True)
class ReviewOutcome:

    correct: bool

    confidence_score: float

    confidence: ConfidenceLevel

    response_time_seconds: float

    hesitation_seconds: float

    answer_changes: int