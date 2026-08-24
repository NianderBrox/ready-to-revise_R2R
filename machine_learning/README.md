# Ready-to-Revise (R2R) Machine Learning

Predicts whether a user will correctly recall a question at their next
review, and recommends which questions to revise first.

## Pipeline

| Stage | Module | Entry point |
|---|---|---|
| Synthetic data generation | `src/data_generation/` | `python -m src.data_generation.runner --users 200 --questions 35 --days 120 --seed 42` |
| Feature engineering + export | `src/feature_engineering/`, `src/preprocessing/` | `python -m src.preprocessing.export_dataset` |
| EDA | `notebooks/eda.ipynb` | open in Jupyter |
| Training (LR / RF / GBC) | `src/training/` | `python train.py` |
| FSRS vs ML comparison | `src/evaluation/` | `python -m src.evaluation.compare_baselines` |
| Inference REST API | `src/inference/` | `uvicorn src.inference.app:app --port 8000` |

> Deployment & integration: the backend reaches this API at
> `ML_SERVICE_URL` (default `http://localhost:8000`) on the same machine.
> A `ml` service exists in the root `docker-compose.yml` (models persist in the
> `ml_models` volume).
> Until models are trained, `/predict` returns 503 and the backend falls back
> to scheduler ordering.

## Quickstart

```bash
# 1. Environment (Python 3.11+, uv)
uv sync

# 2. PostgreSQL — pick ONE:
#    A) Throwaway sandbox DB (safe):
#         createdb r2r_train && psql -d r2r_train -f database/schema.sql
#    B) Shared app database (r2r_db): set DB_HOST/DB_PORT=5433/DB_NAME in .env
#       and read the ml-schema views the backend migrations create.
#
#    ⚠️ NEVER run database/reset_database.sql against r2r_db — it executes
#    DROP SCHEMA public CASCADE and destroys every backend table.

# 3. Generate synthetic history (~105k reviews, few minutes)
uv run python -m src.data_generation.runner --users 200 --questions 35 --days 120 --seed 42

# 4. Export features -> data/features/training_dataset.csv
uv run python -m src.preprocessing.export_dataset

# 5. Train models -> models/*.joblib + training_report.json
uv run python train.py          # add --tune for hyperparameter search (slower)

# 6. Compare ML vs FSRS baseline -> docs/model_comparison_results.md
uv run python -m src.evaluation.compare_baselines

# 7. Serve predictions on http://127.0.0.1:8000 (docs at /docs)
uv run uvicorn src.inference.app:app --host 127.0.0.1 --port 8000
```

## API

- `POST /predict` — recall probability for one engineered feature vector.
- `POST /recommend-revisions` — ranks candidate questions by lowest predicted
  recall (highest urgency) and tags each with a priority level.
- `GET /health` — liveness probe; `model_loaded` flips to `true` lazily after
  the first predict/recommend call (cold start takes a few seconds).

The API consumes **already-engineered** features — the exact vector defined in
[`src/inference/schemas.py`](src/inference/schemas.py), mirroring
`training_dataset.csv`. Nine fields are required per request:
`difficulty`, `word_count`, `character_count`, `session_duration_minutes`,
`question_position_in_session`, `repetition_number`, `answer_changes`,
`hour_of_day`, `day_of_week`. The remaining history/context fields are optional
and imputed inside the model pipeline, so cold-start users are supported.

## Tests

```bash
uv run pytest tests/
```

Covers feature leakage (strict shift semantics), simulator output bounds,
rating mappers, shared-constant consistency, and API behavior. Trained model
artifacts (`models/*.joblib`) must exist for the API tests; run step 5 first.
