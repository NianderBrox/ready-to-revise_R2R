from __future__ import annotations

from datetime import datetime
from typing import TYPE_CHECKING
from uuid import UUID

from sqlalchemy import DateTime, String, text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from src.database.base import Base
from src.database.types import PG_UUID

if TYPE_CHECKING:
    from src.database.models.notification import Notification
    from src.database.models.question import Question
    from src.database.models.question_review import QuestionReview
    from src.database.models.study_session import StudySession


class User(Base):
    __tablename__ = "users"

    id: Mapped[UUID] = mapped_column(
        PG_UUID,
        primary_key=True,
        server_default=text("gen_random_uuid()"),
    )

    created_at: Mapped[datetime] = mapped_column(
        DateTime
    )

    timezone: Mapped[str] = mapped_column(
        String(50)
    )

    questions: Mapped[list[Question]] = relationship(
        back_populates="user"
    )

    study_sessions: Mapped[list[StudySession]] = relationship(
        back_populates="user"
    )

    question_reviews: Mapped[list[QuestionReview]] = relationship(
        back_populates="user"
    )

    notifications: Mapped[list[Notification]] = relationship(
        back_populates="user"
    )

    def __repr__(self) -> str:
        return f"User(id={self.id})"