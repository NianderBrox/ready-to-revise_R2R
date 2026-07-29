from fsrs import Card

from src.scheduler.rating_mapper import map_to_fsrs_rating
from src.scheduler.fsrs_scheduler import schedule_review


def schedule_question_review(
    correct: bool,
    confidence: str,
):
    """
    Schedule one completed review.
    """

    # New card for now.
    # Later we'll load it from the database.
    card = Card()

    rating = map_to_fsrs_rating(
        correct,
        confidence,
    )

    result = schedule_review(
        card,
        rating,
    )

    return result
