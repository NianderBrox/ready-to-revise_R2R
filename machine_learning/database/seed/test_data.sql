-- database/seed/test_data.sql


INSERT INTO users (
    id,
    created_at,
    timezone
)
VALUES
(
    '11111111-1111-1111-1111-111111111111',
    CURRENT_TIMESTAMP,
    'America/New_York'
);

INSERT INTO questions (
    id,
    user_id,
    subject,
    topic,
    difficulty,
    word_count,
    character_count,
    created_at
)
VALUES
(
    '22222222-2222-2222-2222-222222222222',
    '11111111-1111-1111-1111-111111111111',
    'Biology',
    'Cell Structure',
    2,
    120,
    750,
    CURRENT_TIMESTAMP
);