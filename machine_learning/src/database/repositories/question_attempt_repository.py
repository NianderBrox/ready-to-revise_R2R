from __future__ import annotations

from uuid import UUID

from sqlalchemy import select

from src.database.models.question_attempt import QuestionAttempt
from src.database.repositories.base_repository import BaseRepository


class QuestionAttemptRepository(BaseRepository[QuestionAttempt]):
    model = QuestionAttempt

    def get_by_review(
        self,
        review_id: UUID,
    ) -> QuestionAttempt | None:

        stmt = (
            select(QuestionAttempt)
            .where(QuestionAttempt.review_id == review_id)
        )

        return self.session.scalar(stmt)