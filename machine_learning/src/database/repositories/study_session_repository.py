from __future__ import annotations

from uuid import UUID

from sqlalchemy import desc, select

from database.models.study_session import StudySession
from database.repositories.base_repository import BaseRepository


class StudySessionRepository(BaseRepository[StudySession]):
    model = StudySession

    def get_by_user(self, user_id: UUID) -> list[StudySession]:
        stmt = (
            select(StudySession)
            .where(StudySession.user_id == user_id)
            .order_by(StudySession.started_at)
        )

        return list(self.session.scalars(stmt).all())

    def get_last_session(
        self,
        user_id: UUID,
    ) -> StudySession | None:

        stmt = (
            select(StudySession)
            .where(StudySession.user_id == user_id)
            .order_by(desc(StudySession.started_at))
            .limit(1)
        )

        return self.session.scalar(stmt)