-- 
-- Organization Cassandra Schema
-- 
-- Changes required for production:
--   1. List of data centers for NetworkTopologyStrategy will
--     have to be changed as described here:
--     <https://docs.datastax.com/en/cql/3.1/cql/cql_reference/create_keyspace_r.html>
-- 
-- Notes:
--   1. Datetimes are created using bigints representing the number of milliseconds since epoch.

DROP KEYSPACE IF EXISTS organization_main;
CREATE KEYSPACE organization_main
  WITH REPLICATION = { 'class' : 'NetworkTopologyStrategy', 'datacenter1' : 1 };

USE organization_main;

CREATE TABLE user_votes (
  user_id bigint,
  section_id varchar,
  voted_at timestamp,
  PRIMARY KEY(user_id, section_id)
);

CREATE TABLE votes (
  section_id varchar PRIMARY KEY,
  up counter,
  down counter
);

