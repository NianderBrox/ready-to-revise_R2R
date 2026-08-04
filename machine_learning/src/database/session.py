from contextlib import contextmanager
from sqlalchemy.orm import Session
from database.db import SessionLocal
from collections.abc import Generator

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