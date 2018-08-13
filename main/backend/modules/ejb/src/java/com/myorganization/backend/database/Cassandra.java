package com.organization.backend.database;

import com.datastax.driver.core.*;
import com.datastax.driver.core.exceptions.InvalidQueryException;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.datastax.driver.core.exceptions.QueryExecutionException;
import com.datastax.driver.core.exceptions.QueryValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Cassandra connection manager
 */
public class Cassandra {
    private static final Logger LOG = LoggerFactory.getLogger(Cassandra.class);

    public enum Node {
        NODE_1("127.0.0.1", 0),
        NODE_2("127.0.0.2", 0);
        public final String address;
        public final int port;
        Node(String address, int port) {
            this.address = address;
            this.port = port;
        }
    }

    private static Cluster sCluster;
    private static Session sSession;

    /**
     * Execute a statement directly to a Cassandra node.
     * Note: It is currently not in use and saved for all the exception catching in case it is needed in the future.
     * @return ResultSet of the executed query; null if failed
     */
    public static ResultSet execute(Node node, Statement statement) {
        ResultSet rs = null;

        try {
            rs = getSession(node).execute(statement);
        } catch (NoHostAvailableException e) {
            LOG.error("Failed to execute statement: No host in the cluster can be contacted: {}", e.getMessage());
        } catch (QueryExecutionException e) {
            LOG.error("Failed to execute statement: Cassandra cannot successfully execute the query with the " +
                    "specified consistency level: {}", e.getMessage());
        } catch (QueryValidationException e) {
            LOG.error("Failed to execute statement: Query not valid: {}", e.getMessage());
        } catch (IllegalStateException e) {
            LOG.error("Failed to execute statement: BoundStatement not ready: {}", e.getMessage());
        }
        return rs;
    }

    /**
     * Provide a Cassandra cluster session
     */
    public static Session getSession(Node node) {
        if (sSession == null || sSession.isClosed()) {
            sSession = connect(node, DbConstants.CQL.KEYSPACE);
            return sSession;
        }
        return sSession;
    }

    /**
     * Close Cassandra cluster
     */
    public static void close()
    {
        sCluster.close();
    }

    /**
     * Connect to Cassandra cluster
     * @return Session of connected cluster; null if connection failed.
     * @param node Cluster node IP address
     */
    public static Session connect(Node node, String keyspace) {
        sCluster = Cluster.builder().addContactPoint(node.address).build();
        //sCluster = Cluster.builder().addContactPoint(node.address).withPort(node.port).build();
        Metadata metadata = sCluster.getMetadata();

        LOG.debug("Connected to cluster: {}", metadata.getClusterName());
        for (Host host : metadata.getAllHosts()) {
            LOG.debug("Datacenter: {}; Host: {}; Rack: {}", host.getDatacenter(), host.getAddress(), host.getRack());
        }

        try {
            sSession = sCluster.connect(keyspace);
        } catch (InvalidQueryException e) {
            LOG.error("Cassandra failed to connect: {}", e.getMessage());
        }
        return sSession;
    }

    /**
     * A cache to hold prepared statements
     */
    public static class StatementCache {
        private static Map<String, PreparedStatement> sIdPreparedStatementMap = new HashMap<>();

        public static BoundStatement getStatement(String cql) {
            PreparedStatement prepStmt = sIdPreparedStatementMap.get(cql);

            if (prepStmt == null) {
                prepStmt = getSession(Node.NODE_1).prepare(cql);
                sIdPreparedStatementMap.put(cql, prepStmt);
            }
            return prepStmt.bind();
        }
    }

}
