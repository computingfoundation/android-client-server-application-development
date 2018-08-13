--
-- Users schema functions
-- -------------------------------------------------------

-- -------------------------------------------------------
--   User Management
-- -------------------------------------------------------

CREATE OR REPLACE FUNCTION generate_password_hash(password varchar)
RETURNS text AS $$
BEGIN
  RETURN crypt(password, gen_salt('bf', 6));
END;
$$ LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION update_user_password(user_id integer, password varchar)
RETURNS void AS $$
BEGIN
  UPDATE users.user SET passw_hash = crypt(password, gen_salt('bf', 6));
END;
$$ LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION does_user_name_exist(_user_name varchar)
RETURNS boolean AS $$
BEGIN
  PERFORM id FROM users.user WHERE name = _user_name;
  IF FOUND THEN
    RETURN TRUE;
  ELSE
    RETURN FALSE;
  END IF;
END;
$$ LANGUAGE PLPGSQL;

-- -------------------------------------------------------
--   User Inqueries
-- -------------------------------------------------------

CREATE OR REPLACE FUNCTION check_user_login(_user_name text, _passw text)
RETURNS boolean AS $$
BEGIN
  PERFORM * FROM users.user WHERE name = _user_name
  AND passw_hash = crypt(_passw, users.user.passw_hash);
  IF FOUND THEN
    RETURN TRUE;
  ELSE
    RETURN FALSE;
  END IF;
END;
$$ LANGUAGE PLPGSQL;

