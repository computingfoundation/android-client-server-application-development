--
-- UUID Generators
-- -------------------------------------------------------

DROP DATABASE IF EXISTS functions_test;
CREATE DATABASE functions_test TEMPLATE template_postgis;
\connect functions_test;

-- -------------------------------------------------------
--   Base10/Base64/Hex
-- -------------------------------------------------------
CREATE OR REPLACE FUNCTION base10_uuid()
RETURNS TRIGGER AS $$

DECLARE
  key TEXT;
  qry TEXT;
  found TEXT;
BEGIN
  qry := 'SELECT id FROM ' || TG_TABLE_SCHEMA || '.' 
    || quote_ident(TG_TABLE_NAME) || ' WHERE id=';

  LOOP
    -- Generate our string bytes and re-encode as a base10 string.
    key := encode(gen_random_bytes(6), 'hex');
    SELECT ('x' || lpad(hex, 12, '0'))::bit(48)::bigint AS int8_val
    FROM (VALUES (key)) AS t(hex) INTO key;

    -- Base64 encoding contains 2 URL unsafe characters by default.
    -- The URL-safe version has these replacements.
    key := replace(key, '/', '_'); -- url safe replacement
    key := replace(key, '+', '-'); -- url safe replacement

    -- Concat the generated key (safely quoted) with the generated query
    -- and run it.
    -- SELECT id FROM "test" WHERE id='blahblah' INTO found
    -- Now "found" will be the duplicated id or NULL.
    EXECUTE qry || quote_literal(key) INTO found;

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

-- -------------------------------------------------------
--   Using Timestamps and Shard IDs
-- -------------------------------------------------------
create schema shard_1;
create sequence shard_1.global_id_sequence;

CREATE OR REPLACE FUNCTION shard_1.id_generator(OUT result bigint) AS $$
DECLARE
    our_epoch bigint := 1320547192742;
    seq_id bigint;
    now_millis bigint;
    -- the id of this DB shard ("physical shard"), must be set for each
    -- schema shard you have - you could pass this as a parameter too
    shard_id int := 1;
BEGIN
    SELECT nextval('shard_1.global_id_sequence') % 1024 INTO seq_id;
    SELECT FLOOR(EXTRACT(EPOCH FROM clock_timestamp()) * 1000) INTO now_millis;

    -- This algorithm basically makes the last 10 bits the sequential id
    -- and the 13 bits before that the shard_id.
    result := (now_millis - our_epoch);
    -- raise notice 'Value-1: %', result;
    -- raise notice 'Value-1: %', result::bit(48);
    result := (result) << 23;
    -- raise notice 'Value-1: %', result;
    -- raise notice 'Value-2: %', result::bit(48);
    -- raise notice 'now_millis: %', now_millis;

    result := result | (shard_id << 10);
    raise notice 'Value-3: %', result::bit(48);
    -- raise notice 'shard_id: %', shard_id;

    result := result | (seq_id);
    raise notice 'Value-4: %', result::bit(48);
    raise notice 'seq_id: %', seq_id;
END;
$$ LANGUAGE PLPGSQL;

-- select shard_1.id_generator();

--    Output            |      Change or Type
--14589897 9 6300 81        left shift epoch 10 bits
--11952050 9 2756 685825    left shift epoch 23 bits
--11952068 4 3215 904769    not shifting shard_id
--10765432 1 0012 3456789   twitter id (timestamp, worker number, and a sequence number)
--Note on understanding these bit shifts:
--  Left shift 23 bits creates a binary number with 23 0s at the end.
--  By not shifting shard_id, its significance is lost.


-- -------------------------------------------------------
--   HEX With Left Shift
-- -------------------------------------------------------
CREATE OR REPLACE FUNCTION hex_left_shift_uuid(_data_type_id integer)
RETURNS text AS $$
DECLARE
  num_data_types int := 9;
  result_hex varchar;
  result_dec bigint;
  result_mod int;
  query text;
  found text;
BEGIN
  IF _data_type_id < 1 OR _data_type_id > num_data_types THEN
    RAISE EXCEPTION 'Invalid type ID --> %; must be '
    'between 1 and % inclusive', _data_type_id, num_data_types;
  END IF;

  -- query := 'SELECT id FROM ' || TG_TABLE_SCHEMA || '.' 
  --   || quote_ident(TG_TABLE_NAME) || ' WHERE id = ';
  query := 'SELECT id FROM table WHERE id = ''bac878''';

  LOOP

    result_hex := encode(gen_random_bytes(7), 'hex');
    SELECT ('x' || lpad(hex, 14, '0'))::bit(56)::bigint AS int8_val
      FROM (VALUES (result_hex)) AS t(hex) INTO result_dec;

    result_mod := result_dec % 9;
    IF result_mod < _data_type_id THEN
      result_dec := result_dec + (_data_type_id - result_mod);
    ELSIF result_mod > _data_type_id THEN
      result_dec := result_dec - (result_mod - _data_type_id);
    END IF;

    EXECUTE query INTO found;
    IF found IS NULL THEN
      raise notice 'Valueeee3: %', result_dec;
      EXIT;
    END IF;

  END LOOP;

  RETURN to_hex(result_dec);
END;
$$ LANGUAGE PLPGSQL;

select hex_left_shift_uuid(8);


--0.832737649325281


-- -------------------------------------------------------
--   MD5 Random Hex
-- -------------------------------------------------------
CREATE OR REPLACE FUNCTION md5_uuid(length integer)
RETURNS text AS $$
DECLARE
  -- loop_count: number of md5's needed to have the required char length
  loop_count integer := CEIL(length / 32.);
  output text := '';
  output_length integer;
  substr_start_indx integer;
  i INT4;
BEGIN
  FOR i IN 1..loop_count LOOP
    output := output || md5(random()::text);
  END LOOP;
  SELECT length(output) INTO output_length;
  substr_start_indx := output_length - length + 1;
  RETURN substring(output, substr_start_indx);
END;
$$ LANGUAGE PLPGSQL;

