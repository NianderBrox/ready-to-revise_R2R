from __future__ import annotations

from uuid import UUID

from sqlalchemy import desc, select

from src.database.models.question_review import QuestionReview
from src.database.repositories.base_repository import BaseRepository


class QuestionReviewRepository(BaseRepository[QuestionReview]):
    model = QuestionReview

    def get_by_user(
        self,
        user_id: UUID,
    ) -> list[QuestionReview]:
        stmt = (
            select(QuestionReview)
            .where(QuestionReview.user_id == user_id)
            .order_by(QuestionReview.review_time)
        )

        return list(self.session.scalars(stmt).all())

    def get_by_question(
        self,
        question_id: UUID,
    ) -> list[QuestionReview]:
        stmt = (
            select(QuestionReview)
            .where(QuestionReview.question_id == question_id)
            .order_by(QuestionReview.review_time)
        )

        return list(self.session.scalars(stmt).all())

    def get_last_review(
        self,
        user_id: UUID,
        question_id: UUID,
    ) -> QuestionReview | None:

        stmt = (
            select(QuestionReview)
            .where(
                QuestionReview.user_id == user_id,
                QuestionReview.question_id == question_id,
            )
            .order_by(desc(QuestionReview.review_time))
            .limit(1)
        )

        return self.session.scalar(stmt)