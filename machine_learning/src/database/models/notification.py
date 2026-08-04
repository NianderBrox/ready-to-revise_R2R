from __future__ import annotations

from datetime import datetime
from typing import TYPE_CHECKING
from uuid import UUID

from sqlalchemy import Boolean, DateTime, ForeignKey, text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from database.base import Base
from database.types import PG_UUID

if TYPE_CHECKING:
    from database.models.user import User


class Notification(Base):
    __tablename__ = "notifications"

    id: Mapped[UUID] = mapped_column(
        PG_UUID(as_uuid=True),
        primary_key=True,
        server_default=text("gen_random_uuid()"),
    )

    user_id: Mapped[UUID] = mapped_column(
        PG_UUID(as_uuid=True),
        ForeignKey("users.id", ondelete="CASCADE"),
    )

    sent_at: Mapped[datetime] = mapped_column(
        DateTime
    )

    opened: Mapped[bool] = mapped_column(
        Boolean
    )

    opened_at: Mapped[datetime | None] = mapped_column(
        DateTime,
        nullable=True,
    )

    # Relationships

    user: Mapped[User] = relationship(
        back_populates="notifications"
    )

    def __repr__(self) -> str:
        return (
            f"Notification("
            f"id={self.id}, "
            f"user_id={self.user_id}, "
            f"opened={self.opened})"
        )