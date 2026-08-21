from __future__ import annotations

from typing import TYPE_CHECKING
from uuid import UUID

from sqlalchemy import CheckConstraint, ForeignKey, Integer, text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from src.database.base import Base
from src.database.types import PG_UUID

if TYPE_CHECKING:
    from src.database.models.question_review import QuestionReview


class QuestionAttempt(Base):
    __tablename__ = "question_attempts"

    __table_args__ = (
        CheckConstraint(
            "question_position > 0",
            name="ck_question_attempt_position",
        ),
    )

    id: Mapped[UUID] = mapped_column(
        PG_UUID,
        primary_key=True,
        server_default=text("gen_random_uuid()"),
    )

    review_id: Mapped[UUID] = mapped_column(
        PG_UUID,
        ForeignKey("question_reviews.id", ondelete="CASCADE"),
    )

    question_position: Mapped[int] = mapped_column(Integer)

    # Relationships

    question_review: Mapped[QuestionReview] = relationship(
        back_populates="question_attempt"
    )

    def __repr__(self) -> str:
        return (
            f"QuestionAttempt("
            f"id={self.id}, "
            f"position={self.question_position})"
        )