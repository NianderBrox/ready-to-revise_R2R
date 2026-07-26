from sqlalchemy import create_engine
from sqlalchemy.engine import URL

from src.database.config import DB_CONFIG

url = URL.create(
    drivername="postgresql+psycopg2",
    username=DB_CONFIG["user"],
    password=DB_CONFIG["password"],
    host=DB_CONFIG["host"],
    port=int(DB_CONFIG["port"]),
    database=DB_CONFIG["database"],
)

engine = create_engine(
    url,
    future=True,
    pool_pre_ping=True,
)