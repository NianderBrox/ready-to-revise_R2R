from dataclasses import dataclass

# We'll tune them later. all add to 1.0

# Confidence thresholds

LOW_CONFIDENCE_THRESHOLD = 0.40
HIGH_CONFIDENCE_THRESHOLD = 0.75

# Outcome noise shaping: p(correct) = clamp(0.5 + K * (memory_score - 0.5)).
# K=1 reproduces the original Bernoulli(memory_score) draw; larger values
# sharpen outcomes toward deterministic and raise the classifier accuracy
# ceiling (~85% at K=2).

OUTCOME_SHARPNESS_K = 2.0

# Confidence weights

CORRECT_WEIGHT = 0.40
RESPONSE_TIME_WEIGHT = 0.25
HESITATION_WEIGHT = 0.20
ANSWER_CHANGE_WEIGHT = 0.15

ANSWER_CHANGE_MEMORY_WEIGHT = 0.70
ANSWER_CHANGE_HESITATION_WEIGHT = 0.30


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

# Memory score weights

from dataclasses import dataclass


@dataclass(frozen=True)
class MemoryWeights:

    success_rate: float

    average_confidence: float

    average_response_time: float

    average_hesitation: float

    review_interval_days: float

    repetition_number: float


MEMORY_WEIGHTS = MemoryWeights(

    success_rate=0.30,

    average_confidence=0.20,

    average_response_time=0.15,

    average_hesitation=0.10,

    review_interval_days=0.15,

    repetition_number=0.10,

)

@dataclass(frozen=True)
class ResponseTimeConfig:
    min_seconds: float
    base_std_seconds: float
    max_std_seconds: float

RESPONSE_TIME_CONFIG=ResponseTimeConfig(
    min_seconds=2.0,
    base_std_seconds=2.0,
    max_std_seconds=12.0
)


@dataclass(frozen=True)
class HesitationConfig:

    min_ratio: float

    max_ratio: float

    ratio_std: float


HESITATION_CONFIG = HesitationConfig(
    min_ratio=0.05,
    max_ratio=0.70,
    ratio_std=0.08,
)