from __future__ import annotations

from datetime import datetime
from uuid import UUID

from src.database.models.review_schedule import ReviewSchedule
from src.database.repositories.review_schedule_repository import (
    ReviewScheduleRepository,
)
from src.scheduler.card_factory import (
    create_new_card,
    restore_card,
)
from src.scheduler.fsrs_scheduler import schedule_review
from src.scheduler.rating_mapper import (
    map_to_fsrs_rating,
    map_to_scheduler_rating,
)

SCHEDULER_NAME = "fsrs"


class SchedulerService:


    def __init__(
        self,
        schedule_repository: ReviewScheduleRepository,
    ):
        self.repository = schedule_repository

    def schedule_review(
        self,
        *,
        user_id: UUID,
        question_id: UUID,
        review_id: UUID,
        correct: bool,
        confidence: str,
        review_time: datetime,
        previous_schedule: ReviewSchedule | None = None,
    ) -> ReviewSchedule:
        if previous_schedule is None:
            previous_schedule = (
                self.repository.get_latest_schedule_before_review(
                    user_id=user_id,
                    question_id=question_id,
                    review_time=review_time,
                )
            )

        if previous_schedule is None:
            card = create_new_card()
        else:
            card = restore_card(previous_schedule)

        rating = map_to_fsrs_rating(
            correct=correct,
            confidence=confidence,
        )

        result = schedule_review(
            card=card,
            rating=rating,
            review_time=review_time,
        )

        record = ReviewSchedule(
            review_id=review_id,
            scheduler_name=SCHEDULER_NAME,
            input_rating=map_to_scheduler_rating(
                correct=correct,
                confidence=confidence,
            ),
            recall_probability=result["recall_probability"],
            scheduled_interval_days=result["scheduled_interval_days"],
            next_review_at=result["next_review_at"].replace(tzinfo=None),
            fsrs_state=result["fsrs_state"],
            fsrs_step=result["fsrs_step"],
            fsrs_stability=result["fsrs_stability"],
            fsrs_difficulty=result["fsrs_difficulty"],
            created_at=review_time.replace(tzinfo=None),
        )

        self.repository.create(record)

        return record
