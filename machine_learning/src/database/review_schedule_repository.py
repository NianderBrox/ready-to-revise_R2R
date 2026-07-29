from sqlalchemy import text

from src.database.connection import engine
from fsrs import Card


def get_latest_review_schedule(
    user_id,
    question_id,
    scheduler_name="FSRS",
):
    """
    Returns the latest scheduler state for a user's question.

    Parameters
    ----------
    user_id : UUID
        User ID.

    question_id : UUID
        Question ID.

    scheduler_name : str
        Scheduler to load.

    Returns
    -------
    Mapping | None
    """

    query = text("""
        SELECT
            rs.*
        FROM review_schedules rs

        JOIN question_reviews qr
            ON rs.review_id = qr.id

        WHERE
            qr.user_id = :user_id
            AND qr.question_id = :question_id
            AND rs.scheduler_name = :scheduler_name

        ORDER BY
            qr.review_time DESC

        LIMIT 1;
    """)

    with engine.connect() as conn:
        result = conn.execute(
            query,
            {
                "user_id": user_id,
                "question_id": question_id,
                "scheduler_name": scheduler_name,
            },
        )

        return result.mappings().first()


def save_card(card: Card):
    """
    Placeholder.

    Later this will insert/update
    review_schedules.
    """

    print(card)


def save_review_schedule(
    review_id,
    scheduler_name,
    input_rating,
    card,
): ...
