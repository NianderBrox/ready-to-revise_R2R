
from __future__ import annotations

from datetime import datetime, timedelta
from random import Random
from zoneinfo import ZoneInfo

from src.data_generation.config import GenerationConfig
from src.data_generation.profiles import UserProfile


class SessionPlanner:


    def __init__(
        self,
        rng: Random,
        profile: UserProfile,
        start: datetime,
        config: GenerationConfig,
    ):
        self.rng = rng

        self.profile = profile

        self.start = start

        self.config = config

        self.tz = ZoneInfo(profile.timezone)

    def build(self) -> list[datetime]:
        sessions: list[datetime] = []

        current_date = (self.start + timedelta(days=1)).date()

        end_date = self.start.date() + timedelta(
            days=self.config.history_days,
        )

        while current_date <= end_date:
            local_start = self._local_session_datetime(current_date)

            sessions.append(
                local_start.astimezone(ZoneInfo("UTC"))
            )

            current_date = current_date + timedelta(
                days=self._next_gap(),
            )

        return sessions

    def _local_session_datetime(
        self,
        date,
    ) -> datetime:
        if date.weekday() >= 5:
            hour = self.profile.weekend_preferred_hour

        else:
            hour = self.profile.preferred_hour

        jitter = self.rng.choice([-1, 0, 0, 1])

        hour = min(max(hour + jitter, 5), 23)

        minute = self.rng.randint(0, 59)

        return datetime(
            year=date.year,
            month=date.month,
            day=date.day,
            hour=hour,
            minute=minute,
            tzinfo=self.tz,
        )

    def _next_gap(self) -> int:
        if self.rng.random() < self.config.long_break_probability:
            return self.rng.randint(
                self.config.long_break_min_days,
                self.config.long_break_max_days,
            )

        roll = self.rng.random()

        cumulative = 0.0

        for gap, weight in self.config.session_gap_weights:
            cumulative += weight

            if roll <= cumulative:
                return gap

        return 2
