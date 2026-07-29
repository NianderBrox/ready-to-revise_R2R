from src.database.review_schedule_repository import (
    get_latest_review_schedule,
    save_review_schedule,
)

from src.scheduler.card_factory import (
    create_new_card,
    restore_card,
)

from src.scheduler.confidence_mapper import (
    map_confidence_to_rating,
)

from src.scheduler.fsrs_scheduler import (
    review_card,
)


def schedule_review(
    user_id,
    question_id,
    review_id,
    correct,
    confidence,
):
    """
    Updates the review schedule after a question review.
    """
    schedule = get_latest_review_schedule(
        user_id=user_id,
        question_id=question_id,
    )
    if schedule is None:
        card = create_new_card()
    else:
        card = restore_card(schedule)

    rating = map_confidence_to_rating(
        correct=correct,
        confidence=confidence,
    )
