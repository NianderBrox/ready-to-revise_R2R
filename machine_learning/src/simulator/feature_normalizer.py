from __future__ import annotations

from src.simulator.config import SIMULATION_LIMITS
from src.simulator.normalized_review_context import (
    NormalizedReviewContext,
)
from src.simulator.review_context import ReviewContext


class FeatureNormalizer:

    # Memory model normalization

    @staticmethod
    def normalize_review_context(
        context: ReviewContext,
    ) -> NormalizedReviewContext:

        return NormalizedReviewContext(

            success_rate=context.success_rate,

            average_confidence=FeatureNormalizer._normalize_confidence(
                context.average_confidence
            ),

            average_response_time=min(
                context.average_response_time
                / SIMULATION_LIMITS.max_response_time,
                1.0,
            ),

            average_hesitation=min(
                context.average_hesitation
                / SIMULATION_LIMITS.max_hesitation,
                1.0,
            ),

            review_interval_days=min(
                context.review_interval_days
                / SIMULATION_LIMITS.max_review_interval_days,
                1.0,
            ),

            repetition_number=min(
                context.repetition_number
                / SIMULATION_LIMITS.max_repetition_number,
                1.0,
            ),
        )

    @staticmethod
    def _normalize_confidence(
        confidence: float,
    ) -> float:

        return max(
            0.0,
            min(confidence, 1.0),
        )

    # Confidence inference normalization

    @staticmethod
    def normalize_response_time(
        response_time_seconds: float,
    ) -> float:

        return 1.0 - min(
            response_time_seconds
            / SIMULATION_LIMITS.max_response_time,
            1.0,
        )

    @staticmethod
    def normalize_hesitation(
        hesitation_seconds: float,
    ) -> float:

        return 1.0 - min(
            hesitation_seconds
            / SIMULATION_LIMITS.max_hesitation,
            1.0,
        )

    @staticmethod
    def normalize_answer_changes(
        answer_changes: int,
    ) -> float:

        return 1.0 - min(
            answer_changes
            / SIMULATION_LIMITS.max_answer_changes,
            1.0,
        )