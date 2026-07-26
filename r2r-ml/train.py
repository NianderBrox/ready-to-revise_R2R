from src.feature_engineering.loader import load_table

# Tests if engine is working or not
# from src.database.connection import engine

# print(engine)

users = load_table("users")

print(users.head())