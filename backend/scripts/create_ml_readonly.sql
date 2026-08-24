-- Run ONCE on the production/dev database as a superuser or CREATEROLE user.
-- NOT part of Prisma migrations (role creation needs privileges the migration
-- user may not have). Safe to re-run.
--
--   psql "$DATABASE_URL" -f scripts/create_ml_readonly.sql
--
-- Then set machine_learning/.env DB_* credentials for this user (training only).

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'ml_readonly') THEN
        CREATE ROLE "ml_readonly" LOGIN PASSWORD 'CHANGE_ME_BEFORE_USE';
    END IF;
END $$;

DO $$
DECLARE
    db_name TEXT := current_database();
BEGIN
    EXECUTE format('GRANT CONNECT ON DATABASE %I TO "ml_readonly"', db_name);
END $$;
