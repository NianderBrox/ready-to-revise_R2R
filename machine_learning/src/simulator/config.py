from dataclasses import dataclass

# We'll tune them later. all add to 1.0

# Confidence thresholds

LOW_CONFIDENCE_THRESHOLD = 0.40
HIGH_CONFIDENCE_THRESHOLD = 0.75

# Confidence weights

CORRECT_WEIGHT = 0.40
RESPONSE_TIME_WEIGHT = 0.25
HESITATION_WEIGHT = 0.20
ANSWER_CHANGE_WEIGHT = 0.15


@dataclass(frozen=True)
class SimulationLimits:

    max_response_time: float

    max_hesitation: float

    max_review_interval_days: float

    max_repetition_number: int

    max_answer_changes: int


SIMULATION_LIMITS = SimulationLimits(
    max_response_time=120.0,
    max_hesitation=30.0,
    max_review_interval_days=365.0,
    max_repetition_number=20,
    max_answer_changes=5,
)