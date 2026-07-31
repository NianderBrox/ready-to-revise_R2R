from dataclasses import dataclass
from datetime import datetime
from simulator.enums import Difficulty
from uuid import UUID

@dataclass
class Question:
    id: UUID
    user_id: UUID

    subject: str
    topic: str

    question_difficulty: Difficulty

    word_count: int
    character_count: int

    created_at: datetime

    # Runtime only
    intrinsic_difficulty: float