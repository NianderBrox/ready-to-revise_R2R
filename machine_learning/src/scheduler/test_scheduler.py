from src.scheduler.card_repository import create_new_card
from src.scheduler.fsrs_scheduler import schedule_review
from src.scheduler.rating_mapper import map_to_fsrs_rating


card = create_new_card()

rating = map_to_fsrs_rating(
    correct=True,
    confidence="HIGH",
)

updated_card, review_log = schedule_review(
    card,
    rating,
)

print(updated_card)

print()

print(review_log)
