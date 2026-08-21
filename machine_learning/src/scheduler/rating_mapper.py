from fsrs import Rating

from src.database.enums import SchedulerRating


def map_to_fsrs_rating(
    correct: bool,
    confidence: str,
) -> Rating:
    """
    Maps R2R review data to an FSRS rating.

    Parameters
    ----------
    correct : bool
        Whether the answer was correct.

    confidence : str
        LOW, MEDIUM or HIGH

    Returns
    -------
    Rating
    """

    confidence = confidence.upper()

    if not correct:
        return Rating.Again

    if confidence == "LOW":
        return Rating.Hard

    if confidence == "MEDIUM":
        return Rating.Good

    if confidence == "HIGH":
        return Rating.Easy

    raise ValueError(f"Unknown confidence: {confidence}")


def map_to_scheduler_rating(
    correct: bool,
    confidence: str,
) -> SchedulerRating:
    """
    Same mapping as map_to_fsrs_rating but returns the
    SchedulerRating enum used for database storage.
    """

    if not correct:
        return SchedulerRating.AGAIN

    confidence = confidence.upper()

    if confidence == "LOW":
        return SchedulerRating.HARD

    if confidence == "MEDIUM":
        return SchedulerRating.GOOD

    if confidence == "HIGH":
        return SchedulerRating.EASY

    raise ValueError(f"Unknown confidence: {confidence}")
