from __future__ import annotations

from simulator.config import (
    ANSWER_CHANGE_WEIGHT,
    CORRECT_WEIGHT,
    HESITATION_WEIGHT,
    HIGH_CONFIDENCE_THRESHOLD,
    LOW_CONFIDENCE_THRESHOLD,
    RESPONSE_TIME_WEIGHT,
)

from simulator.confidence_context import ConfidenceContext
from simulator.confidence_result import ConfidenceResult
from simulator.enums import ConfidenceLevel
from simulator.feature_normalizer import FeatureNormalizer


class ConfidenceInference:

    @classmethod
    def infer(
        cls,
        context: ConfidenceContext,
    ) -> ConfidenceResult:
        cls._validate(context)

        score = cls._compute_score(context)

        confidence = cls._categorize(score)

        return ConfidenceResult(
            confidence_score=score,
            confidence=confidence,
        )


    @classmethod
    def _compute_score(
        cls,
        context: ConfidenceContext,
    ) -> float:

        correct_score = 1.0 if context.correct else 0.0

        response_score = (
            FeatureNormalizer.normalize_response_time(
                context.response_time_seconds
            )
        )

        hesitation_score = (
            FeatureNormalizer.normalize_hesitation(
                context.hesitation_seconds
            )
        )

        answer_change_score = (
            FeatureNormalizer.normalize_answer_changes(
                context.answer_changes
            )
        )

        score = (
            CORRECT_WEIGHT * correct_score
            + RESPONSE_TIME_WEIGHT * response_score
            + HESITATION_WEIGHT * hesitation_score
            + ANSWER_CHANGE_WEIGHT * answer_change_score
        )

        return score


    @classmethod
    def _categorize(
        cls,
        score: float,
    ) -> ConfidenceLevel:

        if score < LOW_CONFIDENCE_THRESHOLD:
            return ConfidenceLevel.LOW

        if score < HIGH_CONFIDENCE_THRESHOLD:
            return ConfidenceLevel.MEDIUM

        return ConfidenceLevel.HIGH

    @classmethod
    def _validate(
        cls,
        context: ConfidenceContext,
    ) -> None:

        if context.response_time_seconds < 0:
            raise ValueError(
                "Response time cannot be negative."
            )

        if context.hesitation_seconds < 0:
            raise ValueError(
                "Hesitation cannot be negative."
            )

        if context.answer_changes < 0:
            raise ValueError(
                "Answer changes cannot be negative."
            )
