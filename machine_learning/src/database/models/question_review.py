from __future__ import annotations

from datetime import datetime
from typing import TYPE_CHECKING
from uuid import UUID

from sqlalchemy import (
    Boolean,
    CheckConstraint,
    DateTime,
    Enum,
    Float,
    ForeignKey,
    Integer,
    SmallInteger,
    text,
)
from sqlalchemy.orm import Mapped, mapped_column, relationship

from database.base import Base
from database.enums import ConfidenceLevel
from database.types import PG_UUID

if TYPE_CHECKING:
    from database.models.question import Question
    from database.models.question_attempt import QuestionAttempt
    from database.models.review_schedule import ReviewSchedule
    from database.models.study_session import StudySession
    from database.models.user import User


class QuestionReview(Base):
    __tablename__ = "question_reviews"

    __table_args__ = (
        CheckConstraint(
            "response_time_seconds >= 0",
            name="ck_review_response_time",
        ),
        CheckConstraint(
            "hesitation_seconds >= 0",
            name="ck_review_hesitation",
        ),
        CheckConstraint(
            "answer_changes >= 0",
            name="ck_review_answer_changes",
        ),
        CheckConstraint(
            "repetition_number >= 0",
            name="ck_review_repetition_number",
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

    question_id: Mapped[UUID] = mapped_column(
        PG_UUID(as_uuid=True),
        ForeignKey("questions.id", ondelete="CASCADE"),
    )

    session_id: Mapped[UUID] = mapped_column(
        PG_UUID(as_uuid=True),
        ForeignKey("study_sessions.id", ondelete="CASCADE"),
    )

    review_time: Mapped[datetime] = mapped_column(DateTime)

    previous_review_time: Mapped[datetime | None] = mapped_column(
        DateTime,
        nullable=True,
    )

    correct: Mapped[bool] = mapped_column(Boolean)

    confidence: Mapped[ConfidenceLevel] = mapped_column(
        Enum(
            ConfidenceLevel,
            name="confidence_level_enum",
            create_type=False,
        )
    )

    response_time_seconds: Mapped[float] = mapped_column(Float)

    hesitation_seconds: Mapped[float] = mapped_column(Float)

    answer_changes: Mapped[int] = mapped_column(SmallInteger)

    repetition_number: Mapped[int] = mapped_column(Integer)

    # Relationships

    user: Mapped[User] = relationship(
        back_populates="question_reviews"
    )

    question: Mapped[Question] = relationship(
        back_populates="question_reviews"
    )

    study_session: Mapped[StudySession] = relationship(
        back_populates="question_reviews"
    )

    question_attempt: Mapped[QuestionAttempt] = relationship(
        back_populates="question_review",
        uselist=False,
    )

    review_schedules: Mapped[list[ReviewSchedule]] = relationship(
        back_populates="question_review"
    )

    def __repr__(self) -> str:
        return (
            f"QuestionReview("
            f"id={self.id}, "
            f"question_id={self.question_id}, "
            f"correct={self.correct})"
        )