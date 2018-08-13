--
-- Authentication schema functions
-- -------------------------------------------------------

-- Updates every row in the the authentication.session table by generating
-- a new key and hash_salt for each.
CREATE OR REPLACE FUNCTION update_session_keys()
RETURNS void AS $$
DECLARE
  num_keys integer := 1000;
  key_base64 text := '';
  hash_salt_base64 text := '';
BEGIN
  FOR i IN 1..num_keys LOOP
    key_base64 := encode(gen_random_bytes(16), 'base64');
    hash_salt_base64 := encode(gen_random_bytes(4), 'base64');

    UPDATE authentication.session SET (key, hash_salt) = (key_base64,
        hash_salt_base64) WHERE id = i;
  END LOOP;
END;
$$ LANGUAGE PLPGSQL;

