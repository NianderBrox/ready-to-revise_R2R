

from dataclasses import dataclass

CONFIDENCE_SCORE_MAP = {
    "LOW": 0.25,
    "MEDIUM": 0.60,
    "HIGH": 0.90,
}

# Cold-start defaults used when a question has no history yet.
# These mirror the fill values documented in the feature spec.

DEFAULT_SUCCESS_RATE = 0.5

DEFAULT_AVERAGE_CONFIDENCE = 0.60

DEFAULT_AVERAGE_RESPONSE_TIME = 45.0

DEFAULT_AVERAGE_HESITATION = 4.0


@dataclass(frozen=True)
class GenerationConfig:
    n_users: int = 300

    min_questions_per_user: int = 30

    max_questions_per_user: int = 55

    history_days: int = 180

    min_questions_per_session: int = 4

    max_questions_per_session: int = 10


    inter_question_gap_min: float = 4.0

    inter_question_gap_max: float = 18.0


    session_tail_buffer_max: float = 120.0

    session_gap_weights: tuple[tuple[int, float], ...] = (
        (1, 0.45),
        (2, 0.30),
        (3, 0.15),
        (4, 0.10),
    )

    long_break_probability: float = 0.05

    long_break_min_days: int = 7

    long_break_max_days: int = 14

    db_chunk_size: int = 5000


DEFAULT_GENERATION_CONFIG = GenerationConfig()

SUBJECT_TOPICS = {
    "Mathematics": [
        "Algebra",
        "Geometry",
        "Calculus",
        "Probability",
    ],
    "Physics": [
        "Mechanics",
        "Optics",
        "Thermodynamics",
        "Electromagnetism",
    ],
    "Chemistry": [
        "Organic Chemistry",
        "Inorganic Chemistry",
        "Physical Chemistry",
        "Analytical Chemistry",
    ],
    "Biology": [
        "Genetics",
        "Ecology",
        "Human Physiology",
        "Cell Biology",
    ],
    "History": [
        "Ancient History",
        "Medieval History",
        "Modern History",
        "World Wars",
    ],
    "Geography": [
        "Climate",
        "Landforms",
        "Human Geography",
        "Cartography",
    ],
    "Computer Science": [
        "Data Structures",
        "Algorithms",
        "Databases",
        "Networks",
    ],
}

DIFFICULTY_WEIGHTS = [
    ("EASY", 0.35),
    ("MEDIUM", 0.45),
    ("HARD", 0.20),
]

TIMEZONES = [
    "Asia/Kolkata",
    "Asia/Kolkata",
    "Asia/Kolkata",
    "Asia/Dubai",
    "Asia/Singapore",
    "Asia/Tokyo",
    "Europe/London",
    "Europe/Berlin",
    "America/New_York",
    "America/Sao_Paulo",
    "Australia/Sydney",
]



WORD_COUNT_PROFILES = {
    "EASY": (25, 8, 5, 60),
    "MEDIUM": (45, 12, 10, 90),
    "HARD": (70, 18, 20, 140),
}
