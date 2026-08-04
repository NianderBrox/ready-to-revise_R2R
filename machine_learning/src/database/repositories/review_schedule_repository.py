from __future__ import annotations

from uuid import UUID

from sqlalchemy import desc, select

from database.models.review_schedule import ReviewSchedule
from database.repositories.base_repository import BaseRepository


class ReviewScheduleRepository(BaseRepository[ReviewSchedule]):
    model = ReviewSchedule

    def get_by_review(
        self,
        review_id: UUID,
    ) -> list[ReviewSchedule]:

        stmt = (
            select(ReviewSchedule)
            .where(ReviewSchedule.review_id == review_id)
            .order_by(desc(ReviewSchedule.created_at))
        )

        return list(self.session.scalars(stmt).all())

    def get_latest_schedule(
        self,
        review_id: UUID,
    ) -> ReviewSchedule | None:

        stmt = (
            select(ReviewSchedule)
            .where(ReviewSchedule.review_id == review_id)
            .order_by(desc(ReviewSchedule.created_at))
            .limit(1)
        )

        return self.session.scalar(stmt)