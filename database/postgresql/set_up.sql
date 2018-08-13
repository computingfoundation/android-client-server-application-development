--
--   Organization PostgreSQL set up
-- -------------------------------------------------------

DROP DATABASE IF EXISTS organization_main;
CREATE DATABASE organization_main TEMPLATE template_postgis;

\connect organization_main;

-- -----------------------------------------------
--   Global Functions and Procedures
-- -----------------------------------------------

\ir functions/util.sql;
\ir functions/management.sql;
\ir triggers/uuid_generators.sql


-- -----------------------------------------------
--   Postgres Users Set up
-- -----------------------------------------------

-- Note: Do not remove the "public" schema from the search paths.

ALTER ROLE fiadmin SET search_path TO
  public;

ALTER ROLE fipublic SET search_path TO
  public;




-- 
-- ***********************************************************
--   Development Notes
-- ***********************************************************
-- -The "public" schema is required in the search path to use the postgis extension.
-- -Functions only needed to be loaded once, in the main schema, to be used by
--  all other schemas.
-- 
-- 
-- 
