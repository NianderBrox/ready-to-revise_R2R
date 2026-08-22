from __future__ import annotations

from datetime import UTC, datetime

from fsrs import Card, State

from src.database.models.review_schedule import ReviewSchedule


def create_new_card() -> Card:


    return Card()


def restore_card(schedule: ReviewSchedule) -> Card:


    if _is_unscheduled(schedule):
        return create_new_card()

    return Card(
        state=State(schedule.fsrs_state),
        step=schedule.fsrs_step,
        stability=schedule.fsrs_stability,
        difficulty=schedule.fsrs_difficulty,
        due=_to_utc(schedule.next_review_at),
        last_review=_to_utc(schedule.created_at),
    )


def _is_unscheduled(schedule: ReviewSchedule) -> bool:


    if (
        schedule.fsrs_state is None
        or schedule.fsrs_stability is None
        or schedule.fsrs_difficulty is None
        or schedule.next_review_at is None
    ):
        return True

    try:
        State(schedule.fsrs_state)
    except ValueError:
        return True

    return False


def _to_utc(value: datetime | None) -> datetime | None:
    if value is None:
        return None

    if value.tzinfo is None:
        return value.replace(tzinfo=UTC)

    return value.astimezone(UTC)
