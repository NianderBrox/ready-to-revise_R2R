

from __future__ import annotations

from typing import Literal

from pydantic import BaseModel, Field


class ReviewFeatures(BaseModel):

    total_revisions: float | None = Field(default=None, ge=0)
    success_rate: float | None = Field(default=None, ge=0, le=1)
    average_confidence: float | None = Field(default=None, ge=0, le=1)
    average_response_time: float | None = Field(default=None, ge=0)
    average_hesitation: float | None = Field(default=None, ge=0)

    difficulty: int = Field(ge=1, le=3)
    word_count: int = Field(ge=0)
    character_count: int = Field(ge=0)

    session_duration_minutes: float = Field(ge=0)
    question_position_in_session: int = Field(ge=1)
    days_since_last_session: float | None = Field(default=None, ge=0)

    review_interval_days: float | None = Field(default=None, ge=0)
    repetition_number: int = Field(ge=0)

    last_review_confidence_score: float | None = Field(
        default=None, ge=0, le=1
    )
    last_review_response_time: float | None = Field(default=None, ge=0)
    last_review_hesitation: float | None = Field(default=None, ge=0)

    answer_changes: int = Field(ge=0)

    subject: str
    topic: str
    hour_of_day: int = Field(ge=0, le=23)
    day_of_week: int = Field(ge=0, le=6)

    last_review_correct: bool | None = None

    # Hybrid + aggregate features. All optional for backward
    # compatibility; imputed inside the pipeline when absent.

    fsrs_recall_probability: float | None = Field(default=None, ge=0, le=1)
    had_fsrs_estimate: bool | None = None

    user_success_rate: float | None = Field(default=None, ge=0, le=1)
    user_average_confidence: float | None = Field(default=None, ge=0, le=1)
    user_average_response_time: float | None = Field(default=None, ge=0)

    question_global_success_rate: float | None = Field(
        default=None, ge=0, le=1
    )

    recent_success_rate_5: float | None = Field(default=None, ge=0, le=1)
    recent_confidence_5: float | None = Field(default=None, ge=0, le=1)

    consecutive_correct: float | None = Field(default=None, ge=0)

    hesitation_response_ratio: float | None = Field(default=None, ge=0)

    normalized_interval_days: float | None = Field(
        default=None, ge=0, le=1
    )
    normalized_repetition_number: float | None = Field(
        default=None, ge=0, le=1
    )
    normalized_avg_response_time: float | None = Field(
        default=None, ge=0, le=1
    )
    normalized_avg_hesitation: float | None = Field(
        default=None, ge=0, le=1
    )


class PredictRequest(BaseModel):
    model_name: Literal[
        "gradient_boosting",
        "hist_gradient_boosting",
        "random_forest",
        "logistic_regression",
    ] = "gradient_boosting"

    features: ReviewFeatures


class PredictionResponse(BaseModel):
    recall_probability: float
    predicted_correct: bool
    threshold: float
    model_name: str


class Candidate(BaseModel):
    question_id: str
    features: ReviewFeatures


class RecommendRequest(BaseModel):
    candidates: list[Candidate] = Field(min_length=1, max_length=500)

    top_k: int = Field(default=10, ge=1, le=100)

    model_name: Literal[
        "gradient_boosting",
        "hist_gradient_boosting",
        "random_forest",
        "logistic_regression",
    ] = "gradient_boosting"


class Recommendation(BaseModel):
    rank: int
    question_id: str
    recall_probability: float
    priority: str


class RecommendResponse(BaseModel):
    recommendations: list[Recommendation]
    model_name: str


class HealthResponse(BaseModel):
    status: str
    model_loaded: bool
