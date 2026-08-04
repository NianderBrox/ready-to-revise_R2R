from contextlib import contextmanager

from database.db import SessionLocal


@contextmanager
def session_scope():
    session = SessionLocal()

    try:
        yield session
        session.commit()

    except Exception:
        session.rollback()
        raise

    finally:
        session.close()