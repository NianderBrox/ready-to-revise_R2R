from __future__ import annotations

from typing import Generic, TypeVar
from uuid import UUID

from sqlalchemy import select
from sqlalchemy.orm import Session

T = TypeVar("T")


class BaseRepository(Generic[T]):
    model: type[T]

    def __init__(self, session: Session):
        self.session = session

    def get_all(self) -> list[T]:
        stmt = select(self.model)
        return list(self.session.scalars(stmt).all())



    def get_by_id(
        self,
        entity_id: UUID,
        ) -> T | None:

        stmt = (
            select(self.model)
            .where(self.model.id == entity_id)
        )

        return self.session.scalar(stmt)

    def create(self, entity: T) -> T:
        self.session.add(entity)
        self.session.flush()
        return entity

    def delete(self, entity: T) -> None:
        self.session.delete(entity)