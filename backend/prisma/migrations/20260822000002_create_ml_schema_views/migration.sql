-- Create the `ml` schema exposing read-only views over Prisma-owned tables
-- (Decision D3). Shapes mirror machine_learning/database/schema.sql so the
-- training pipeline can consume real data without touching backend code paths.
-- The ml_readonly ROLE is created separately by scripts/create_ml_readonly.sql
-- (needs CREATEROLE); grants below activate automatically once it exists.

CREATE SCHEMA IF NOT EXISTS "ml";

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type t
                   JOIN pg_namespace n ON n.oid = t.typnamespace
                   WHERE t.typname = 'confidence_level_enum' AND n.nspname = 'ml') THEN
        CREATE TYPE "ml"."confidence_level_enum" AS ENUM ('LOW', 'MEDIUM', 'HIGH');
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type t
                   JOIN pg_namespace n ON n.oid = t.typnamespace
                   WHERE t.typname = 'question_difficulty_enum' AND n.nspname = 'ml') THEN
        CREATE TYPE "ml"."question_difficulty_enum" AS ENUM ('EASY', 'MEDIUM', 'HARD');
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type t
                   JOIN pg_namespace n ON n.oid = t.typnamespace
                   WHERE t.typname = 'scheduler_rating_enum' AND n.nspname = 'ml') THEN
        CREATE TYPE "ml"."scheduler_rating_enum" AS ENUM ('AGAIN', 'HARD', 'GOOD', 'EASY');
    END IF;
END $$;

CREATE OR REPLACE VIEW "ml"."users" AS
SELECT
    u."id",
    u."createdAt" AS created_at,
    'UTC'::VARCHAR(50) AS timezone
FROM "User" u;

CREATE OR REPLACE VIEW "ml"."questions" AS
SELECT
    si."id",
    si."userId" AS user_id,
    COALESCE(sub.name, 'general') AS subject,
    COALESCE(t.name, 'general') AS topic,
    COALESCE(si."difficulty", 'MEDIUM'::"Difficulty")::TEXT AS question_difficulty,
    COALESCE(array_length(regexp_split_to_array(trim(si."content"), '\s+'), 1), 0) AS word_count,
    COALESCE(length(si."content"), 0) AS character_count,
    si."correctAnswerIndex" AS correct_answer_index,
    si."options",
    si."createdAt" AS created_at
FROM "StudyItem" si
LEFT JOIN "Topic" t ON t."id" = si."topicId"
LEFT JOIN "Chapter" ch ON ch."id" = t."chapterId"
LEFT JOIN "Subject" sub ON sub."id" = ch."subjectId"
WHERE si."type" = 'QUESTION';

CREATE OR REPLACE VIEW "ml"."question_reviews" AS
SELECT
    r."id",
    r."studyItemId" AS question_id,
    si."userId" AS user_id,
    r."sessionId" AS session_id,
    r."reviewedAt" AS review_time,
    LAG(r."reviewedAt") OVER (
        PARTITION BY r."studyItemId" ORDER BY r."reviewedAt"
    ) AS previous_review_time,
    COALESCE(r."isCorrect", false) AS correct,
    CASE
        WHEN r."confidenceScore" IS NULL THEN NULL
        WHEN r."confidenceScore" < 0.40 THEN 'LOW'
        WHEN r."confidenceScore" < 0.75 THEN 'MEDIUM'
        ELSE 'HIGH'
    END::"ml"."confidence_level_enum" AS confidence,
    ROUND(r."confidenceScore"::NUMERIC, 4) AS confidence_score,
    (r."responseTimeMs"::DOUBLE PRECISION / 1000.0) AS response_time_seconds,
    (r."hesitationMs"::DOUBLE PRECISION / 1000.0) AS hesitation_seconds,
    COALESCE(r."answerChanges", 0)::SMALLINT AS answer_changes,
    (ROW_NUMBER() OVER (
        PARTITION BY r."studyItemId" ORDER BY r."reviewedAt"
    ) - 1)::INTEGER AS repetition_number,
    r."result"::TEXT::"ml"."scheduler_rating_enum" AS input_rating,
    r."intervalDays" AS scheduled_interval_days,
    r."nextReviewAt" AS next_review_at,
    EXTRACT(HOUR FROM r."reviewedAt")::SMALLINT AS hour_of_day,
    EXTRACT(DOW FROM r."reviewedAt")::SMALLINT AS day_of_week
FROM "Review" r
JOIN "StudyItem" si ON si."id" = r."studyItemId";

CREATE OR REPLACE VIEW "ml"."study_sessions" AS
SELECT
    r."sessionId" AS id,
    si."userId" AS user_id,
    MIN(r."reviewedAt") AS started_at,
    MAX(r."reviewedAt") AS ended_at,
    COUNT(*)::INTEGER AS review_count
FROM "Review" r
JOIN "StudyItem" si ON si."id" = r."studyItemId"
WHERE r."sessionId" IS NOT NULL
GROUP BY r."sessionId", si."userId";

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'ml_readonly') THEN
        GRANT USAGE ON SCHEMA "ml" TO "ml_readonly";
        GRANT SELECT ON ALL TABLES IN SCHEMA "ml" TO "ml_readonly";
        EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA "ml" GRANT SELECT ON TABLES TO "ml_readonly"';
    END IF;
END $$;
