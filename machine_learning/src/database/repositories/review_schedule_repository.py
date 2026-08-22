from __future__ import annotations

from datetime import datetime
from uuid import UUID

from sqlalchemy import desc, select

from src.database.models.question_review import QuestionReview
from src.database.models.review_schedule import ReviewSchedule
from src.database.repositories.base_repository import BaseRepository


class ReviewScheduleRepository(BaseRepository[ReviewSchedule]):
    model = ReviewSchedule

    def get_by_review(
        self,
        review_id: UUID,
    ) -> list[ReviewSchedule]:

        stmt = (
            select(ReviewSchedule)
            .where(
                ReviewSchedule.review_id == review_id
            )
            .order_by(
                desc(ReviewSchedule.created_at)
            )
        )

        return list(
            self.session.scalars(stmt).all()
        )

    def get_latest_schedule(
        self,
        review_id: UUID,
    ) -> ReviewSchedule | None:

        stmt = (
            select(ReviewSchedule)
            .where(
                ReviewSchedule.review_id == review_id
            )
            .order_by(
                desc(ReviewSchedule.created_at)
            )
            .limit(1)
        )

        return self.session.scalar(stmt)

    def get_latest_schedule_before_review(
        self,
        user_id: UUID,
        question_id: UUID,
        review_time: datetime,
    ) -> ReviewSchedule | None:


        stmt = (
            select(ReviewSchedule)
            .join(
                QuestionReview,
                ReviewSchedule.review_id
                == QuestionReview.id,
            )
            .where(
                QuestionReview.user_id == user_id,
                QuestionReview.question_id == question_id,
                QuestionReview.review_time < review_time,
            )
            .order_by(
                desc(QuestionReview.review_time),
                desc(ReviewSchedule.created_at),
            )
            .limit(1)
        )

        return self.session.scalar(stmt)

