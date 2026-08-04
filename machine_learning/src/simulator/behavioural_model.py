from __future__ import annotations

from random import Random

from simulator.review_context import ReviewContext
from simulator.review_outcome import ReviewOutcome
from simulator.confidence_result import ConfidenceResult
from src.simulator.confidence_inference import ConfidenceContext, ConfidenceInference


class BehavioralModel:


    def __init__(self, rng: Random):
        self.rng = rng

    def simulate(
        self,
        context: ReviewContext,
    ) -> ReviewOutcome:

        correct = self._simulate_correct(context)

        response_time = self._simulate_response_time(
            context,
            correct,
        )

        hesitation = self._simulate_hesitation(
            context,
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
        context: ReviewContext,
    ) -> bool:
        raise NotImplementedError

    def _simulate_response_time(
        self,
        context: ReviewContext,
        correct: bool,
    ) -> float:
        raise NotImplementedError

    def _simulate_hesitation(
        self,
        context: ReviewContext,
        response_time: float,
    ) -> float:
        raise NotImplementedError

    def _simulate_answer_changes(
        self,
        hesitation: float,
    ) -> int:
        raise NotImplementedError