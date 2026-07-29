from fsrs import Rating


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
