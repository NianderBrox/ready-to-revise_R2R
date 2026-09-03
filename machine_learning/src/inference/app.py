

from __future__ import annotations

import os
from datetime import UTC, datetime

from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fsrs import Card, State

from src.inference.predictor import ModelPredictor
from src.inference.schemas import (
    HealthResponse,
    PredictionResponse,
    PredictRequest,
    Recommendation,
    RecommendRequest,
    RecommendResponse,
    ScheduleReviewRequest,
    ScheduleReviewResponse,
)
from src.scheduler.card_factory import create_new_card
from src.scheduler.fsrs_scheduler import schedule_review
from src.scheduler.rating_mapper import map_to_fsrs_rating

load_dotenv()

app = FastAPI(
    title="R2R Inference API",
    description="Recall prediction and revision recommendations for Ready to Revise.",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

ALPHA = 0.5
R_STAR = 0.9

DEFAULT_MODEL = os.getenv("ML_MODEL_NAME", "calibrated_best")

_predictors: dict[str, ModelPredictor] = {}


def get_predictor(model_name: str) -> ModelPredictor:
    predictor = _predictors.get(model_name)

    if predictor is None:
        try:
            predictor = ModelPredictor(model_name).load()
        except FileNotFoundError as error:
            raise HTTPException(
                status_code=503,
                detail=(
                    f"Model '{model_name}' is not available. "
                    "Run `python train.py` first."
                ),
            ) from error

        _predictors[model_name] = predictor

    return predictor


def _priority(probability: float) -> str:
    if probability < 0.35:
        return "high"

    if probability < 0.65:
        return "medium"

    return "low"


@app.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    default = _predictors.get(DEFAULT_MODEL)

    return HealthResponse(
        status="ok",
        model_loaded=bool(default is not None and default.is_loaded),
    )


@app.post("/predict", response_model=PredictionResponse)
def predict(request: PredictRequest) -> PredictionResponse:
    predictor = get_predictor(request.model_name)

    result = predictor.predict_one(
        request.features.model_dump(),
    )

    return PredictionResponse(**result)


@app.post("/recommend-revisions", response_model=RecommendResponse)
def recommend_revisions(request: RecommendRequest) -> RecommendResponse:
    predictor = get_predictor(request.model_name)

    records = [
        candidate.features.model_dump()
        for candidate in request.candidates
    ]

    probabilities = predictor.predict_proba(records)

    order = probabilities.argsort()

    top_k = min(request.top_k, len(order))

    recommendations = []

    for rank_index in range(top_k):
        position = int(order[rank_index])

        probability = float(probabilities[position])

        recommendations.append(
            Recommendation(
                rank=rank_index + 1,
                question_id=request.candidates[position].question_id,
                recall_probability=round(probability, 4),
                priority=_priority(probability),
            )
        )

    return RecommendResponse(
        recommendations=recommendations,
        model_name=request.model_name,
    )


def _restore_card(
    fsrs_state: int | None,
    fsrs_step: int | None,
    fsrs_stability: float | None,
    fsrs_difficulty: float | None,
    last_review_at: str | None,
) -> Card:
    if fsrs_state is None or fsrs_stability is None or fsrs_difficulty is None:
        return create_new_card()

    try:
        state = State(fsrs_state)
    except ValueError:
        return create_new_card()

    last_review = (
        datetime.fromisoformat(last_review_at.replace("Z", "+00:00"))
        if last_review_at
        else datetime.now(UTC)
    )

    return Card(
        state=state,
        step=fsrs_step,
        stability=fsrs_stability,
        difficulty=fsrs_difficulty,
        due=last_review,
        last_review=last_review,
    )


@app.post("/schedule-review", response_model=ScheduleReviewResponse)
def schedule_review_endpoint(
    request: ScheduleReviewRequest,
) -> ScheduleReviewResponse:
    predictor = get_predictor(DEFAULT_MODEL)

    recall_probability = float(
        predictor.predict_proba([request.features.model_dump()])[0]
    )

    card = _restore_card(
        request.fsrs_state,
        request.fsrs_step,
        request.fsrs_stability,
        request.fsrs_difficulty,
        request.last_review_at,
    )

    rating = map_to_fsrs_rating(
        correct=request.correct,
        confidence=request.confidence,
    )

    now = datetime.now(UTC)

    result = schedule_review(card=card, rating=rating, review_time=now)

    base_interval = result["scheduled_interval_days"]

    adjusted_interval = base_interval * (
        1 + ALPHA * (recall_probability - R_STAR)
    )

    adjusted_interval = max(adjusted_interval, 0.0001)

    next_review_at = now.timestamp() + adjusted_interval * 86400

    next_review_dt = datetime.fromtimestamp(next_review_at, tz=UTC)

    return ScheduleReviewResponse(
        interval_days=round(adjusted_interval, 4),
        next_review_at=next_review_dt.isoformat(),
        recall_probability=round(recall_probability, 4),
        fsrs_state=result["fsrs_state"],
        fsrs_step=result["fsrs_step"],
        fsrs_stability=round(result["fsrs_stability"], 4),
        fsrs_difficulty=round(result["fsrs_difficulty"], 4),
    )
