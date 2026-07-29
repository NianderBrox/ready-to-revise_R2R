from src.feature_engineering.loader import load_table
from src.feature_engineering.joiner import join_tables
from src.feature_engineering.feature_builder import build_features


questions = load_table("questions")

sessions = load_table("study_sessions")

reviews = load_table("question_reviews")

attempts = load_table("question_attempts")


df = join_tables(
    reviews,
    questions,
    sessions,
    attempts,
)


features = build_features(df)


print(features.head())

print(features.columns)
