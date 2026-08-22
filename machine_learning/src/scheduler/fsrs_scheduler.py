from __future__ import annotations

from datetime import UTC, datetime

from fsrs import Card, Rating, Scheduler

scheduler = Scheduler()


def _as_utc(value: datetime | None) -> datetime:
    if value is None:
        return datetime.now(UTC)

    if value.tzinfo is None:
        return value.replace(tzinfo=UTC)

    return value.astimezone(UTC)


def get_retrievability(card: Card, review_time: datetime | None = None) -> float:


    return scheduler.get_card_retrievability(
        card,
        _as_utc(review_time),
    )


def schedule_review(
    card: Card,
    rating: Rating,
    review_time: datetime | None = None,
) -> dict:


    updated_card, review_log = scheduler.review_card(
        card,
        rating,
        _as_utc(review_time),
    )

    scheduled_at = _as_utc(review_time)

    interval_days = (
        updated_card.due - scheduled_at
    ).total_seconds() / 86400

    return {
        "updated_card": updated_card,
        "review_log": review_log,
        "input_rating": rating,
        "recall_probability": get_retrievability(
            card,
            scheduled_at,
        ),
        "scheduled_interval_days": max(interval_days, 0.0001),
        "next_review_at": updated_card.due,
        "fsrs_state": updated_card.state.value,
        "fsrs_step": updated_card.step,
        "fsrs_stability": updated_card.stability,
        "fsrs_difficulty": updated_card.difficulty,
        "last_review": updated_card.last_review,
    }
