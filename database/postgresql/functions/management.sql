--
-- Management functions
-- -------------------------------------------------------

-- Truncate all tables for one or all schemas for the specified user.
-- Argument 'schema' must either be a valid schema name or "all" for all schemas.
CREATE OR REPLACE FUNCTION truncate_tables(username VARCHAR, schema VARCHAR)
RETURNS void AS $$
DECLARE
  statementsSingleSchema CURSOR FOR
    SELECT schemaname,tablename FROM pg_tables
    WHERE tableowner = username AND schemaname = schema;
  statementsAllSchemas CURSOR FOR
    SELECT schemaname,tablename FROM pg_tables
    WHERE tableowner = username;
BEGIN
  IF schema = 'all' THEN
    FOR stmt IN statementsAllSchemas LOOP
      EXECUTE 'TRUNCATE TABLE ' || stmt.schemaname || '.' ||
        quote_ident(stmt.tablename) || ' CASCADE;';
    END LOOP;
  ELSE
    FOR stmt IN statementsSingleSchema LOOP
      EXECUTE 'TRUNCATE TABLE ' || stmt.schemaname || '.' ||
        quote_ident(stmt.tablename) || ' CASCADE;';
    END LOOP;
  END IF;
END;
$$ LANGUAGE plpgsql;
