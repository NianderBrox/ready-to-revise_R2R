from fsrs import Scheduler

scheduler = Scheduler()


def schedule_review(card, rating):

    updated_card, review_log = scheduler.review_card(
        card,
        rating,
    )

    return {
        "updated_card": updated_card,
        "review_log": review_log,
        "fsrs_state": updated_card.state.value,
        "fsrs_step": updated_card.step,
        "fsrs_stability": updated_card.stability,
        "fsrs_difficulty": updated_card.difficulty,
        "next_review_at": updated_card.due,
        "last_review": updated_card.last_review,
    }
