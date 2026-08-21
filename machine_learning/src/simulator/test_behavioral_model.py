from random import Random

from src.simulator.behavioural_model import BehavioralModel
from src.simulator.review_context import ReviewContext


def test_behavioral_model():

    rng = Random(42)

    model = BehavioralModel(rng)

    context = ReviewContext(
        success_rate=0.7,
        average_confidence=0.7,
        average_response_time=10.0,
        average_hesitation=2.0,
        review_interval_days=3.0,
        repetition_number=3,
    )

    outcome = model.simulate(context)

    print("\nGenerated review:")
    print(outcome)

    assert isinstance(outcome.correct, bool)

    assert 0.0 <= outcome.confidence_score <= 1.0

    assert outcome.response_time_seconds > 0

    assert (
        outcome.response_time_seconds
        <= 120.0
    )

    assert (
        0.0
        <= outcome.hesitation_seconds
        <= outcome.response_time_seconds
    )

    assert outcome.answer_changes >= 0

def test_behavioral_model_is_reproducible():

    context = ReviewContext(
        success_rate=0.7,
        average_confidence=0.7,
        average_response_time=10.0,
        average_hesitation=2.0,
        review_interval_days=3.0,
        repetition_number=3,
    )

    outcome_1 = BehavioralModel(
        Random(42)
    ).simulate(context)

    outcome_2 = BehavioralModel(
        Random(42)
    ).simulate(context)

    assert outcome_1 == outcome_2