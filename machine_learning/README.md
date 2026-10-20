# Ready-to-Revise (R2R) Machine Learning

Predicts whether a user will correctly recall a question at their next
review, and recommends which questions to revise first.

## Pipeline

| Stage | Module | Entry point |
|---|---|---|
| Synthetic data generation (simulator → SQL) | `src/data_generation/` | `uv run python -m src.data_generation.runner --users 200 --questions 35 --days 120 --seed 42` |
| Feature export (synthetic history) | `src/feature_engineering/`, `src/preprocessing/` | `uv run python -m src.preprocessing.export_dataset` → `data/features/training_dataset.csv` |
| Feature export (real app data) | `src/preprocessing/` | `uv run python -m src.preprocessing.export_real_dataset` → `data/features/real_dataset.csv` (reads the backend's read-only `ml.question_reviews` view) |
| Hybrid merge (sampled synthetic + real) | `src/preprocessing/` | `uv run python -m src.preprocessing.build_hybrid` → `data/features/hybrid.csv` |
| EDA | `notebooks/eda.ipynb` | open in Jupyter |
| Training (LR / RF / GB / HGB) + isotonic calibration | `src/training/` | `uv run python train.py` (`--tune` hyperparameter search; `--keep-incumbent` saves the serving model for the gate) → `models/*.joblib`, `models/training_report.json` |
| Promotion gate (candidate vs incumbent) | `src/evaluation/` | `uv run python -m src.evaluation.eval_gate` → `models/eval_gate_report.json` |
| ML vs FSRS baseline comparison | `src/evaluation/` | `uv run python -m src.evaluation.compare_baselines` → `models/comparison_summary.json` |
| Inference REST API | `src/inference/` | `uv run uvicorn src.inference.app:app --host 127.0.0.1 --port 8000` |

The serving artifact is `models/calibrated_best.joblib`: the best base model
wrapped in `CalibratedClassifierCV` (isotonic). The four base candidates are
`logistic_regression`, `random_forest`, `gradient_boosting`,
`hist_gradient_boosting`; `calibrated_best` is the calibrated incumbent the
backend consumes by default. Training defaults to
`data/features/training_dataset.csv` — pass the path of `hybrid.csv` via
`--dataset` to train on merged data.

## Quickstart

```bash
# 1. Environment (Python 3.11+, uv)
uv sync

# 2. PostgreSQL — pick ONE:
#    A) Throwaway sandbox DB (safe):
#         createdb r2r_train && psql -d r2r_train -f database/schema.sql
#       then set DB_SCHEMA=public in .env
#    B) Shared app database (r2r_db): set DB_HOST/DB_PORT=5433/DB_NAME in .env
#       and read the ml-schema views the backend migrations create
#       (backend migration `create_ml_schema_views`).
#
#    ⚠️ NEVER run database/reset_database.sql against r2r_db — it executes
#    DROP SCHEMA public CASCADE and destroys every backend table.

# 3. Export features — pick the source:
#    synthetic (simulator CSV):  uv run python -m src.preprocessing.export_dataset
#    real app data (backend DB + ml views):
#         uv run python -m src.preprocessing.export_real_dataset
#    hybrid: export both, then
#         uv run python -m src.preprocessing.build_hybrid

# 4. Train models -> models/*.joblib + models/training_report.json
uv run python train.py          # add --tune for hyperparameter search (slower)
                                # add --keep-incumbent before retraining a deployed model

# 5. Gate the new model against the incumbent (needs --keep-incumbent run)
uv run python -m src.evaluation.eval_gate

# 6. Compare ML vs FSRS baseline -> models/comparison_summary.json
uv run python -m src.evaluation.compare_baselines

# 7. Serve predictions on http://127.0.0.1:8000 (docs at /docs)
uv run uvicorn src.inference.app:app --host 127.0.0.1 --port 8000
```

The repo ships two committed artifacts (`models/calibrated_best.joblib`,
`models/gradient_boosting.joblib`; other variants are git-ignored and appear
when you train), so the API works out of the box. Remove/absent `models/`
means `/predict` responds 503 until step 4 runs.

## API

- `POST /predict` — recall probability for one engineered feature vector;
  returns `recall_probability`, `predicted_correct`, `threshold`, `model_name`
  (default serving threshold comes from `training_report.json`).
- `POST /recommend-revisions` — ranks 1–500 candidate questions by lowest
  predicted recall (highest urgency), tags priority, `top_k` 1–100.
- `POST /schedule-review` — advances the FSRS-style memory state for a review
  (`correct`, `confidence` LOW/MEDIUM/HIGH, optional FSRS card fields) and
  returns `interval_days`, `next_review_at`, `recall_probability` and the
  updated FSRS state.
- `GET /health` — liveness probe; `model_loaded` flips to `true` lazily after
  the first predict/recommend call (cold start takes a few seconds).

All endpoints accept `model_name` (one of the five models above; default
`calibrated_best`, overridable via `ML_MODEL_NAME`).

The API consumes **already-engineered** features — the exact vector defined in
[`src/inference/schemas.py`](src/inference/schemas.py), mirroring
`training_dataset.csv`. Eleven fields are required per request:
`difficulty`, `word_count`, `character_count`, `session_duration_minutes`,
`question_position_in_session`, `repetition_number`, `answer_changes`,
`subject`, `topic`, `hour_of_day`, `day_of_week`. Everything else — per-item
and last-review history, FSRS signals (`fsrs_recall_probability`,
`had_fsrs_estimate`), aggregates (`user_*`, `question_global_success_rate`,
`recent_*_5`, `consecutive_correct`), derived ratios and the `normalized_*`
variants — is optional and imputed inside the model pipeline, so cold-start
users are supported.

## Deployment & integration

- The backend reaches this API at `ML_SERVICE_URL` (default
  `http://localhost:8000`) on the same machine — inference never touches the
  database, so only the training pipeline needs DB credentials.
- A `ml` service exists in the root `docker-compose.yml` (builds this folder,
  `COPY models` bakes `models/` into the image).
- `render.yaml` in this folder deploys the service standalone on Render
  (Docker runtime, health check `/health`).
- When the model is missing/unreachable, the backend falls back to scheduler
  ordering (`source: "scheduler"`) — recommendations keep working.

## Environment

`.env.example`: serving API binds `ML_HOST=127.0.0.1` / `ML_PORT=8000`, model
selection `ML_MODEL_NAME=calibrated_best`; Postgres below is **training
only**: `DB_HOST`, `DB_PORT=5433` (compose host port), `DB_NAME=r2r_db`,
`DB_USER=ml_readonly`, `DB_PASSWORD`, and `DB_SCHEMA` — `ml` reads the
backend's read-only views, `public` targets the local sandbox built from
`database/schema.sql`.

## Tests

```bash
uv run pytest tests/
```

Covers aggregate and historical feature builders, the real-data export shape
(`tests/test_export_real_dataset.py`), simulator output bounds, rating mapper,
dataset constants and API behavior. The API tests expect the committed or
freshly trained `models/*.joblib`.
