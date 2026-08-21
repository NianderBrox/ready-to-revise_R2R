"""
Consistency tests for shared constants and dataset plumbing.
"""

from __future__ import annotations

import pandas as pd
import pytest

from src.data_generation.config import (
    CONFIDENCE_SCORE_MAP as GENERATION_MAP,
)
from src.feature_engineering.feature_manifest import (
    ALL_FEATURES,
    TARGET,
)
from src.training.dataset import load_dataset, split_xy


def test_confidence_maps_are_consistent():
    """
    The simulator writes LOW/MEDIUM/HIGH strings; the feature
    builder maps them to scores. Both modules must agree on the
    numeric encoding or training data silently shifts meaning.
    """

    builder_map = {"LOW": 0.25, "MEDIUM": 0.60, "HIGH": 0.90}

    assert GENERATION_MAP == builder_map

    assert set(GENERATION_MAP) == {"LOW", "MEDIUM", "HIGH"}


def test_split_xy_shapes():
    df = pd.DataFrame(
        {
            **{
                feature: [1.0, 2.0]
                for feature in ALL_FEATURES
            },
            TARGET: [True, False],
        }
    )

    X, y = split_xy(df)

    assert list(X.columns) == ALL_FEATURES

    assert len(X) == len(y) == 2

    assert set(y.unique()) == {True, False}


def test_load_dataset_missing_file_raises(tmp_path):
    with pytest.raises(FileNotFoundError):
        load_dataset(tmp_path / "nope.csv")
