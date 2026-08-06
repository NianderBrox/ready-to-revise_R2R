from __future__ import annotations
from random import Random

from simulator.review_context import ReviewContext
from simulator.review_outcome import ReviewOutcome
from simulator.confidence_result import ConfidenceResult
from src.simulator.confidence_inference import ConfidenceContext, ConfidenceInference
from simulator.feature_normalizer import FeatureNormalizer
from simulator.memory_score import MemoryScore
from src.simulator.config import RESPONSE_TIME_CONFIG,SIMULATION_LIMITS,HESITATION_CONFIG
from src.simulator.memory_result import MemoryResult

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

        memory_score = (
            memory_result.memory_score
        )

        correct = self._simulate_correct(memory_result)

        response_time = self._simulate_response_time(
            memory_result,
            correct,
        )

        hesitation = self._simulate_hesitation(
            memory_result,
            response_time,
        )

        answer_changes = self._simulate_answer_changes(
            hesitation,
        )

        context = ConfidenceContext(
            correct=correct,
            response_time_seconds=response_time,
            hesitation_seconds=hesitation,
            answer_changes=answer_changes,
        )

        confidence_result = ConfidenceInference.infer(
            ConfidenceContext(
                correct=correct,
                response_time_seconds=response_time,
                hesitation_seconds=hesitation,
                answer_changes=answer_changes,
            )
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
        return (
            self.rng.random()
            < memory_result.memory_score
        )

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

        return ratio * response_time

    def _simulate_answer_changes(
        self,
        hesitation: float,
    ) -> int:
        raise NotImplementedError