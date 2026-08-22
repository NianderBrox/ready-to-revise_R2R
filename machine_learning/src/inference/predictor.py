

from __future__ import annotations

import threading

import joblib
import numpy as np
import pandas as pd

from src.feature_engineering.feature_manifest import (
    ALL_FEATURES,
    BOOLEAN_FEATURES,
)
from src.utils.config import MODEL_DIR


class ModelPredictor:


    def __init__(
        self,
        model_name: str = "gradient_boosting",
    ):
        self.model_name = model_name

        self._lock = threading.Lock()

        self._pipeline = None

    @property
    def is_loaded(self) -> bool:
        return self._pipeline is not None

    def load(self) -> ModelPredictor:
        if self._pipeline is not None:
            return self

        path = MODEL_DIR / f"{self.model_name}.joblib"

        with self._lock:
            if self._pipeline is None:
                self._pipeline = joblib.load(path)

        return self

    def features_to_frame(
        self,
        records: list[dict],
    ) -> pd.DataFrame:


        frame = pd.DataFrame(records)

        for column in ALL_FEATURES:
            if column not in frame.columns:
                frame[column] = np.nan

        for column in BOOLEAN_FEATURES:
            frame[column] = (
                frame[column].fillna(False).astype(int)
            )

        return frame[ALL_FEATURES]

    def predict_proba(
        self,
        records: list[dict],
    ) -> np.ndarray:
        self.load()

        frame = self.features_to_frame(records)

        with self._lock:
            proba = self._pipeline.predict_proba(frame)[:, 1]

        return np.clip(proba, 0.0, 1.0)

    def predict_one(
        self,
        record: dict,
        threshold: float = 0.5,
    ) -> dict:
        probability = float(self.predict_proba([record])[0])

        return {
            "recall_probability": round(probability, 4),
            "predicted_correct": bool(probability >= threshold),
            "threshold": threshold,
            "model_name": self.model_name,
        }


_DEFAULT_PREDICTOR: ModelPredictor | None = None


def get_default_predictor() -> ModelPredictor:
    global _DEFAULT_PREDICTOR

    if _DEFAULT_PREDICTOR is None:
        _DEFAULT_PREDICTOR = ModelPredictor().load()

    return _DEFAULT_PREDICTOR
