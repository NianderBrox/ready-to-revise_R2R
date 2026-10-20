# R2R Android

Kotlin/Jetpack Compose client for Ready-to-Revise: upload study material,
revise AI-generated MCQs, and track behavior-driven readiness — talking only to
the NestJS backend over `api/v1` (raw observables only; the app never
self-reports results or confidence).

- Single Gradle module `:app` · Kotlin 2.4.10 · AGP 9.3.1 · JDK 17
- minSdk 26 · targetSdk 36 · compileSdk 37 · versionName 1.1
- Compose (BOM 2026.06.01, Material 3) · Navigation Compose ·
  Retrofit 3 + kotlinx-serialization · OkHttp 5 · DataStore · WorkManager ·
  Coil · swipe gesture `me.saket.swipe`

## Package map

| Package | Responsibility |
|---|---|
| `data/remote` | Retrofit interfaces (`AuthApi`, `DocumentsApi`, `StudyItemsApi`, `ReviewsApi`, `RecommendationsApi`, `DashboardApi`, `AnalyticsApi`) + DTOs; base URL from `NetworkConstants` → `BuildConfig.BACKEND_URL` |
| `data/local` | `TokenManager` (JWT persistence via DataStore) |
| `data/repository` | Repository implementations bridging API → domain |
| `domain` | Repository interfaces + input validation |
| `di` | `AppContainer`: manual dependency injection |
| `presentation` | MVI screens: `UiEvent` / `UiState` / `UiEffect` + `BaseViewModel`, one folder per feature |
| `quiz` | Revision session engine: `RevisionScreen`, `RevisionSession`, question card model |
| `navigation` | `Screen` definitions + `AppNavGraph` |
| `notification` | Revision reminders: `RevisionReminderWorker`, exact-alarm `SnoozeAlarmReceiver`, `SnoozeReceiver`, `BootReceiver` (re-arms alarms after reboot), `NotificationSettingsScreen` |
| `work` | Upload + processing pipeline in WorkManager |
| `ui` | Theme, typography, shared components |

## Screens

Splash + onboarding/permission gate → auth (`login`, `register`,
`forgot-password`), `dashboard`, `upload`, `preview`/`processing`/`upload
success`, `document preview`, revision quiz, `analytics`, `profile`,
`change-password`, `settings`, `notification settings`. Navigation lives in
`navigation/AppNavGraph.kt`.

## Key behaviors

- **Upload pipeline**: document picker accepts PDF + `.docx`; images are taken
  from the gallery and re-encoded to JPEG → chained WorkManager workers
  (`FileUploadWorker` → `AnalysisPollWorker` → `GenerateQuestionsWorker` →
  `PersistCurationWorker`) with upload notification + cancel action and
  battery-optimization prompt so jobs survive backgrounding.
- **Revision session**: MCQ cards from
  `GET /recommendations`; swiping a card left marks "memorized"; on session
  finish the app batch-commits raw observables (`reviews/`) and swipe commits
  via `reviews/self-grade` — the backend derives confidence and schedules.
- **Reminders**: daily revision notification via WorkManager; exact alarms with
  snooze (`SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`); runtime
  `POST_NOTIFICATIONS` grant flow, settings screen for time toggles.
- **Permissions**: `INTERNET`, `CAMERA`, `POST_NOTIFICATIONS`,
  `FOREGROUND_SERVICE(_DATA_SYNC)`, `SCHEDULE_EXACT_ALARM`,
  `RECEIVE_BOOT_COMPLETED`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.

## Configuration

`BACKEND_URL` is read from `android/local.properties` at build time and baked
into `BuildConfig.BACKEND_URL` (`app/build.gradle.kts`). Default is the
emulator target `http://10.0.2.2:3000/`; the deployed backend is
`https://ready-to-revise-r2r.onrender.com/`. Cleartext HTTP stays enabled for
local dev (`usesCleartextTraffic` in the manifest).

## Run

Open the folder in Android Studio (JDK 17) and Run, or:

```bash
cd android
edit local.properties          # sdk.dir + BACKEND_URL
./gradlew assembleDebug        # build debug APK
./gradlew installDebug         # install on connected device/emulator
```

`test.main.kts` is a standalone (non-Gradle) API contract smoke test
against the backend DTOs.
