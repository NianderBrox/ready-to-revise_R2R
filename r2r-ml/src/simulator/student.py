from dataclasses import dataclass, field
from random import Random


@dataclass
class Student:
    """
    Simulates one student in the system.

    These values evolve as the student studies.
    """

    student_id: int
    rng: Random

    # Running statistics
    total_revisions: int = 0
    correct_answers: int = 0

    confidence_sum: float = 0.0
    response_time_sum: float = 0.0

    study_streak: int = 0
    days_since_last_session: int = 0

    preferred_study_hour: int = field(default_factory=lambda: 20)

    def __post_init__(self):
        """
        Give every student a slightly different preferred study hour.
        """
        self.preferred_study_hour = self.rng.randint(6, 22)

    @property
    def success_rate(self) -> float:
        if self.total_revisions == 0:
            return 0.0
        return self.correct_answers / self.total_revisions

    @property
    def average_confidence(self) -> float:
        if self.total_revisions == 0:
            return 0.0
        return self.confidence_sum / self.total_revisions

    @property
    def average_response_time(self) -> float:
        if self.total_revisions == 0:
            return 0.0
        return self.response_time_sum / self.total_revisions

    def update_after_review(
        self,
        correct: bool,
        confidence: int,
        response_time: float,
    ):
        """
        Update the student's running statistics after one review.
        """

        self.total_revisions += 1

        if correct:
            self.correct_answers += 1

        self.confidence_sum += confidence
        self.response_time_sum += response_time

    def new_session(self, gap_days: int):
        """
        Called whenever the student starts a new study session.
        """

        self.days_since_last_session = gap_days

        if gap_days <= 1:
            self.study_streak += 1
        else:
            self.study_streak = 1