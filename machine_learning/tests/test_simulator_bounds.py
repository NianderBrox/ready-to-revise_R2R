"""
Behavioral simulator bounds + determinism tests.
"""

from __future__ import annotations

from src.simulator.config import SIMULATION_LIMITS
from src.simulator.enums import ConfidenceLevel
from src.simulator.review_context import ReviewContext
from src.simulator.simulation import Simulator


def _context(**overrides) -> ReviewContext:
    base = {
        "success_rate": 0.7,
        "average_confidence": 0.6,
        "average_response_time": 30.0,
        "average_hesitation": 5.0,
        "review_interval_days": 3.0,
        "repetition_number": 2,
    }

    base.update(overrides)

    return ReviewContext(**base)


def _run(contexts, seed=42):
    return Simulator(seed).simulate_reviews(contexts).outcomes


def test_outcome_fields_within_simulation_limits():
    contexts = [
        _context(
            success_rate=rate,
            review_interval_days=interval,
        )
        for rate in (0.0, 0.3, 0.7, 1.0)
        for interval in (0, 1, 10, 60)
    ]

    outcomes = _run(contexts * 25)

    assert len(outcomes) == len(contexts) * 25

    for outcome in outcomes:
        assert isinstance(outcome.correct, bool)

        assert 0.0 <= outcome.confidence_score <= 1.0

        assert outcome.confidence in ConfidenceLevel

        assert 0 < outcome.response_time_seconds <= (
            SIMULATION_LIMITS.max_response_time
        )

        assert 0 <= outcome.hesitation_seconds <= (
            SIMULATION_LIMITS.max_hesitation
        )

        assert 0 <= outcome.answer_changes <= (
            SIMULATION_LIMITS.max_answer_changes
        )


def test_extreme_histories_stay_in_bounds():
    contexts = [
        _context(success_rate=1.0, average_confidence=1.0),
        _context(success_rate=0.0, average_confidence=0.0),
        _context(review_interval_days=365.0),
        _context(repetition_number=20),
    ]

    for outcome in _run(contexts * 50):
        assert 0.0 <= outcome.confidence_score <= 1.0

        assert outcome.response_time_seconds > 0


def test_same_seed_is_deterministic():
    contexts = [_context()] * 100

    first = _run(contexts, seed=7)

    second = _run(contexts, seed=7)

    assert first == second


def test_different_sees_diverge_statistically():
    contexts = [_context()] * 300

    first = _run(contexts, seed=1)

    second = _run(contexts, seed=2)

    rate_a = sum(o.correct for o in first) / len(first)

    rate_b = sum(o.correct for o in second) / len(second)

    assert abs(rate_a - rate_b) < 0.2


def test_higher_success_rate_yields_more_correct_answers():
    easy = _run([_context(success_rate=0.95)] * 400)

    hard = _run([_context(success_rate=0.05)] * 400, seed=99)

    easy_rate = sum(o.correct for o in easy) / len(easy)

    hard_rate = sum(o.correct for o in hard) / len(hard)

    assert easy_rate > hard_rate


def test_sharpness_k_pushes_outcomes_toward_extremes(monkeypatch):
    import src.simulator.behavioural_model as bm

    strong = _context(
        success_rate=0.95,
        average_confidence=0.95,
        review_interval_days=1,
        repetition_number=5,
    )

    weak = _context(
        success_rate=0.05,
        average_confidence=0.05,
        review_interval_days=180,
        repetition_number=0,
    )

    def rate(context, k):
        monkeypatch.setattr(bm, "OUTCOME_SHARPNESS_K", k)

        outcomes = Simulator(11).simulate_reviews(
            [context] * 500
        ).outcomes

        return sum(o.correct for o in outcomes) / len(outcomes)

    assert rate(strong, 2.0) > rate(strong, 1.0) + 0.05

    assert rate(weak, 2.0) < rate(weak, 1.0) - 0.05
