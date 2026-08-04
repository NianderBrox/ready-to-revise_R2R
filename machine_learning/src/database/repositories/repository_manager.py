from sqlalchemy.orm import Session

from database.repositories.notification_repository import NotificationRepository
from database.repositories.question_attempt_repository import QuestionAttemptRepository
from database.repositories.question_repository import QuestionRepository
from database.repositories.question_review_repository import QuestionReviewRepository
from database.repositories.review_schedule_repository import ReviewScheduleRepository
from database.repositories.study_session_repository import StudySessionRepository
from database.repositories.user_repository import UserRepository


class RepositoryManager:

    def __init__(self, session: Session):

        self.users = UserRepository(session)
        self.questions = QuestionRepository(session)
        self.study_sessions = StudySessionRepository(session)
        self.question_reviews = QuestionReviewRepository(session)
        self.question_attempts = QuestionAttemptRepository(session)
        self.review_schedules = ReviewScheduleRepository(session)
        self.notifications = NotificationRepository(session)

    