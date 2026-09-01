# Ready to Revise (R2R)

<p align="center">
  <img src="android/app/src/main/assets/AppBanner4.png" alt="Ready to Revise banner" width="440" />
</p>

AI-powered spaced-repetition study app: upload study material, get AI-generated
quizzes, and revise exactly what you're about to forget.

```
Upload PDF/image/docx ─► Gemini generates MCQs ─► daily slipping-soon session ─► behavior-derived confidence
     ▲                                                              │
     │                                                              ▼
Scheduler tracks memory intervals ◄───────────────── ML + forgetting-dates pick what you see
```

## Components

| Folder | What it is | Stack | Port |
|---|---|---|---|
| [`android/`](android/) | Mobile app (end-user devices) | Kotlin, Jetpack Compose, Retrofit | — |
| [`backend/`](backend/) | REST API + orchestration | NestJS 11, Prisma, PostgreSQL | 3000 |
| [`machine_learning/`](machine_learning/) | Recall-prediction service | Python 3.11, FastAPI, scikit-learn | 8000 |

**Topology:** backend + ML run co-located on one server (ML binds `127.0.0.1`);
PostgreSQL location is independent (`DATABASE_URL`); the Android app talks only
to the backend over HTTPS.

```
Upload PDF/image/docx ─► Gemini generates MCQs ─► daily slipping-soon session ─► behavior-derived confidence
     ▲                                                              │
     │                                                              ▼
Scheduler tracks memory intervals ◄───────────────── ML + forgetting-dates pick what you see
```

## How it fits together

1. **Upload** → `POST api/v1/documents` stores the file; Gemini analyzes it.
2. **Generate** → `POST api/v1/documents/:id/questions` creates MCQ study items;
   they are revisable **immediately** (nominal forgetting date = next day).
3. **Serve** → each day's session = questions whose predicted
   forgetting date falls within the next 48 h, earliest-slipperiest first,
   capped at 20; just-answered questions rest ≥20 h. The rule-based scheduler
   stays as internal plumbing (model features, offline fallback, analytics) and
   never gates serving.
4. **Quiz** → user taps MCQ options. The app sends *raw observables only*
   (selected option + passive timing signals). No self-rating buttons exist.
   Swiping a card left toggles "memorized" locally; marks batch-commit as
   `MEMORIZED` reviews (`reviews/self-grade`, GOOD interval × 1.5) when the
   session finishes.
5. **Derive** → backend grades the answer and computes a confidence score from
   behavior (correctness 0.40 · response speed 0.25 · hesitation 0.20 · answer
   stability 0.15), mapping it to a scheduler grade.
6. **Schedule** → expanding spaced-repetition intervals update each item's
   memory state (`nextReviewAt`) — feeding both future predictions and the
   offline fallback ordering.
7. **Prioritize** → the ML service predicts recall probability per candidate;
   items are served by earliest predicted forgetting date, ML ordering on top.
   Falls back to pure forgetting-date order when ML is unavailable or untrained.

One database (`r2r_db`): Prisma owns the `public` schema; an `ml` schema of
read-only views feeds offline model training (`train.py`). The inference API is
stateless — it never touches the DB at runtime.

## Quickstart

### Full stack (docker compose)

```bash
cp .env.example .env                  # set JWT_SECRET, GEMINI_API_KEY
docker compose up --build             # postgres + ml + backend (migrate deploy runs automatically)
```

Backend: http://localhost:3000 (Swagger at `/api/docs`) · ML: 127.0.0.1:8000 ·
Postgres host port: 5433.

### Individual services

#### 0. Database

```bash
docker compose up -d postgres        # r2r-postgres on host port 5433
```

#### 1. Backend

```bash
cd backend
cp .env.example .env                 # fill GEMINI_API_KEY, JWT_SECRET, DATABASE_URL
npm ci
npx prisma migrate deploy            # or: npx prisma db push for dev
npx prisma generate
npm run start:dev                    # http://localhost:3000 — Swagger: /api/docs
```

Required env: `DATABASE_URL`, `JWT_SECRET`, `GEMINI_API_KEY`, `GEMINI_MODEL`,
optional `ML_SERVICE_URL` (default `http://localhost:8000`).

#### 2. Machine learning service

```bash
cd machine_learning
cp .env.example .env                 # DB_* needed for TRAINING only
uv sync                              # or: pip install -r requirements.txt
python train.py --tune               # operator step: writes models/*.joblib
uvicorn src.inference.app:app --host 127.0.0.1 --port 8000
```

Until trained, `/predict` returns 503 and the backend automatically falls back
to scheduler ordering — everything else keeps working.

#### 3. Android app

Open `android/` in Android Studio; set `BACKEND_URL` in `local.properties`
(default emulator target `http://10.0.2.2:3000/`), then Run.

### Verify (headless)

```bash
./scripts/e2e.sh                     # 19 checks against localhost:3000:
                                     # register → upload PDF + generated .docx →
                                     # MCQs → +24h stamp (internal) → reviews w/ derived
                                     # confidence → swipe self-grade (MEMORIZED) →
                                     # slipping-soon recommendations (ml or
                                     # forgetting-date fallback) → refractory check
```

Backend must be running (`npm run start:dev`); the ML service is optional —
recommendations then report `source: "scheduler"`.