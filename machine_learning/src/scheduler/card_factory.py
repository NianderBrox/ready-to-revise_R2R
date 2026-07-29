from fsrs import Card
from fsrs import State


def create_new_card() -> Card:
    """
    Creates a brand-new FSRS card.
    """

    return Card()


def restore_card(schedule) -> Card:
    """
    Restores an FSRS card from the database.
    """

    return Card(
        card_id=schedule["review_id"].int,
        state=State(schedule["fsrs_state"]),
        step=schedule["fsrs_step"],
        stability=schedule["fsrs_stability"],
        difficulty=schedule["fsrs_difficulty"],
        due=schedule["next_review_at"],
        last_review=schedule["last_review"],
    )
