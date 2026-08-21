
from __future__ import annotations

import argparse
from datetime import UTC, datetime, timedelta
from random import Random
from uuid import uuid4

from sqlalchemy import insert

from src.data_generation.config import (
    DEFAULT_GENERATION_CONFIG,
    GenerationConfig,
)
from src.data_generation.profiles import (
    make_question_pool,
    make_user_profile,
    pick_timezone,
)
from src.data_generation.question_selector import QuestionSelector
from src.data_generation.review_engine import ReviewEngine
from src.data_generation.review_history import UserHistory
from src.data_generation.session_planner import SessionPlanner
from src.database.connection import engine
from src.database.models.question import Question
from src.database.models.question_attempt import QuestionAttempt
from src.database.models.question_review import QuestionReview
from src.database.models.review_schedule import ReviewSchedule
from src.database.models.study_session import StudySession
from src.database.models.user import User
from src.utils.config import RANDOM_SEED


class GenerationBuffers:


    def __init__(self):
        self.users: list[dict] = []

        self.questions: list[dict] = []

        self.sessions: list[dict] = []

        self.reviews: list[dict] = []

        self.attempts: list[dict] = []

        self.schedules: list[dict] = []

    def extend_user_rows(
        self,
        *,
        user: dict,
        questions: list[dict],
        sessions: list[dict],
        reviews: list[dict],
        attempts: list[dict],
        schedules: list[dict],
    ) -> None:
        self.users.append(user)

        self.questions.extend(questions)


        self.sessions.extend(sessions)

        self.reviews.extend(reviews)

        self.attempts.extend(attempts)

        self.schedules.extend(schedules)

    def total(self) -> int:
        return (
            len(self.users)
            + len(self.questions)
            + len(self.sessions)
            + len(self.reviews)
            + len(self.attempts)
            + len(self.schedules)
        )

    def flush(self, connection) -> None:
        if self.users:
            connection.execute(insert(User), self.users)

        if self.questions:
            connection.execute(insert(Question), self.questions)

        if self.sessions:
            connection.execute(insert(StudySession), self.sessions)

        if self.reviews:
            connection.execute(insert(QuestionReview), self.reviews)

        if self.attempts:
            connection.execute(insert(QuestionAttempt), self.attempts)

        if self.schedules:
            connection.execute(insert(ReviewSchedule), self.schedules)

        self.users.clear()

        self.questions.clear()

        self.sessions.clear()

        self.reviews.clear()

        self.attempts.clear()

        self.schedules.clear()


def generate(
    *,
    n_users: int,
    n_questions: int,
    history_days: int,
    seed: int,
    config: GenerationConfig = DEFAULT_GENERATION_CONFIG,
) -> dict:
    rng = Random(seed)

    selector = QuestionSelector(
        rng=rng,
        min_per_session=config.min_questions_per_session,
        max_per_session=config.max_questions_per_session,
    )

    buffers = GenerationBuffers()

    span_start = (
        datetime.now(UTC)
        .replace(minute=0, second=0, microsecond=0)
        - timedelta(days=history_days)
    )

    stats = {
        "users": 0,
        "questions": 0,
        "sessions": 0,
        "reviews": 0,
    }

    started_at = datetime.now(UTC).replace(tzinfo=None)

    with engine.begin() as connection:
        for user_index in range(n_users):
            _generate_user(
                rng=rng,
                selector=selector,
                user_index=user_index,
                n_questions=n_questions,
                span_start=span_start,
                config=config,
                buffers=buffers,
                stats=stats,
            )

            if buffers.total() >= config.db_chunk_size:
                buffers.flush(connection)
                print(
                    f"  user {user_index + 1}/{n_users} | "
                    f"reviews so far: {stats['reviews']}"
                )

        buffers.flush(connection)

    stats["elapsed_seconds"] = round(
        (datetime.now(UTC).replace(tzinfo=None) - started_at).total_seconds(),
        1,
    )

    return stats


