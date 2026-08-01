from dataclasses import dataclass
from datetime import datetime
from uuid import UUID

from machine_learning.src.database.enums import Confidence


@dataclass
class Review:

    id: UUID

    user_id: UUID

    question_id: UUID

    session_id: UUID

    review_time: datetime

    previous_review_time: datetime | None

    correct: bool

    confidence: Confidence

    response_time_seconds: float

    hesitation_seconds: float

    answer_changes: int

    repetition_number: int
# class ReviewGenerator:

#     def generate_review(...):

#         ...

#     def _compute_recall_probability(...):

#         ...

#     def _generate_correctness(...):

#         ...

#     def _generate_confidence(...):

#         ...

#     def _generate_response_time(...):

#         ...

#     def _generate_hesitation(...):

#         ...

#     def _generate_answer_changes(...):

#         ...

#     def _generate_scheduler_rating(...):

#         ...