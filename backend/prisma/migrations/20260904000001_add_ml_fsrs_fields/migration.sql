-- FSRS / ML scheduling columns for the Review model.
-- intervalDays widens INT -> DOUBLE PRECISION because the ML /schedule-review
-- endpoint returns fractional intervals (e.g. 27.3666).

-- The ml.question_reviews view depends on "intervalDays", so Postgres forbids
-- altering its type until the view is dropped. Drop it, apply the change,
-- then recreate it with an identical definition (the widened column type
-- propagates into the view's scheduled_interval_days automatically).
DROP VIEW "ml"."question_reviews";

-- AlterTable
ALTER TABLE "Review" ALTER COLUMN "intervalDays" SET DATA TYPE DOUBLE PRECISION USING "intervalDays"::DOUBLE PRECISION;

-- Add FSRS / ML state columns (persist card state across reviews)
ALTER TABLE "Review" ADD COLUMN "recallProbability" DOUBLE PRECISION;
ALTER TABLE "Review" ADD COLUMN "fsrsState" INTEGER;
ALTER TABLE "Review" ADD COLUMN "fsrsStep" INTEGER;
ALTER TABLE "Review" ADD COLUMN "fsrsStability" DOUBLE PRECISION;
ALTER TABLE "Review" ADD COLUMN "fsrsDifficulty" DOUBLE PRECISION;

-- Recreate the ml.question_reviews view (definition unchanged; the
-- intervalDays column now resolves to DOUBLE PRECISION in the view).
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

-- Restore read-only grants lost when the view was recreated (see 20260822000002).
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'ml_readonly') THEN
        GRANT SELECT ON "ml"."question_reviews" TO "ml_readonly";
    END IF;
END $$;
