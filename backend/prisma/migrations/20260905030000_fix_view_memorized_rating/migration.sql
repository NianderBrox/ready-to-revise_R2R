-- Fix ml.question_reviews input_rating cast.
--
-- The view casts Review.result to ml.scheduler_rating_enum, but Review can
-- hold 'MEMORIZED' (added by migration 20260822000003) while the ML enum is
-- only {AGAIN,HARD,GOOD,EASY}. SELECT * on the view therefore threw
-- InvalidTextRepresentation for MEMORIZED rows, breaking any flat read (the
-- ML real-data exporter). input_rating is scheduler-facing (not a training
-- feature), so map MEMORIZED -> NULL. Column name/type unchanged.

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
    CASE r."result"
        WHEN 'MEMORIZED' THEN NULL
        ELSE r."result"::TEXT::"ml"."scheduler_rating_enum"
    END AS input_rating,
    r."intervalDays" AS scheduled_interval_days,
    r."nextReviewAt" AS next_review_at,
    EXTRACT(HOUR FROM r."reviewedAt")::SMALLINT AS hour_of_day,
    EXTRACT(DOW FROM r."reviewedAt")::SMALLINT AS day_of_week,
    CASE si."difficulty"
        WHEN 'EASY' THEN 'EASY'::"ml"."question_difficulty_enum"
        WHEN 'MEDIUM' THEN 'MEDIUM'::"ml"."question_difficulty_enum"
        WHEN 'HARD' THEN 'HARD'::"ml"."question_difficulty_enum"
        ELSE NULL
    END AS difficulty,
    COALESCE(t."name", '') AS topic,
    COALESCE(s."name", '') AS subject,
    LENGTH(COALESCE(si."title", ''))::INTEGER AS character_count,
    CASE
        WHEN TRIM(COALESCE(si."title", '')) = '' THEN 0
        ELSE COALESCE(
            CARDINALITY(REGEXP_SPLIT_TO_ARRAY(
                TRIM(COALESCE(si."title", '')), '\s+'
            )),
            0
        )
    END::INTEGER AS word_count,
    r."sessionDurationMinutes"::DOUBLE PRECISION AS session_duration_minutes,
    r."questionPositionInSession" AS question_position_in_session,
    r."recallProbability" AS recall_probability
FROM "Review" r
JOIN "StudyItem" si ON si."id" = r."studyItemId"
LEFT JOIN "Topic" t ON t."id" = si."topicId"
LEFT JOIN "Chapter" c ON c."id" = t."chapterId"
LEFT JOIN "Subject" s ON s."id" = c."subjectId";

-- Restore read-only grants in case the view recreate dropped them.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'ml_readonly') THEN
        GRANT SELECT ON "ml"."question_reviews" TO "ml_readonly";
    END IF;
END $$;