--
-- Users schema
-- -------------------------------------------------------
\ir functions_initial.sql

CREATE SCHEMA users;

CREATE TABLE users.user (
  id bigint PRIMARY KEY,
  name varchar NOT NULL UNIQUE,
  passw_hash varchar DEFAULT NULL,
  email varchar DEFAULT NULL UNIQUE,
  country_code smallint DEFAULT NULL,
  phone_number varchar DEFAULT NULL UNIQUE,
  first_name varchar DEFAULT NULL,
  last_name varchar DEFAULT NULL,
  created_at timestamp NOT NULL,
  UNIQUE (country_code, phone_number)
);
-- Note: column passw_hash cannot use the NOT NULL constraint as it must be set
-- using UPDATE rather than INSERT.

CREATE TABLE users.user_log (
  user_id bigint PRIMARY KEY REFERENCES users.user (id),
  logged_in_at timestamp DEFAULT NULL,
  logged_out_at timestamp DEFAULT NULL,
  times_logged_in int DEFAULT 0,
  changed_name_at timestamp DEFAULT NULL
);

CREATE TYPE external_login_service AS ENUM ('twitter', 'facebook', 'google');

CREATE TABLE users.application_user_id_external_login_service_user_id_pair (
  application_user_id bigint NOT NULL UNIQUE REFERENCES users.user (id),
  external_login_service_user_id varchar PRIMARY KEY,
  external_login_service external_login_service NOT NULL,
  UNIQUE (external_login_service_user_id, external_login_service)
);

-- --- --- --- --- --- --- --- --- --- --- --- --- --- ---

CREATE INDEX idx_log_user_id ON users.user_log (user_id);

CREATE INDEX idx_activity_user_id ON users.user_activity (user_id);

CREATE INDEX idx_pair_application_user_id ON
  users.application_user_id_external_login_service_user_id_pair
  (application_user_id);


-- --- --- --- --- --- --- --- --- --- --- --- --- --- ---

CREATE TRIGGER generate_id BEFORE INSERT ON users.user
  FOR EACH ROW EXECUTE PROCEDURE generate_decimal_uuid('{"users"}', 'user', 12);

\ir triggers.sql

CREATE TRIGGER insert_user AFTER INSERT ON users.user
  FOR EACH ROW EXECUTE PROCEDURE process_post_insert_user_operations();

CREATE TRIGGER delete_user BEFORE DELETE ON users.user
  FOR EACH ROW EXECUTE PROCEDURE process_post_delete_user_operations();


-- --- --- --- --- --- --- --- --- --- --- --- --- --- ---

GRANT ALL PRIVILEGES ON TABLE users.user TO fipublic;


-- --- --- --- --- --- --- --- --- --- --- --- --- --- ---

\ir functions_post.sql

