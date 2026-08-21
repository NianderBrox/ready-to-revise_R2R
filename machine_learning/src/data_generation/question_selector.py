

from __future__ import annotations

from datetime import datetime
from random import Random

from src.data_generation.profiles import QuestionMeta


class QuestionSelector:

    def __init__(
        self,
        rng: Random,
        min_per_session: int,
        max_per_session: int,
    ):
        self.rng = rng

        self.min_per_session = min_per_session

        self.max_per_session = max_per_session

    def select(
        self,
        pool: list[QuestionMeta],
        cards: dict,
        histories: dict,
        now: datetime,
    ) -> list[QuestionMeta]:
        size = self.rng.randint(
            self.min_per_session,
            self.max_per_session,
        )

        due = []

        unseen = []

        reviewed = []

        for question in pool:
            if question.created_at.replace(tzinfo=None) > now:
                continue

            history = histories.get(question.question_id)

            if history is None or history.review_count == 0:
                unseen.append(question)

                continue

            card = cards[question.question_id]

            card_due = card.due.replace(tzinfo=None)

            if card_due <= now:
                due.append((card_due, question))

            reviewed.append(
                (history.last_review_time, question)
            )

        due.sort(key=lambda pair: pair[0])

        reviewed.sort(key=lambda pair: pair[0])

        selected: list[QuestionMeta] = []

        for _, question in due:
            if len(selected) >= size:
                break

            selected.append(question)

        if len(selected) < size and unseen:
            selected.extend(
                self.rng.sample(
                    unseen,
                    min(size - len(selected), len(unseen)),
                )
            )

        if len(selected) < size:
            chosen_ids = {
                q.question_id for q in selected
            }

            for _, question in reviewed:
                if len(selected) >= size:
                    break

                if question.question_id in chosen_ids:
                    continue

                selected.append(question)

                chosen_ids.add(question.question_id)

        return selected
