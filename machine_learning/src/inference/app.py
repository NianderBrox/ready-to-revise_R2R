

from __future__ import annotations

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from src.inference.predictor import ModelPredictor
from src.inference.schemas import (
    HealthResponse,
    PredictionResponse,
    PredictRequest,
    Recommendation,
    RecommendRequest,
    RecommendResponse,
)

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
    default = _predictors.get("gradient_boosting")

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
