

from __future__ import annotations

from datetime import UTC, datetime
from random import Random
from uuid import UUID, uuid4

from fsrs import Card

from src.data_generation.config import (
    DEFAULT_AVERAGE_CONFIDENCE,
    DEFAULT_AVERAGE_HESITATION,
    DEFAULT_AVERAGE_RESPONSE_TIME,
    DEFAULT_SUCCESS_RATE,
)
from src.data_generation.profiles import QuestionMeta
from src.data_generation.review_history import QuestionHistory
from src.scheduler.fsrs_scheduler import schedule_review
from src.scheduler.rating_mapper import (
    map_to_fsrs_rating,
    map_to_scheduler_rating,
)
from src.simulator.behavioural_model import BehavioralModel
from src.simulator.review_context import ReviewContext


class ReviewEngine:


    def __init__(self, rng: Random):
        self.rng = rng

        self.behavioral_model = BehavioralModel(rng)

        self.cards: dict[UUID, Card] = {}

    def build_context(
        self,
        question: QuestionMeta,
        history: QuestionHistory,
        now: datetime,
    ) -> ReviewContext:
        if history.review_count == 0:
            success_rate = DEFAULT_SUCCESS_RATE

            average_confidence = DEFAULT_AVERAGE_CONFIDENCE

            average_response_time = DEFAULT_AVERAGE_RESPONSE_TIME

            average_hesitation = DEFAULT_AVERAGE_HESITATION

            interval_days = max(
                (now - question.created_at).total_seconds() / 86400,
                0.0,
            )

        else:
            success_rate = history.success_rate

            average_confidence = history.average_confidence

            average_response_time = history.average_response_time

            average_hesitation = history.average_hesitation

            interval_days = (
                now - history.last_review_time
            ).total_seconds() / 86400

        return ReviewContext(
            success_rate=success_rate,
            average_confidence=average_confidence,
            average_response_time=average_response_time,
            average_hesitation=average_hesitation,
            review_interval_days=interval_days,
            repetition_number=history.review_count,
        )

    def review_question(
        self,
        *,
        user_id: UUID,
        question: QuestionMeta,
        session_id: UUID,
        position: int,
        history: QuestionHistory,
        review_time: datetime,
    ) -> tuple[dict, dict, dict]:


        naive_review_time = _naive(review_time)

        context = self.build_context(
            question,
            history,
            naive_review_time,
        )

        outcome = self.behavioral_model.simulate(context)

        confidence = outcome.confidence.value

        response_time = round(
            outcome.response_time_seconds, 2
        )

        hesitation = round(outcome.hesitation_seconds, 2)

        previous_review_time = (
            history.last_review_time
            if history.review_count > 0
            else None
        )

        repetition_number = history.review_count

        review_id = uuid4()

        review_row = {
            "id": review_id,
            "user_id": user_id,
            "question_id": question.question_id,
            "session_id": session_id,
            "review_time": naive_review_time,
            "previous_review_time": previous_review_time,
            "correct": outcome.correct,
            "confidence": confidence,
            "confidence_score": outcome.confidence_score,
            "response_time_seconds": response_time,
            "hesitation_seconds": hesitation,
            "answer_changes": outcome.answer_changes,
            "repetition_number": repetition_number,
        }

        attempt_row = {
            "id": uuid4(),
            "review_id": review_id,
            "question_position": position,
        }

        schedule_row = self._schedule(
            user_id=user_id,
            question=question,
            review_id=review_id,
            correct=outcome.correct,
            confidence=confidence,
            first_review=history.review_count == 0,
            review_time=review_time,
        )

        history.update(
            correct=outcome.correct,
            confidence_score=outcome.confidence_score,
            response_time_seconds=response_time,
            hesitation_seconds=hesitation,
            review_time=naive_review_time,
        )

        return review_row, attempt_row, schedule_row

    def _schedule(
        self,
        *,
        user_id: UUID,
        question: QuestionMeta,
        review_id: UUID,
        correct: bool,
        confidence: str,
        first_review: bool,
        review_time: datetime,
    ) -> dict:
        card = self.cards.get(question.question_id)

        if card is None:
            card = Card()

        rating = map_to_fsrs_rating(
            correct=correct,
            confidence=confidence,
        )

        result = schedule_review(
            card=card,
            rating=rating,
            review_time=review_time,
        )

        self.cards[question.question_id] = result["updated_card"]

        return {
            "id": uuid4(),
            "review_id": review_id,
            "scheduler_name": "fsrs",
            "input_rating": map_to_scheduler_rating(
                correct=correct,
                confidence=confidence,
            ),
            "recall_probability": (
                None
                if first_review
                else round(result["recall_probability"], 4)
            ),
            "scheduled_interval_days": round(
                result["scheduled_interval_days"], 4
            ),
            "next_review_at": result["next_review_at"].replace(
                tzinfo=None
            ),
            "fsrs_state": result["fsrs_state"],
            "fsrs_step": result["fsrs_step"],
            "fsrs_stability": result["fsrs_stability"],
            "fsrs_difficulty": result["fsrs_difficulty"],
            "created_at": review_time.replace(tzinfo=None),
        }


def _naive(value: datetime) -> datetime:


    if value.tzinfo is None:
        return value

    return value.astimezone(UTC).replace(tzinfo=None)
