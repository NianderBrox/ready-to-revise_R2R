"""
R2R ML Feature Manifest

This file defines the official features used by the forgetting curve model.

Do not change feature names without updating:
- preprocessing
- training
- inference
- backend API
"""


# ==============================
# Numerical Features
# ==============================

NUMERICAL_FEATURES = [
    # Student history
    "total_revisions",
    "success_rate",
    "average_confidence",
    "average_response_time",
    "average_hesitation",
    # Question information
    "difficulty",
    "word_count",
    "character_count",
    # Session information
    "session_duration_minutes",
    "question_position_in_session",
    "days_since_last_session",
    # Review history
    "review_interval_days",
    "repetition_number",

    "last_review_confidence_score",

    "last_review_response_time",

    "last_review_hesitation",
    # Behaviour
    "answer_changes",
]


# ==============================
# Categorical Features
# ==============================

CATEGORICAL_FEATURES = [
    "subject",
    "topic",
    "hour_of_day",
    "day_of_week",
]


# ==============================
# Boolean Features
# ==============================

BOOLEAN_FEATURES = [
    "last_review_correct",
]


# ==============================
# Target
# ==============================

TARGET = "correct"


# ==============================
# Complete Feature List
# ==============================

ALL_FEATURES = NUMERICAL_FEATURES + CATEGORICAL_FEATURES + BOOLEAN_FEATURES
