--
-- User schema triggers
-- -------------------------------------------------------

-- Generates a decimal id that is unique to one table across one or more schemas.
-- Arg 1: Table
-- Arg 2: Array of schemas
-- Arg 3: Number of 4-bit bytes to use
CREATE OR REPLACE FUNCTION generate_decimal_uuid()
RETURNS TRIGGER AS $$
DECLARE
  arg_schemas text[] := TG_ARGV[0];
  arg_table text := TG_ARGV[1];
  arg_num_bytes int := TG_ARGV[2];
  key text;
  qry text;
  found text;
  i text;
BEGIN
  IF NEW.id IS NOT NULL THEN
    RETURN NEW;
  END IF;

  FOREACH i IN ARRAY arg_schemas
  LOOP
    qry := format('%sSELECT id FROM %I.%I WHERE id = {id} UNION ALL ',
      qry, i, arg_table, '%s');
  END LOOP;
  SELECT SUBSTR(qry, 1, LENGTH(qry) - 10) INTO qry;

  LOOP
    -- Generate string bytes and re-encode to a decimal (base10).
    -- + lpad(hex, x, ... where x is the total length of the string created
    --   with gen_random_bytes(6)
    -- + ::bit(x) ... where x is the number of bytes from gen_random_bytes * 8
    -- + Note: The max length string will always be x*2 where x is
    --   the argument for gen_random_bytes
    -- Source: <http://stackoverflow.com/a/8335376>
    key := encode(gen_random_bytes(arg_num_bytes), 'hex');

    -- Note: This code is duplicated for each number of byte because casting to
    --       bit(x) requires a constant (no variables).
    -- TODO: Find a way to split this up into less duplicated statements.
    -- TODO: Using more than 6 bytes causes negative numbers but should be added

    IF arg_num_bytes < 2 THEN
      RAISE EXCEPTION 'Invalid number of 4-bit bytes specified for
          generate_decimal_uuid --> %', arg_num_bytes
          USING HINT = 'Minimum 2 required';
    ELSEIF arg_num_bytes = 2 THEN
      SELECT ('x' || lpad(hex, arg_num_bytes*2, '0'))::bit(8)::int
          AS int_val FROM (VALUES (key)) AS t(hex) INTO key;
    ELSEIF arg_num_bytes = 3 THEN
      SELECT ('x' || lpad(hex, arg_num_bytes*2, '0'))::bit(12)::int
          AS int_val FROM (VALUES (key)) AS t(hex) INTO key;
    ELSEIF arg_num_bytes = 4 THEN
      SELECT ('x' || lpad(hex, arg_num_bytes*2, '0'))::bit(16)::int
          AS int_val FROM (VALUES (key)) AS t(hex) INTO key;
    ELSEIF arg_num_bytes = 5 THEN
      SELECT ('x' || lpad(hex, arg_num_bytes*2, '0'))::bit(20)::int
          AS int_val FROM (VALUES (key)) AS t(hex) INTO key;
    ELSEIF arg_num_bytes = 6 THEN
      SELECT ('x' || lpad(hex, arg_num_bytes*2, '0'))::bit(24)::int
          AS int_val FROM (VALUES (key)) AS t(hex) INTO key;
    ELSEIF arg_num_bytes = 7 THEN
      SELECT ('x' || lpad(hex, arg_num_bytes*2, '0'))::bit(28)::int
          AS int_val FROM (VALUES (key)) AS t(hex) INTO key;
    ELSEIF arg_num_bytes = 8 THEN
      SELECT ('x' || lpad(hex, arg_num_bytes*2, '0'))::bit(32)::bigint
          AS int_val FROM (VALUES (key)) AS t(hex) INTO key;
    ELSEIF arg_num_bytes = 9 THEN
      SELECT ('x' || lpad(hex, arg_num_bytes*2, '0'))::bit(36)::bigint
          AS int_val FROM (VALUES (key)) AS t(hex) INTO key;
    ELSEIF arg_num_bytes = 10 THEN
      SELECT ('x' || lpad(hex, arg_num_bytes*2, '0'))::bit(40)::bigint
          AS int8_val FROM (VALUES (key)) AS t(hex) INTO key;
    ELSEIF arg_num_bytes = 11 THEN
      SELECT ('x' || lpad(hex, arg_num_bytes*2, '0'))::bit(44)::bigint
          AS int8_val FROM (VALUES (key)) AS t(hex) INTO key;
    ELSEIF arg_num_bytes = 12 THEN
      SELECT ('x' || lpad(hex, arg_num_bytes*2, '0'))::bit(48)::bigint
          AS int8_val FROM (VALUES (key)) AS t(hex) INTO key;
    ELSE
      RAISE EXCEPTION 'Too many 4-bit bytes specified for
          generate_decimal_uuid --> %', arg_num_bytes
          USING HINT = 'Maximum 12 allowed';
    END IF;

    -- Base64 encoding contains 2 URL unsafe characters by default.
    -- The URL-safe version has these replacements.
    key := replace(key, '/', '_'); -- url safe replacement
    key := replace(key, '+', '-'); -- url safe replacement

    -- Concat the generated key (safely quoted) with the generated query
    -- and run it.
    -- SELECT id FROM "test" WHERE id='blahblah' INTO found
    -- Now "found" will be the duplicated id or NULL.
    EXECUTE replace(qry, '{id}', key) INTO found;

    -- Check to see if found is NULL and leave loop if no collision was found.
    IF found IS NULL THEN
      EXIT;
    END IF;
    
  END LOOP;

  -- NEW and OLD are available in TRIGGER PROCEDURES.
  -- NEW is the mutated row that will actually be INSERTed.
  -- We're replacing id, regardless of what it was before
  -- with our key variable.
  NEW.id = key;

  -- The RECORD returned here is what will actually be INSERTed,
  -- or what the next trigger will get if there is one.
  RETURN NEW;
END;
$$ LANGUAGE PLPGSQL;

-- --- --- --- --- --- --- --- --- --- --- --- --- --- ---

CREATE OR REPLACE FUNCTION process_new_user()
RETURNS TRIGGER AS $$
BEGIN
  INSERT INTO users.user_statistics (user_id) VALUES (NEW.id);
  RETURN NEW;
END;
$$ LANGUAGE PLPGSQL;
