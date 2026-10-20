# R2R Backend

NestJS 11 API for Ready-to-Revise: auth, study material ingestion, AI question
generation, behavior-derived spaced repetition, and recall predictions.

- Base path `api/v1` · Swagger at `/api/docs`
- Postgres via Prisma (`prisma/schema.prisma`); migrations applied with
  `npx prisma migrate deploy`

## Module map

| Module | Responsibility |
|---|---|
| `auth`, `users`, `health` | JWT auth (access token), profile, liveness; password reset (OTP) + change password |
| `documents` | Multipart upload (20 MB cap), listing, status polling, file download |
| `ai` | Shared Gemini layer: `LLMProvider` + Gemini provider/client, OCR, classification, flashcard, summary, metadata services |
| `document-analysis` | Gemini analysis provider (pdf/images inline; `.docx` via mammoth text extraction), text-extractor service |
| `learning-generation` | `POST documents/{id}/questions` → strict 4-option MCQ generation + persistence |
| `study-items` | CRUD; MCQ options storage; `GET ?type=&due=true` due filter |
| `attachments` | Study-item attachments: create (`POST attachments`) list per study item and delete |
| `subject`, `chapters` | Taxonomy CRUD: subjects and chapters (`Topic`, `DocumentMetadata` extend this) |
| `reviews` | Raw-observable intake (`selectedOptionIndex` + passive timers) → server derives result/confidence → expanding scheduler; review history; `POST reviews/self-grade` for swipe "memorized" |
| `recall-predictions` | Feature building per frozen contract, `GET recommendations?limit=&subjectId=` with scheduler-order fallback |
| `ml-client` | Typed client for the frozen ML HTTP contract (2 s timeout, availability flag, failure cooldown) |
| `dashboard` | `GET dashboard` aggregated stats + at-risk suggestion behind ML availability |
| `analytics` | `GET analytics/overview` + `GET analytics/heatmap` (activity grid) |
| `mail` | No controller: mail service used by auth — Gmail SMTP or Brevo HTTP API (see Environment) |
| `storage` | File storage abstraction: `local` driver (filesystem, `file-extension.util` for MIME → extension) or `s3` driver (Tigris / Cloudflare R2 / Backblaze B2) |

Prisma models (`prisma/schema.prisma`): `User`, `PasswordResetOtp`, `Subject`,
`Chapter`, `Topic`, `StudyItem`, `Review`, `Attachment`, `Document`,
`DocumentMetadata`.

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

See `.env.example`. Server: `PORT` (default 3000), `CORS_ORIGIN`
(comma-separated origins, `*` allowed), `NODE_ENV`.
Required: `DATABASE_URL`, `JWT_SECRET`
(`JWT_EXPIRES_IN`, default 7d), `GEMINI_API_KEY`
(`GEMINI_MODEL`, default `gemini-3.6-flash`).
Storage: `STORAGE_DRIVER=local` (filesystem; `UPLOAD_DIRECTORY`) or `s3`
(`S3_BUCKET`, `S3_ENDPOINT`, `S3_REGION`, `S3_ACCESS_KEY_ID`,
`S3_SECRET_ACCESS_KEY`, `S3_FORCE_PATH_STYLE`, `S3_PREFIX`).
ML: `ML_SERVICE_URL` (default `http://localhost:8000`), `ML_MODEL_NAME`
(default `calibrated_best`), `ML_TIMEOUT_MS`, `ML_FAILURE_COOLDOWN_MS`.
Recommendations: `RECOMMENDATION_DAILY_CAP` (default 20).
Password-reset OTP mail (Gmail): `SMTP_HOST=smtp.gmail.com`,
`SMTP_PORT=587`, `SMTP_USER`/`SMTP_FROM` = your Gmail address,
`SMTP_PASS` = 16-char App Password (Google account with 2SV on;
spaces optional, they are stripped). Without `SMTP_HOST`, OTPs are
only logged, not sent.
Brevo HTTP mail (recommended on Render, where direct SMTP egress is
unreliable): set `BREVO_API_KEY`, `BREVO_SENDER_EMAIL` (a sender
address verified in the Brevo dashboard), and optionally
`BREVO_SENDER_NAME`. When Brevo is fully configured it takes
precedence and SMTP is not used; otherwise the Gmail SMTP settings
above apply. Brevo setup: create an account → add and verify your
Gmail address under Senders → generate a v3 API key → set the three
variables on Render and redeploy.

### Docker (Dockerfile-only deploys)

`.env` is excluded from the image (see `.dockerignore`), so it must be
passed at run time. From the `backend/` directory:

```bash
docker build -t r2r-backend .
docker run -d --name r2r-backend -p 3000:3000 --env-file .env r2r-backend
docker logs r2r-backend | grep -i brevo   # Brevo deployment: expect "Brevo HTTP email enabled"
                                          # (then SMTP lines below are not used)
docker exec r2r-backend env | grep SMTP   # SMTP deployment: expect 5 lines
docker logs r2r-backend | grep -i SMTP    # SMTP deployment: expect "transporter verified"
```

Mail selection at startup: Brevo runs when `BREVO_API_KEY` **and**
`BREVO_SENDER_EMAIL` are set (log line `Brevo HTTP email enabled. OTP
emails will be sent via Brevo.`); otherwise Gmail SMTP is used only when
`SMTP_HOST` is set (log line `SMTP transporter verified.`); with neither,
OTPs are log-only. Brevo API failures surface as 503
`Email service is temporarily unavailable`.

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
npm run ai:test            # Gemini document-analysis smoke (scripts/test-document-analysis.ts)
```

From the repo root, `./scripts/e2e.sh` exercises the full live flow
(register → upload PDF + docx → MCQs → reviews → self-grade → recommendations).
