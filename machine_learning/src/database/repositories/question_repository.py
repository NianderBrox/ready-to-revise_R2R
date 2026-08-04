from __future__ import annotations

from uuid import UUID

from sqlalchemy import select

from database.models.question import Question
from database.repositories.base_repository import BaseRepository


class QuestionRepository(BaseRepository[Question]):
    model = Question

    def get_by_user(self, user_id: UUID) -> list[Question]:
        stmt = (
            select(Question)
            .where(Question.user_id == user_id)
        )

        return list(self.session.scalars(stmt).all())