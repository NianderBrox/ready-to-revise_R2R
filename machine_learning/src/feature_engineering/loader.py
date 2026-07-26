import pandas as pd

from src.database.connection import engine


def load_table(table_name: str) -> pd.DataFrame:
    """
    Load an entire PostgreSQL table into a DataFrame.
    """
    query = f"SELECT * FROM {table_name};"

    return pd.read_sql(query, engine)