from __future__ import annotations

from dataclasses import dataclass
from random import Random

from src.simulator.behavioural_model import BehavioralModel
from src.simulator.review_context import ReviewContext
from src.simulator.review_outcome import ReviewOutcome


@dataclass(frozen=True, slots=True)
class SimulationResult:
    outcomes: list[ReviewOutcome]


class Simulator:

    def __init__(self, seed: int):
        self.rng = Random(seed)
        self.behavioral_model = BehavioralModel(self.rng)

    def simulate_reviews(
        self,
        contexts: list[ReviewContext],
    ) -> SimulationResult:

        outcomes = []

        for context in contexts:
            outcome = self.behavioral_model.simulate(
                context
            )

            outcomes.append(outcome)

        return SimulationResult(
            outcomes=outcomes
        )