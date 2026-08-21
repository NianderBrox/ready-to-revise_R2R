
from src.feature_engineering.feature_builder import build_features
from src.feature_engineering.joiner import join_tables
from src.feature_engineering.loader import load_table
from src.utils.config import FEATURE_DATA_DIR

OUTPUT_PATH = FEATURE_DATA_DIR / "training_dataset.csv"


def create_training_dataset():

    print("Loading database tables...")

    questions = load_table("questions")

    sessions = load_table("study_sessions")

    reviews = load_table("question_reviews")

    attempts = load_table("question_attempts")

    schedules = load_table("review_schedules")
    #use simulator's confidence_score
    print("Joining tables...")

    df = join_tables(
        reviews,
        questions,
        sessions,
        attempts,
        schedules=schedules,
    )

    print("Building features...")

    dataset = build_features(df)

    print("Saving dataset...")

    FEATURE_DATA_DIR.mkdir(parents=True, exist_ok=True)

    dataset.to_csv(OUTPUT_PATH, index=False)

    print(f"Dataset saved: {OUTPUT_PATH}")

    print(f"Rows: {len(dataset)}")

    print(f"Columns: {len(dataset.columns)}")


if __name__ == "__main__":
    create_training_dataset()
