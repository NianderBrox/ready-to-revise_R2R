from __future__ import annotations

from random import Random

from src.simulator.confidence_context import ConfidenceContext
from src.simulator.confidence_inference import ConfidenceInference
from src.simulator.config import (
    ANSWER_CHANGE_HESITATION_WEIGHT,
    ANSWER_CHANGE_MEMORY_WEIGHT,
    HESITATION_CONFIG,
    OUTCOME_SHARPNESS_K,
    RESPONSE_TIME_CONFIG,
    SIMULATION_LIMITS,
)
from src.simulator.feature_normalizer import FeatureNormalizer
from src.simulator.memory_result import MemoryResult
from src.simulator.memory_score import MemoryScore
from src.simulator.review_context import ReviewContext
from src.simulator.review_outcome import ReviewOutcome


class BehavioralModel:


    def __init__(self, rng: Random):
        self.rng = rng

    def simulate(
        self,
        context: ReviewContext,
    ) -> ReviewOutcome:
        normalized_context = (
            FeatureNormalizer.normalize_review_context(
                context,
            )
        )

        memory_result = (
            MemoryScore.compute(
                normalized_context,
            )
        )

        correct = self._simulate_correct(memory_result)

        response_time = self._simulate_response_time(
            memory_result,
        )

        hesitation = self._simulate_hesitation(
            memory_result,
            response_time,
        )


        answer_changes = self._simulate_answer_changes(
            memory_result,
            hesitation,
            response_time,
        )

        confidence_context = ConfidenceContext(
            correct=correct,
            response_time_seconds=response_time,
            hesitation_seconds=hesitation,
            answer_changes=answer_changes,
        )

        confidence_result = ConfidenceInference.infer(
            confidence_context
        )

        
        return ReviewOutcome(
            correct=correct,
            confidence=confidence_result.confidence,
            confidence_score=confidence_result.confidence_score,
            response_time_seconds=response_time,
            hesitation_seconds=hesitation,
            answer_changes=answer_changes,
        )

    def _simulate_correct(
        self,
        memory_result: MemoryResult,
        ) -> bool:
        probability = 0.5 + OUTCOME_SHARPNESS_K * (
            memory_result.memory_score - 0.5
        )

        probability = max(0.0, min(1.0, probability))

        return self.rng.random() < probability

    def _simulate_response_time(
        self,
        memory_result: MemoryResult,
    ) -> float:
        memory = memory_result.memory_score

        expected = (
            RESPONSE_TIME_CONFIG.min_seconds
            +
            (1.0 - memory)
            * (
                SIMULATION_LIMITS.max_response_time
                - RESPONSE_TIME_CONFIG.min_seconds
            )
        )

        std_dev = (
            RESPONSE_TIME_CONFIG.base_std_seconds
            +
            (1.0 - memory)
            * (
                RESPONSE_TIME_CONFIG.max_std_seconds
                - RESPONSE_TIME_CONFIG.base_std_seconds
            )
        )

        response_time = self.rng.gauss(
            expected,
            std_dev,
        )

        response_time = max(
            RESPONSE_TIME_CONFIG.min_seconds,
            response_time,
        )

        response_time = min(
            SIMULATION_LIMITS.max_response_time,
            response_time,
        )

        return response_time

    def _simulate_hesitation(
        self,
        memory_result: MemoryResult,
        response_time: float,
        ) -> float:

        memory = memory_result.memory_score

        expected_ratio = (
            HESITATION_CONFIG.min_ratio
            +
            (1.0 - memory)
            * (
                HESITATION_CONFIG.max_ratio
                - HESITATION_CONFIG.min_ratio
            )
        )

        ratio = self.rng.gauss(
            expected_ratio,
            HESITATION_CONFIG.ratio_std,
        )

        ratio = max(0.0, min(1.0, ratio))

        hesitation = ratio * response_time

        hesitation = min(
            SIMULATION_LIMITS.max_hesitation,
            hesitation,
        )

        return hesitation

    def _simulate_answer_changes(
        self,
        memory_result: MemoryResult,
        hesitation_seconds: float,
        response_time_seconds: float,
    ) -> int:

        memory = memory_result.memory_score

        if response_time_seconds <= 0:
            raise ValueError(
                "Response time must be greater than zero."
            )

        hesitation_ratio = (
            hesitation_seconds
            / response_time_seconds
        )

        memory_uncertainty = 1.0 - memory

        change_probability = (
            ANSWER_CHANGE_MEMORY_WEIGHT
            * memory_uncertainty
            +
            ANSWER_CHANGE_HESITATION_WEIGHT
            * hesitation_ratio
        )

        change_probability = max(
            0.0,
            min(change_probability, 1.0),
        )

        answer_changes = 0

        for _ in range(
            SIMULATION_LIMITS.max_answer_changes
        ):
            if self.rng.random() < change_probability:
                answer_changes += 1
            else:
                break

        return answer_changes