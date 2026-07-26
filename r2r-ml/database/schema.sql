-- ============================================================
-- R2R Database Schema v1.0
-- ============================================================

-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

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

    difficulty SMALLINT NOT NULL
        CHECK (difficulty BETWEEN 1 AND 3),

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

    confidence SMALLINT NOT NULL
        CHECK (confidence BETWEEN 1 AND 3),

    response_time_seconds REAL NOT NULL
        CHECK (response_time_seconds >= 0),

    hesitation_seconds REAL NOT NULL
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