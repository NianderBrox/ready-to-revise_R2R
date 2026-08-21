from collections.abc import Generator
from contextlib import contextmanager

from sqlalchemy.orm import Session

from src.database.db import SessionLocal


#better duplicated of context.py since type mentioned
@contextmanager
def session_scope() -> Generator[Session, None, None]:
    session = SessionLocal()

    try:
        yield session
        session.commit()

    except Exception:
        session.rollback()
        raise

    finally:
        session.close()