from __future__ import annotations

from database.models.user import User
from database.repositories.base_repository import BaseRepository


class UserRepository(BaseRepository[User]):
    model = User