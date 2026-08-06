from __future__ import annotations

from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class MemoryResult:
    
    # Output of the memory model
    # 0 =< memory_score <= 1

    memory_score: float