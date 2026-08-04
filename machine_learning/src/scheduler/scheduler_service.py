from src.database.repositories.review_schedule_repository import (
    get_latest_schedule,
)
from src.scheduler.card_factory import (
    create_new_card,
    restore_card,
)
from src.scheduler.rating_mapper import (
    map_to_fsrs_rating,
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
    schedule = get_latest_schedule(
        # user_id=user_id,
        # question_id=question_id,
        review_id=review_id,
    )
    if schedule is None:
        card = create_new_card()
    else:
        card = restore_card(schedule)

    rating = map_to_fsrs_rating(
        correct=correct,
        confidence=confidence,
    )
