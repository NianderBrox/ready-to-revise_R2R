from __future__ import annotations

from src.database.models.user import User
from src.database.repositories.base_repository import BaseRepository


class UserRepository(BaseRepository[User]):
    model = User