
from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
from random import Random
from uuid import UUID, uuid4

from src.data_generation.config import (
    DIFFICULTY_WEIGHTS,
    SUBJECT_TOPICS,
    TIMEZONES,
    WORD_COUNT_PROFILES,
)


@dataclass(frozen=True)
class UserProfile:

    user_id: UUID

    timezone: str

    preferred_hour: int

    weekend_preferred_hour: int

    account_created_at_days_before_start: int


@dataclass(frozen=True)
class QuestionMeta:

    question_id: UUID

    subject: str

    topic: str

    difficulty: str

    word_count: int

    character_count: int

    created_at: datetime


def make_user_profile(
    rng: Random,
    timezone: str,
) -> UserProfile:
    user_id = uuid4()

    habit = rng.random()

    if habit < 0.70:
        preferred_hour = rng.randint(17, 22)

    elif habit < 0.90:
        preferred_hour = rng.randint(6, 9)

    else:
        preferred_hour = rng.randint(13, 16)

    return UserProfile(
        user_id=user_id,
        timezone=timezone,
        preferred_hour=preferred_hour,
        weekend_preferred_hour=rng.randint(9, 21),
        account_created_at_days_before_start=rng.randint(3, 30),
    )


def pick_timezone(rng: Random) -> str:
    return rng.choice(TIMEZONES)


def _pick_difficulty(rng: Random) -> str:
    roll = rng.random()

    cumulative = 0.0

    for difficulty, weight in DIFFICULTY_WEIGHTS:
        cumulative += weight

        if roll <= cumulative:
            return difficulty

    return "MEDIUM"


def _sample_word_count(
    rng: Random,
    difficulty: str,
) -> int:
    mean, std, minimum, maximum = WORD_COUNT_PROFILES[difficulty]

    value = rng.gauss(mean, std)

    return int(max(minimum, min(maximum, value)))


def make_question_pool(
    rng: Random,
    user_id: UUID,
    n_questions: int,
    created_at_fn,
) -> tuple[list[dict], list[QuestionMeta]]:


    subjects = list(SUBJECT_TOPICS.keys())

    rows: list[dict] = []

    meta: list[QuestionMeta] = []

    for index in range(n_questions):
        subject = rng.choice(subjects)

        topic = rng.choice(SUBJECT_TOPICS[subject])

        difficulty = _pick_difficulty(rng)

        word_count = _sample_word_count(rng, difficulty)

        character_count = int(word_count * rng.gauss(5.8, 0.7))

        created_at = created_at_fn(index)

        question_id = uuid4()

        rows.append(
            {
                "id": question_id,
                "user_id": user_id,
                "subject": subject,
                "topic": topic,
                "question_difficulty": difficulty,
                "word_count": word_count,
                "character_count": max(character_count, word_count),
                "created_at": created_at,
            }
        )

        meta.append(
            QuestionMeta(
                question_id=question_id,
                subject=subject,
                topic=topic,
                difficulty=difficulty,
                word_count=word_count,
                character_count=max(character_count, word_count),
                created_at=created_at,
            )
        )

    return rows, meta
