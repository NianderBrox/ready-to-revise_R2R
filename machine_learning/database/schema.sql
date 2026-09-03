-- ============================================================
-- R2R Database Schema v2.0
-- ============================================================

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- ENUM TYPES
-- ============================================================

CREATE TYPE question_difficulty_enum AS ENUM (
    'EASY',
    'MEDIUM',
    'HARD'
);

CREATE TYPE confidence_level_enum AS ENUM (
    'LOW',
    'MEDIUM',
    'HIGH'
);

CREATE TYPE scheduler_rating_enum AS ENUM (
    'AGAIN',
    'HARD',
    'GOOD',
    'EASY'
);

-- ============================================================
-- USERS
-- ============================================================

CREATE TABLE users (

    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    created_at TIMESTAMP NOT NULL,

    timezone VARCHAR(50) NOT NULL
);

-- ============================================================
-- QUESTIONS
-- ============================================================

CREATE TABLE questions (

    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID NOT NULL
        REFERENCES users(id)
        ON DELETE CASCADE,

    subject VARCHAR(100) NOT NULL,

    topic VARCHAR(100) NOT NULL,

    -- Difficulty of the content itself
    question_difficulty question_difficulty_enum NOT NULL,

    word_count INTEGER NOT NULL
        CHECK (word_count >= 0),

    character_count INTEGER NOT NULL
        CHECK (character_count >= 0),

    created_at TIMESTAMP NOT NULL
);

-- ============================================================
-- STUDY SESSIONS
-- ============================================================

CREATE TABLE study_sessions (

    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID NOT NULL
        REFERENCES users(id)
        ON DELETE CASCADE,

    started_at TIMESTAMP NOT NULL,

    ended_at TIMESTAMP NOT NULL,

    CHECK (ended_at >= started_at)
);

-- ============================================================
-- QUESTION REVIEWS
-- ============================================================

CREATE TABLE question_reviews (

    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID NOT NULL
        REFERENCES users(id)
        ON DELETE CASCADE,

    question_id UUID NOT NULL
        REFERENCES questions(id)
        ON DELETE CASCADE,

    session_id UUID NOT NULL
        REFERENCES study_sessions(id)
        ON DELETE CASCADE,

    review_time TIMESTAMP NOT NULL,

    previous_review_time TIMESTAMP,

    correct BOOLEAN NOT NULL,

    -- User-selected confidence
    confidence confidence_level_enum NOT NULL,

    -- Continuous inferred confidence score (0-1), distinct from discrete enum
    confidence_score REAL NOT NULL
        CHECK (confidence_score BETWEEN 0 AND 1),

    response_time_seconds Float NOT NULL
        CHECK (response_time_seconds >= 0),

    hesitation_seconds Float NOT NULL
        CHECK (hesitation_seconds >= 0),

    answer_changes SMALLINT NOT NULL
        CHECK (answer_changes >= 0),

    repetition_number INTEGER NOT NULL
        CHECK (repetition_number >= 0)
);

-- ============================================================
-- QUESTION ATTEMPTS
-- ============================================================

CREATE TABLE question_attempts (

    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    review_id UUID NOT NULL
        REFERENCES question_reviews(id)
        ON DELETE CASCADE,

    question_position INTEGER NOT NULL
        CHECK (question_position > 0)
);

-- ============================================================
-- REVIEW SCHEDULES
-- ============================================================
CREATE TABLE review_schedules (

    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    review_id UUID NOT NULL
        REFERENCES question_reviews(id)
        ON DELETE CASCADE,

    scheduler_name VARCHAR(50) NOT NULL,

    input_rating scheduler_rating_enum,

    recall_probability REAL
        CHECK (
            recall_probability IS NULL OR
            (recall_probability BETWEEN 0 AND 1)
        ),

    scheduled_interval_days REAL NOT NULL
        CHECK (scheduled_interval_days > 0),

    next_review_at TIMESTAMP NOT NULL,

    fsrs_state SMALLINT,

    fsrs_step SMALLINT,

    fsrs_stability REAL,

    fsrs_difficulty REAL,

    created_at TIMESTAMP NOT NULL,

    UNIQUE (review_id, scheduler_name)
);


-- ============================================================
-- NOTIFICATIONS
-- ============================================================

CREATE TABLE notifications (

    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    user_id UUID NOT NULL
        REFERENCES users(id)
        ON DELETE CASCADE,

    sent_at TIMESTAMP NOT NULL,

    opened BOOLEAN NOT NULL,

    opened_at TIMESTAMP
);



