-- Run ONCE on the production/dev database as a superuser or CREATEROLE user.
-- NOT part of Prisma migrations (role creation needs privileges the migration
-- user may not have). Safe to re-run.
--
--   psql "$DATABASE_URL" -f scripts/create_ml_readonly.sql
--
-- Then set machine_learning/.env DB_* credentials for this user (training only).
--
-- NOTE: the Prisma migration `create_ml_schema_views` has a conditional
-- GRANT block that activates automatically IF the ml_readonly role already
-- exists when the migration runs. If the role is created AFTER the migration,
-- those grants are skipped. This script therefore also applies them explicitly.

-- 1. Create the role (idempotent)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'ml_readonly') THEN
        CREATE ROLE "ml_readonly" LOGIN PASSWORD 'CHANGE_ME_BEFORE_USE';
    END IF;
END $$;

-- 2. Grant database connectivity
DO $$
DECLARE
    db_name TEXT := current_database();
BEGIN
    EXECUTE format('GRANT CONNECT ON DATABASE %I TO "ml_readonly"', db_name);
END $$;

-- 3. Grant read-only access to the ml schema (views + future tables)
GRANT USAGE ON SCHEMA "ml" TO "ml_readonly";
GRANT SELECT ON ALL TABLES IN SCHEMA "ml" TO "ml_readonly";
ALTER DEFAULT PRIVILEGES IN SCHEMA "ml" GRANT SELECT ON TABLES TO "ml_readonly";
