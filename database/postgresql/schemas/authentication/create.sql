--
-- Authentication schema
-- -------------------------------------------------------
\ir functions.sql

CREATE SCHEMA authentication;

CREATE TABLE authentication.session (
  id serial PRIMARY KEY,
  key varchar DEFAULT NULL,
  created_at timestamp DEFAULT now_utc(),
  hash_salt varchar DEFAULT NULL
);
-- TODO: Create index on column 'key'

CREATE TABLE authentication.user (
  user_id bigint PRIMARY KEY REFERENCES users.user (id),
  key varchar DEFAULT NULL,
  created_at timestamp DEFAULT now_utc()
);

-- --- --- --- --- --- --- --- --- --- --- --- --- --- ---

