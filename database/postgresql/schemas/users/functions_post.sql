--
-- Users schema post creation functions (functions that rely on the schema)
-- -------------------------------------------------------

-- -------------------------------------------------------
--   User Inqueries
-- -------------------------------------------------------

CREATE OR REPLACE FUNCTION log_in_user_by_name(_name text, _passw text)
RETURNS SETOF users.user AS $$
BEGIN
  RETURN QUERY SELECT * FROM users.user WHERE name = _name
  AND passw_hash = crypt(_passw, users.user.passw_hash);
END;
$$ LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION log_in_user_by_phone_number(_phone_number text, _passw text)
RETURNS SETOF users.user AS $$
DECLARE
  forLoopStartIndx integer := 2;
BEGIN
  _phone_number = ltrim(_phone_number, '0');

  IF length(_phone_number) < 5 THEN
    RETURN;
  END IF;

  IF length(_phone_number) > 11 THEN
    forLoopStartIndx := length(_phone_number) - 11;
  ELSE
    RETURN QUERY SELECT * FROM users.user
    WHERE phone_number = _phone_number AND passw_hash = crypt(_passw,
        users.user.passw_hash);

    IF FOUND THEN
      RETURN;
    END IF;
  END IF;

  FOR i IN 1..3 LOOP
    RETURN QUERY SELECT * FROM users.user
    WHERE country_code = substr(_phone_number, 1, i)::smallint AND
        phone_number = substr(_phone_number, i + 1) AND passw_hash =
        crypt(_passw, users.user.passw_hash);

    IF FOUND THEN
      RETURN;
    END IF;
  END LOOP;
END;
$$ LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION log_in_user_by_email(_email text, _passw text)
RETURNS SETOF users.user AS $$
BEGIN
  RETURN QUERY SELECT * FROM users.user WHERE email = _email
  AND passw_hash = crypt(_passw, users.user.passw_hash);
END;
$$ LANGUAGE PLPGSQL;

