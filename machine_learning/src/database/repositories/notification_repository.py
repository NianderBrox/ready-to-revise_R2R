from __future__ import annotations

from uuid import UUID

from sqlalchemy import desc, select

from src.database.models.notification import Notification
from src.database.repositories.base_repository import BaseRepository


class NotificationRepository(BaseRepository[Notification]):
    model = Notification

    def get_by_user(
        self,
        user_id: UUID,
    ) -> list[Notification]:

        stmt = (
            select(Notification)
            .where(Notification.user_id == user_id)
            .order_by(desc(Notification.sent_at))
        )

        return list(self.session.scalars(stmt).all())