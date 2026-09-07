from __future__ import annotations

import pandas as pd

from src.database.connection import engine
from src.feature_engineering.feature_builder import build_features
from src.utils.config import FEATURE_DATA_DIR

OUTPUT_PATH = FEATURE_DATA_DIR / "real_dataset.csv"

VIEW_NAME = "ml.question_reviews"

# Columns the extended view must expose for a valid training export.
REQUIRED_COLUMNS = {
    "id",
    "user_id",
    "question_id",
    "session_id",
    "review_time",
    "previous_review_time",
    "correct",
    "confidence_score",
    "response_time_seconds",
    "hesitation_seconds",
    "answer_changes",
    "repetition_number",
    "subject",
    "topic",
    "word_count",
    "character_count",
    "recall_probability",
    "difficulty",
    "question_position_in_session",
    "session_duration_minutes",
}

# Columns passed straight through to build_features under the same name.
PASSTHROUGH_COLUMNS = [
    "id",
    "user_id",
    "question_id",
    "session_id",
    "review_time",
    "previous_review_time",
    "correct",
    "confidence",
    "confidence_score",
    "response_time_seconds",
    "hesitation_seconds",
    "answer_changes",
    "repetition_number",
    "subject",
    "topic",
    "word_count",
    "character_count",
    "recall_probability",
]

_DIFFICULTY_SCORE_MAP = {
    "EASY": 1,
    "MEDIUM": 2,
    "HARD": 3,
}

_NUMERIC_TO_DIFFICULTY = {
    1: "EASY",
    2: "MEDIUM",
    3: "HARD",
}


def validate_view(view: pd.DataFrame) -> None:

    missing = REQUIRED_COLUMNS - set(view.columns)

    if missing:
        raise ValueError(
            f"{VIEW_NAME} is missing columns required for a training export: "
            f"{sorted(missing)}. The extended view (backend migrations "
            "20260904000001_add_ml_fsrs_fields / "
            "20260905000001_add_session_metrics / "
            "20260905020000_add_view_recall_probability / "
            "20260905030000_fix_view_memorized_rating) may not be applied "
            "yet."
        )


def _as_difficulty_enum(value) -> str:
    try:
        return _NUMERIC_TO_DIFFICULTY[int(value)]
    except (ValueError, TypeError):
        return str(value)


def _shape_for_pipeline(view: pd.DataFrame) -> pd.DataFrame:

    fields = pd.DataFrame()

    for column in PASSTHROUGH_COLUMNS:
        fields[column] = view[column]

    # difficulty -> question_difficulty, as the text enum build_features maps.
    difficulty = view["difficulty"]

    if pd.api.types.is_numeric_dtype(difficulty):
        question_difficulty = difficulty.map(_as_difficulty_enum)
    else:
        question_difficulty = difficulty.astype(str)

    fields["question_difficulty"] = question_difficulty

    # build_features copies question_position -> question_position_in_session.
    fields["question_position"] = view["question_position_in_session"]

    # Session bounds are not in the view; synthesize them so build_features'
    # session_duration recomputation yields 0, which we restore afterwards with
    # the view's real session_duration_minutes value.
    fields["started_at"] = pd.to_datetime(view["review_time"])
    fields["ended_at"] = pd.to_datetime(view["review_time"])

    return fields


def build_real_dataset(view: pd.DataFrame) -> pd.DataFrame:

    validate_view(view)

    shaped = _shape_for_pipeline(view)

    dataset = build_features(shaped)

    # build_features recomputes duration from the aliased bounds (=0); restore
    # the view's real, per-review measurement. Index alignment survives the
    # internal sort because build_features preserves row labels.
    dataset["session_duration_minutes"] = (
        view["session_duration_minutes"].astype(float)
    )

    return dataset


def export_real_dataset(output_path=OUTPUT_PATH) -> pd.DataFrame:

    view = pd.read_sql(f"SELECT * FROM {VIEW_NAME}", engine)

    dataset = build_real_dataset(view)

    FEATURE_DATA_DIR.mkdir(parents=True, exist_ok=True)

    dataset.to_csv(output_path, index=False)

    print(f"Saved {output_path}: {len(dataset)} rows, {len(dataset.columns)} columns")

    return dataset


if __name__ == "__main__":
    export_real_dataset()