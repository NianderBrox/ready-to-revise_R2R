from __future__ import annotations

from datetime import datetime
from typing import TYPE_CHECKING
from uuid import UUID

from sqlalchemy import (
    CheckConstraint,
    DateTime,
    Enum,
    ForeignKey,
    Integer,
    String,
    text,
)
from sqlalchemy.orm import Mapped, mapped_column, relationship

from database.base import Base
from database.enums import QuestionDifficulty
from database.types import PG_UUID

if TYPE_CHECKING:
    from database.models.question_review import QuestionReview
    from database.models.user import User


class Question(Base):
    __tablename__ = "questions"

    __table_args__ = (
        CheckConstraint("word_count >= 0", name="ck_questions_word_count"),
        CheckConstraint(
            "character_count >= 0",
            name="ck_questions_character_count",
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

    subject: Mapped[str] = mapped_column(String(100))

    topic: Mapped[str] = mapped_column(String(100))

    question_difficulty: Mapped[QuestionDifficulty] = mapped_column(
        Enum(
            QuestionDifficulty,
            name="question_difficulty_enum",
            create_type=False,
        )
    )

    word_count: Mapped[int] = mapped_column(Integer)

    character_count: Mapped[int] = mapped_column(Integer)

    created_at: Mapped[datetime] = mapped_column(DateTime)

    # Relationships

    user: Mapped[User] = relationship(
        back_populates="questions"
    )

    question_reviews: Mapped[list[QuestionReview]] = relationship(
        back_populates="question"
    )

    def __repr__(self) -> str:
        return (
            f"Question("
            f"id={self.id}, "
            f"subject='{self.subject}', "
            f"topic='{self.topic}')"
        )