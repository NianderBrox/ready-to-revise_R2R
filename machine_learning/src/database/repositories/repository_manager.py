from sqlalchemy.orm import Session

from src.database.repositories.notification_repository import NotificationRepository
from src.database.repositories.question_attempt_repository import (
    QuestionAttemptRepository,
)
from src.database.repositories.question_repository import QuestionRepository
from src.database.repositories.question_review_repository import (
    QuestionReviewRepository,
)
from src.database.repositories.review_schedule_repository import (
    ReviewScheduleRepository,
)
from src.database.repositories.study_session_repository import StudySessionRepository
from src.database.repositories.user_repository import UserRepository


class RepositoryManager:

    def __init__(self, session: Session):

        self.users = UserRepository(session)
        self.questions = QuestionRepository(session)
        self.study_sessions = StudySessionRepository(session)
        self.question_reviews = QuestionReviewRepository(session)
        self.question_attempts = QuestionAttemptRepository(session)
        self.review_schedules = ReviewScheduleRepository(session)
        self.notifications = NotificationRepository(session)

    