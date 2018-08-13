--
-- Initialization
-- -------------------------------------------------------

BEGIN;

DELETE FROM authentication.session;

DO $do$
DECLARE
  num_keys integer := 1000;
  key_base64 text := '';
  hash_salt_base64 text := '';
BEGIN
  FOR i IN 1..1000 LOOP
    key_base64 := encode(gen_random_bytes(16), 'base64');
    hash_salt_base64 := encode(gen_random_bytes(4), 'base64');
    
    INSERT INTO authentication.session (key, hash_salt) 
      VALUES (key_base64, hash_salt_base64);
  END LOOP;
END $do$;


COMMIT;

