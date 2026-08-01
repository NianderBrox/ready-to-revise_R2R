from __future__ import annotations

from datetime import datetime
from typing import TYPE_CHECKING
from uuid import UUID

from sqlalchemy import ForeignKey, String
from database.types import PG_UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship
from sqlalchemy import text
from database.base import Base
from sqlalchemy import DateTime

if TYPE_CHECKING:
    from database.models.question import Question
    from database.models.study_session import StudySession
    from database.models.question_review import QuestionReview
    from database.models.notification import Notification


class User(Base):
    __tablename__ = "users"

    id: Mapped[UUID] = mapped_column(
        PG_UUID(as_uuid=True),
        primary_key=True,
        server_default=text("gen_random_uuid()"),
    )
    user_id: Mapped[UUID] = mapped_column(
        PG_UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="CASCADE"),
    )
    created_at: Mapped[datetime] = mapped_column(DateTime)

    timezone: Mapped[str] = mapped_column(String(50))

    questions: Mapped[list["Question"]] = relationship(
        back_populates="user"
    )

    study_sessions: Mapped[list["StudySession"]] = relationship(
        back_populates="user"
    )

    question_reviews: Mapped[list["QuestionReview"]] = relationship(
        back_populates="user"
    )

    notifications: Mapped[list["Notification"]] = relationship(
        back_populates="user"
    )

    def __repr__(self) -> str:
        return f"User(id={self.id})"