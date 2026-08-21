
from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime


@dataclass
class QuestionHistory:

    review_count: int = 0

    correct_count: int = 0

    confidence_sum: float = 0.0

    response_time_sum: float = 0.0

    hesitation_sum: float = 0.0

    last_review_time: datetime | None = None

    def update(
        self,
        *,
        correct: bool,
        confidence_score: float,
        response_time_seconds: float,
        hesitation_seconds: float,
        review_time: datetime,
    ) -> None:
        self.review_count += 1

        if correct:
            self.correct_count += 1

        self.confidence_sum += confidence_score

        self.response_time_sum += response_time_seconds

        self.hesitation_sum += hesitation_seconds

        self.last_review_time = review_time

    @property
    def success_rate(self) -> float | None:
        if self.review_count == 0:
            return None

        return self.correct_count / self.review_count

    @property
    def average_confidence(self) -> float | None:
        if self.review_count == 0:
            return None

        return self.confidence_sum / self.review_count

    @property
    def average_response_time(self) -> float | None:
        if self.review_count == 0:
            return None

        return self.response_time_sum / self.review_count

    @property
    def average_hesitation(self) -> float | None:
        if self.review_count == 0:
            return None

        return self.hesitation_sum / self.review_count


@dataclass
class UserHistory:

    histories: dict = field(default_factory=dict)

    def get(
        self,
        question_id,
    ) -> QuestionHistory:
        history = self.histories.get(question_id)

        if history is None:
            history = QuestionHistory()

            self.histories[question_id] = history

        return history
