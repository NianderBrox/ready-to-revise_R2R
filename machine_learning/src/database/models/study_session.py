from __future__ import annotations

from datetime import datetime
from typing import TYPE_CHECKING
from uuid import UUID

from sqlalchemy import CheckConstraint, DateTime, ForeignKey, text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from database.base import Base
from database.types import PG_UUID

if TYPE_CHECKING:
    from database.models.question_review import QuestionReview
    from database.models.user import User


class StudySession(Base):
    __tablename__ = "study_sessions"

    __table_args__ = (
        CheckConstraint(
            "ended_at >= started_at",
            name="ck_study_sessions_time_order",
        ),
    )

    id: Mapped[UUID] = mapped_column(
        PG_UUID(as_uuid=True),
        primary_key=True,
        server_default=text("gen_random_uuid()"),
    )

    user_id: Mapped[UUID] = mapped_column(
        PG_UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="CASCADE"),
    )

    started_at: Mapped[datetime] = mapped_column(DateTime)

    ended_at: Mapped[datetime] = mapped_column(DateTime)

    # Relationships

    user: Mapped[User] = relationship(
        back_populates="study_sessions"
    )

    question_reviews: Mapped[list[QuestionReview]] = relationship(
        back_populates="study_session"
    )

    def __repr__(self) -> str:
        return (
            f"StudySession("
            f"id={self.id}, "
            f"user_id={self.user_id})"
        )