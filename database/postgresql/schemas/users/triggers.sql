--
-- User schema triggers
-- -------------------------------------------------------

-- 
-- Process post insert user operations.
-- 
CREATE OR REPLACE FUNCTION process_post_insert_user_operations()
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO users.user_log (user_id) VALUES (NEW.id);

  INSERT INTO users.user_activity (user_id) VALUES (NEW.id);

  INSERT INTO authentication.user (user_id, key)
      VALUES (NEW.id, encode(gen_random_bytes(16), 'base64'));
  RETURN NEW;
END;
$$ LANGUAGE PLPGSQL;

-- 
-- Process post delete user operations.
-- 
CREATE OR REPLACE FUNCTION process_post_delete_user_operations()
RETURNS TRIGGER AS $$
BEGIN
  DELETE FROM users.user_log WHERE user_id = OLD.id;

  DELETE FROM users.user_activity WHERE user_id = OLD.id;

  DELETE FROM authentication.user WHERE user_id = OLD.id;
  RETURN OLD;
END;
$$ LANGUAGE PLPGSQL;

