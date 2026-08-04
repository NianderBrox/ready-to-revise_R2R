from __future__ import annotations

from datetime import datetime
from typing import TYPE_CHECKING
from uuid import UUID

from sqlalchemy import (
    CheckConstraint,
    DateTime,
    Enum,
    Float,
    ForeignKey,
    SmallInteger,
    String,
    UniqueConstraint,
    text,
)
from sqlalchemy.orm import Mapped, mapped_column, relationship

from database.base import Base
from database.enums import SchedulerRating
from database.types import PG_UUID

if TYPE_CHECKING:
    from database.models.question_review import QuestionReview


class ReviewSchedule(Base):
    __tablename__ = "review_schedules"

    __table_args__ = (
        UniqueConstraint(
            "review_id",
            "scheduler_name",
            name="uq_review_scheduler",
        ),
        CheckConstraint(
            "scheduled_interval_days > 0",
            name="ck_schedule_interval",
        ),
        CheckConstraint(
            "recall_probability IS NULL OR "
            "(recall_probability BETWEEN 0 AND 1)",
            name="ck_schedule_recall_probability",
        ),
    )

    id: Mapped[UUID] = mapped_column(
        PG_UUID(as_uuid=True),
        primary_key=True,
        server_default=text("gen_random_uuid()"),
    )

    review_id: Mapped[UUID] = mapped_column(
        PG_UUID(as_uuid=True),
        ForeignKey("question_reviews.id", ondelete="CASCADE"),
    )

    scheduler_name: Mapped[str] = mapped_column(
        String(50)
    )

    input_rating: Mapped[SchedulerRating | None] = mapped_column(
        Enum(
            SchedulerRating,
            name="scheduler_rating_enum",
            create_type=False,
        ),
        nullable=True,
    )

    recall_probability: Mapped[float | None] = mapped_column(
        Float,
        nullable=True,
    )

    scheduled_interval_days: Mapped[float] = mapped_column(
        Float
    )

    next_review_at: Mapped[datetime] = mapped_column(
        DateTime
    )

    fsrs_state: Mapped[int | None] = mapped_column(
        SmallInteger,
        nullable=True,
    )

    fsrs_step: Mapped[int | None] = mapped_column(
        SmallInteger,
        nullable=True,
    )

    fsrs_stability: Mapped[float | None] = mapped_column(
        Float,
        nullable=True,
    )

    fsrs_difficulty: Mapped[float | None] = mapped_column(
        Float,
        nullable=True,
    )

    created_at: Mapped[datetime] = mapped_column(
        DateTime
    )

    # Relationships

    question_review: Mapped[QuestionReview] = relationship(
        back_populates="review_schedules"
    )
    def __repr__(self) -> str:
        return (
            f"ReviewSchedule("
            f"id={self.id}, "
            f"scheduler='{self.scheduler_name}')"
        )