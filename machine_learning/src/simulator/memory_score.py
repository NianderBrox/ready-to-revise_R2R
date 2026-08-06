from __future__ import annotations

from simulator.config import MEMORY_WEIGHTS
from simulator.memory_result import MemoryResult
from simulator.normalized_review_context import (
    NormalizedReviewContext,
)


class MemoryScore:

    @classmethod
    def compute(
        cls,
        context: NormalizedReviewContext,
    ) -> MemoryResult:

        cls._validate(context)

        score = cls._compute_score(context)

        return MemoryResult(
            memory_score=score,
        )

    @classmethod
    def _compute_score(
        cls,
        context: NormalizedReviewContext,
    ) -> float:

        response_component = (
            1.0 - context.average_response_time
        )

        hesitation_component = (
            1.0 - context.average_hesitation
        )

        interval_component = (
            1.0 - context.review_interval_days
        )

        score = (

            MEMORY_WEIGHTS.success_rate
            * context.success_rate

            + MEMORY_WEIGHTS.average_confidence
            * context.average_confidence

            + MEMORY_WEIGHTS.average_response_time
            * response_component

            + MEMORY_WEIGHTS.average_hesitation
            * hesitation_component

            + MEMORY_WEIGHTS.review_interval_days
            * interval_component

            + MEMORY_WEIGHTS.repetition_number
            * context.repetition_number

        )

        return score

    @classmethod
    def _validate(
        cls,
        context: NormalizedReviewContext,
    ) -> None:

        values = {

            "success_rate":
                context.success_rate,

            "average_confidence":
                context.average_confidence,

            "average_response_time":
                context.average_response_time,

            "average_hesitation":
                context.average_hesitation,

            "review_interval_days":
                context.review_interval_days,

            "repetition_number":
                context.repetition_number,

        }

        for name, value in values.items():

            if not 0.0 <= value <= 1.0:

                raise ValueError(
                    f"{name} must be between 0 and 1. "
                    f"Received {value}."
                )