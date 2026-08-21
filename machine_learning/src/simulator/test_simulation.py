
from simulator.review_context import ReviewContext
from simulator.simulation import Simulator


def test_simulate_reviews():

    contexts = [
        ReviewContext(
            success_rate=0.7,
            average_confidence=0.7,
            average_response_time=10.0,
            average_hesitation=2.0,
            review_interval_days=3.0,
            repetition_number=3,
        ),
        ReviewContext(
            success_rate=0.4,
            average_confidence=0.4,
            average_response_time=20.0,
            average_hesitation=5.0,
            review_interval_days=10.0,
            repetition_number=2,
        ),
    ]

    simulator = Simulator(seed=42)

    result = simulator.simulate_reviews(
        contexts
    )

    assert len(result.outcomes) == 2

    for outcome in result.outcomes:

        assert isinstance(
            outcome.correct,
            bool,
        )

        assert 0.0 <= outcome.confidence_score <= 1.0

        assert outcome.response_time_seconds > 0

        assert (
            outcome.hesitation_seconds
            <= outcome.response_time_seconds
        )

        assert outcome.answer_changes >= 0

def test_simulation_is_deterministic():

    contexts = [
        ReviewContext(
            success_rate=0.7,
            average_confidence=0.7,
            average_response_time=10.0,
            average_hesitation=2.0,
            review_interval_days=3.0,
            repetition_number=3,
        )
    ]

    result_1 = Simulator(
        seed=42
    ).simulate_reviews(contexts)

    result_2 = Simulator(
        seed=42
    ).simulate_reviews(contexts)

    assert result_1 == result_2