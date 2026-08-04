from __future__ import annotations

from dataclasses import dataclass

from simulator.enums import ConfidenceLevel


@dataclass(frozen=True, slots=True)
class ConfidenceResult:
    confidence_score: float
    confidence: ConfidenceLevel