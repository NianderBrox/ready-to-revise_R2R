"""
Inference API integration tests (require trained models in
models/ — run `python train.py` once before the suite).
"""

from __future__ import annotations

import pandas as pd
import pytest
from fastapi.testclient import TestClient

from src.feature_engineering.feature_manifest import ALL_FEATURES

pytest.importorskip("fastapi")

from src.inference.app import app


@pytest.fixture(scope="module")
def client():
    return TestClient(app)


INT_FIELDS = {
    "difficulty",
    "word_count",
    "character_count",
    "question_position_in_session",
    "hour_of_day",
    "day_of_week",
    "repetition_number",
    "answer_changes",
}


@pytest.fixture(scope="module")
def sample_features():
    df = pd.read_csv("data/features/training_dataset.csv")

    row = df.dropna(subset=ALL_FEATURES).iloc[0]

    features = {}

    for name in ALL_FEATURES:
        value = row[name]

        if name == "last_review_correct":
            features[name] = bool(value)
        elif name in INT_FIELDS:
            features[name] = int(value)
        elif name in ("subject", "topic"):
            features[name] = str(value)
        else:
            features[name] = float(value)

    return features


def test_health(client):
    response = client.get("/health")

    assert response.status_code == 200

    assert response.json()["status"] == "ok"


def test_predict_returns_valid_probability(client, sample_features):
    response = client.post(
        "/predict",
        json={"features": sample_features},
    )

    assert response.status_code == 200

    body = response.json()

    assert 0.0 <= body["recall_probability"] <= 1.0

    assert isinstance(body["predicted_correct"], bool)

    assert body["model_name"] == "gradient_boosting"


def test_predict_rejects_invalid_difficulty(client, sample_features):
    invalid = dict(sample_features)

    invalid["difficulty"] = 9

    response = client.post(
        "/predict",
        json={"features": invalid},
    )

    assert response.status_code == 422


def _variant(base: dict, **overrides) -> dict:
    row = dict(base)

    row.update(overrides)

    return row


def test_recommend_orders_by_lowest_recall(client, sample_features):
    easy = _variant(
        sample_features,
        success_rate=0.95,
        average_confidence=0.90,
        total_revisions=8,
        repetition_number=4,
        review_interval_days=1.0,
        last_review_correct=True,
        last_review_confidence_score=0.90,
        answer_changes=0,
        fsrs_recall_probability=0.97,
        had_fsrs_estimate=True,
        user_success_rate=0.9,
        question_global_success_rate=0.9,
        recent_success_rate_5=0.95,
        recent_confidence_5=0.9,
        consecutive_correct=6,
        hesitation_response_ratio=0.1,
        normalized_interval_days=0.003,
        normalized_repetition_number=0.2,
        normalized_avg_response_time=0.15,
        normalized_avg_hesitation=0.1,
    )

    hard = _variant(
        sample_features,
        success_rate=0.05,
        average_confidence=0.15,
        total_revisions=1,
        repetition_number=0,
        review_interval_days=180.0,
        last_review_correct=False,
        last_review_confidence_score=0.25,
        answer_changes=4,
        fsrs_recall_probability=0.03,
        had_fsrs_estimate=True,
        user_success_rate=0.1,
        question_global_success_rate=0.1,
        recent_success_rate_5=0.05,
        recent_confidence_5=0.15,
        consecutive_correct=0,
        hesitation_response_ratio=0.7,
        normalized_interval_days=0.49,
        normalized_repetition_number=0.0,
        normalized_avg_response_time=0.85,
        normalized_avg_hesitation=0.8,
    )

    payload = {
        "candidates": [
            {"question_id": "easy-q", "features": easy},
            {"question_id": "hard-q", "features": hard},
            {"question_id": "easy-clone", "features": dict(easy)},
        ],
        "top_k": 3,
    }

    response = client.post("/recommend-revisions", json=payload)

    assert response.status_code == 200

    recommendations = response.json()["recommendations"]

    probabilities = [r["recall_probability"] for r in recommendations]

    assert probabilities == sorted(probabilities)

    assert (
        probabilities[-1] - probabilities[0] > 0.1
    ), f"no contrast between candidates: {probabilities}"

    top = recommendations[0]

    assert top["question_id"] == "hard-q"

    assert top["recall_probability"] < 0.5

    expected_band = (
        "high"
        if top["recall_probability"] < 0.35
        else "medium"
        if top["recall_probability"] < 0.65
        else "low"
    )

    assert top["priority"] == expected_band


def test_recommend_respects_top_k(client, sample_features):
    payload = {
        "candidates": [
            {"question_id": f"q{i}", "features": sample_features}
            for i in range(5)
        ],
        "top_k": 2,
    }

    body = client.post("/recommend-revisions", json=payload).json()

    assert len(body["recommendations"]) == 2

    ranks = [r["rank"] for r in body["recommendations"]]

    assert ranks == [1, 2]
