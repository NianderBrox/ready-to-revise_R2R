# R2R Backend

NestJS 11 API for Ready-to-Revise: auth, study material ingestion, AI question
generation, behavior-derived spaced repetition, and recall predictions.

- Base path `api/v1` · Swagger at `/api/docs`
- Postgres via Prisma (`prisma/schema.prisma`); migrations applied with
  `npx prisma migrate deploy`

## Module map

| Module | Responsibility |
|---|---|
| `auth`, `users`, `health` | JWT auth (access token), profile, liveness |
| `documents` | Multipart upload (20 MB cap), listing, status polling, file download |
| `document-analysis` | Gemini analysis provider (pdf/images inline; `.docx` via mammoth text extraction), text-extractor service |
| `storage` | Local file storage keyed per document (`file-extension.util` maps MIME → extension) |
| `learning-generation` | `POST documents/{id}/questions` → strict 4-option MCQ generation + persistence |
| `study-items` | CRUD; MCQ options storage; `GET ?type=&due=true` due filter |
| `reviews` | Raw-observable intake (`selectedOptionIndex` + passive timers) → server derives result/confidence → expanding scheduler; `POST reviews/self-grade` for swipe "memorized" |
| `ml-client` | Typed client for the frozen ML HTTP contract (2 s timeout, availability flag) |
| `recall-predictions` | Feature building per frozen contract, `GET recommendations?limit=&subjectId=` with scheduler-order fallback |
| `dashboard` | Aggregated stats + at-risk suggestion behind ML availability |

## Key behaviors

- **No self-report**: clients never send results or confidence; both are derived
  from answer correctness and passive telemetry (weights ported from the ML
  simulator; missing telemetry normalizes to neutral).
- **Scheduler**: AGAIN=1d; base HARD=3 / GOOD=7 / EASY=14; multipliers 1.2 / 1.9
  / 2.6 capped at 365d. First revision of a generated question is stamped
  +24h at creation.
- **Swipe self-grade**: `POST api/v1/reviews/self-grade`
  `{studyItemId, sessionId?, responseTimeMs?}` records a `MEMORIZED` review ⇒
  baseline GOOD interval × `MEMORY_MULTIPLIER = 1.5` (first memorize = 11d,
  then compounding; same 365d cap).
- **Uploads**: accepted types are PDF, PNG/JPEG images and `.docx` only
  (`FileValidationPipe`, 20 MB limit). `.docx` is analyzed by extracting raw
  text with mammoth and sending it to Gemini as a plain-text part; legacy `.doc`
  is intentionally rejected.
- **Graceful ML degradation**: when the ML service is down or untrained,
  recommendations fall back to scheduler order (`source: "scheduler"`).

## Environment

See `.env.example`. Required: `DATABASE_URL`, `JWT_SECRET`,
`GEMINI_API_KEY`; optional: `GEMINI_MODEL`, `ML_SERVICE_URL`,
`ML_MODEL_NAME`, `ML_TIMEOUT_MS`.

## Commands

```bash
npm ci
npx prisma generate        # fresh checkout
npx prisma migrate deploy  # apply migrations (no dev DB edits here)
npm run start:dev          # http://localhost:3000 (watch mode)
npm run build              # production build
npm run lint               # eslint
npm test                   # unit tests
npm run test:e2e           # requires a reachable DATABASE_URL
```

From the repo root, `./scripts/e2e.sh` exercises the full live flow
(register → upload PDF + docx → MCQs → reviews → self-grade → recommendations).
