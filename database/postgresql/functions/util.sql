--
-- Utility functions
-- -------------------------------------------------------

-- -------------------------------------------------------
--   Date/Time Util
-- -------------------------------------------------------
CREATE FUNCTION now_utc() RETURNS timestamp AS $$
  SELECT now() at time zone 'utc';
$$ LANGUAGE SQL;

-- Get current epoch time (automatically uses UTC time zone)
CREATE FUNCTION now_epoch() RETURNS double precision AS $$
  SELECT extract(epoch from now());
$$ LANGUAGE SQL;


-- -------------------------------------------------------
--   Generators
-- -------------------------------------------------------
CREATE OR REPLACE FUNCTION generate_url_safe_base64(num_bytes int)
RETURNS text AS $$
DECLARE
  base64_str text := '';
BEGIN
  base64_str := encode(gen_random_bytes(num_bytes), 'base64');
  -- Remove 2 URL unsafe Base64 characters
  base64_str := replace(base64_str, '/', '_');
  base64_str := replace(base64_str, '+', '-');
  RETURN base64_str;
END;
$$ LANGUAGE PLPGSQL;
