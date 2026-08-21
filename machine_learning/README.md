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

## Quickstart

```bash
# 1. Environment (Python 3.11+, uv)
uv sync

# 2. PostgreSQL: set credentials in .env, then load the schema
psql -f database/reset_database.sql -f database/schema.sql

# 3. Generate synthetic history (~105k reviews, few minutes)
uv run python -m src.data_generation.runner --users 200 --questions 35 --days 120 --seed 42

# 4. Export features -> data/features/training_dataset.csv
uv run python -m src.preprocessing.export_dataset

# 5. Train models -> models/*.joblib + training_report.json
uv run python train.py

# 6. Compare ML vs FSRS baseline -> docs/model_comparison_results.md
uv run python -m src.evaluation.compare_baselines

# 7. Serve predictions on http://localhost:8000 (docs at /docs)
uv run uvicorn src.inference.app:app --host 0.0.0.0 --port 8000
```

## API

- `POST /predict` — recall probability for one engineered feature vector.
- `POST /recommend-revisions` — ranks candidate questions by lowest predicted
  recall (highest urgency) and tags each with a priority level.
- `GET /health` — liveness probe.

The API consumes **already-engineered** features (same 36 columns as
`training_dataset.csv`; see `src/inference/schemas.py`). Missing optional
history fields are imputed inside the model pipeline, so cold-start users are
supported.

## Tests

```bash
uv run pytest tests/
```

Covers feature leakage (strict shift semantics), simulator output bounds,
rating mappers, shared-constant consistency, and API behavior. Trained model
artifacts (`models/*.joblib`) must exist for the API tests; run step 5 first.
