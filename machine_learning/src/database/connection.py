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

search_path = DB_CONFIG.get("schema") or "ml"

engine = create_engine(
    url,
    future=True,
    pool_pre_ping=True,
    connect_args={"options": f"-csearch_path={search_path},public"},
)