def _generate_user(
    *,
    rng: Random,
    selector: QuestionSelector,
    user_index: int,
    n_questions: int,
    span_start: datetime,
    config: GenerationConfig,
    buffers: GenerationBuffers,
    stats: dict,
) -> None:
    profile = make_user_profile(
        rng,
        pick_timezone(rng),
    )

    account_created = span_start - timedelta(
        days=profile.account_created_at_days_before_start,
    )

    user_row = {
        "id": profile.user_id,
        "created_at": account_created.replace(tzinfo=None),
        "timezone": profile.timezone,
    }

    jitter = rng.uniform(0.8, 1.2)

    bank_size = max(int(n_questions * jitter), 5)

    stagger_days = max(config.history_days // max(bank_size, 1), 1)

    question_rows, pool = make_question_pool(
        rng=rng,
        user_id=profile.user_id,
        n_questions=bank_size,
        created_at_fn=lambda index: (
            span_start
            - timedelta(days=rng.randint(0, 14))
            + timedelta(days=(index * stagger_days) // 2)
        ).replace(tzinfo=None),
    )

    sessions = SessionPlanner(
        rng=rng,
        profile=profile,
        start=span_start,
        config=config,
    ).build()

    review_engine = ReviewEngine(rng)

    histories = UserHistory()

    session_rows: list[dict] = []

    review_rows: list[dict] = []

    attempt_rows: list[dict] = []

    schedule_rows: list[dict] = []

    for session_start in sessions:
        naive_now = session_start.replace(tzinfo=None)

        picks = selector.select(
            pool=pool,
            cards=review_engine.cards,
            histories=histories.histories,
            now=naive_now,
        )

        if not picks:
            continue

        session_id = uuid4()

        cursor = session_start

        for position, question_meta in enumerate(picks):
            history = histories.get(question_meta.question_id)

            review_row, attempt_row, schedule_row = (
                review_engine.review_question(
                    user_id=profile.user_id,
                    question=question_meta,
                    session_id=session_id,
                    position=position + 1,
                    history=history,
                    review_time=cursor,
                )
            )

            review_rows.append(review_row)

            attempt_rows.append(attempt_row)

            schedule_rows.append(schedule_row)

            cursor = cursor + timedelta(
                seconds=review_row["response_time_seconds"]
                + rng.uniform(
                    config.inter_question_gap_min,
                    config.inter_question_gap_max,
                ),
            )

        tail_seconds = rng.uniform(
            0,
            config.session_tail_buffer_max,
        )

        session_rows.append(
            {
                "id": session_id,
                "user_id": profile.user_id,
                "started_at": session_start.replace(tzinfo=None),
                "ended_at": (cursor + timedelta(seconds=tail_seconds)).replace(
                    tzinfo=None
                ),
            }
        )

        stats["reviews"] += len(picks)

        stats["sessions"] += 1

    buffers.extend_user_rows(
        user=user_row,
        questions=question_rows,
        sessions=session_rows,
        reviews=review_rows,
        attempts=attempt_rows,
        schedules=schedule_rows,
    )

    stats["users"] += 1

    stats["questions"] += bank_size


def main() -> None:
    parser = argparse.ArgumentParser(
        description="Generate synthetic R2R data into PostgreSQL.",
    )

    parser.add_argument(
        "--users",
        type=int,
        default=DEFAULT_GENERATION_CONFIG.n_users,
    )

    parser.add_argument(
        "--questions",
        type=int,
        default=40,
        help="Question bank size per user.",
    )

    parser.add_argument(
        "--days",
        type=int,
        default=DEFAULT_GENERATION_CONFIG.history_days,
    )

    parser.add_argument("--seed", type=int, default=RANDOM_SEED)

    args = parser.parse_args()

    print(
        f"Generating data for {args.users} users "
        f"({args.questions} questions each, "
        f"{args.days} days of history, seed={args.seed})..."
    )

    stats = generate(
        n_users=args.users,
        n_questions=args.questions,
        history_days=args.days,
        seed=args.seed,
    )

    print("Done.")

    print(
        f"users={stats['users']} questions={stats['questions']} "
        f"sessions={stats['sessions']} reviews={stats['reviews']} "
        f"in {stats['elapsed_seconds']}s"
    )


if __name__ == "__main__":
    main()
