import os
import pandas as pd

from src.feature_engineering.loader import load_table
from src.feature_engineering.joiner import join_tables
from src.feature_engineering.feature_builder import build_features


OUTPUT_PATH = "data/features/training_dataset.csv"


def create_training_dataset():

    print("Loading database tables...")

    questions = load_table("questions")

    sessions = load_table("study_sessions")

    reviews = load_table("question_reviews")

    attempts = load_table("question_attempts")


    print("Joining tables...")

    df = join_tables(
        reviews,
        questions,
        sessions,
        attempts,
    )


    print("Building features...")

    dataset = build_features(df)


    print("Saving dataset...")


    os.makedirs(
        os.path.dirname(OUTPUT_PATH),
        exist_ok=True
    )


    dataset.to_csv(
        OUTPUT_PATH,
        index=False
    )


    print(
        f"Dataset saved: {OUTPUT_PATH}"
    )

    print(
        f"Rows: {len(dataset)}"
    )

    print(
        f"Columns: {len(dataset.columns)}"
    )


if __name__ == "__main__":
    create_training_dataset()