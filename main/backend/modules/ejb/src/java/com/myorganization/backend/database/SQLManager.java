package com.organization.backend.database;

import com.organization.commons.internal.CommonsConstants;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * SQL manager
 */
public class SQLManager {
    private static final Logger LOG = LoggerFactory.getLogger(SQLManager.class);

    private static HikariDataSource sDataSource = null;
    private static final int MAX_CONNECTION_POOL_SIZE = 5;

    static {
        boolean initializationError = false;
        LOG.info("Initializing {}", SQLManager.class.getSimpleName());
        String jdbcDriverClass = "org.postgresql.Driver";

        LOG.debug("Loading JDBC driver {}", jdbcDriverClass);
        try {
            Class.forName(jdbcDriverClass);
        } catch (ClassNotFoundException e) {
            LOG.error("Failed to load JDBC driver {}", e.toString());
            initializationError = true;
        }

        if (!initializationError) {
            LOG.debug("Creating SQL connection pool");
            try {
                HikariConfig config = new HikariConfig();
                config.setJdbcUrl(getUrl());
                config.setMaximumPoolSize(MAX_CONNECTION_POOL_SIZE);
                config.addDataSourceProperty("cachePrepStmts", "true");
                config.addDataSourceProperty("prepStmtCacheSize", "250");
                config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                config.addDataSourceProperty("leakDetectionThreshold", "5");
                // setConnectionTestQuery needed to fix the following bug:
                // "Failed to initialize pool: Method org.postgresql.jdbc4.Jdbc4Connection.isValid(int) is not yet implemented"
                //config.setConnectionTestQuery("SELECT * FROM " + DbConstants.SQL.USERS.TBL_USER);
                sDataSource = new HikariDataSource(config);
            } catch (Exception e) {
                LOG.error("Failed to create SQL connection pool: {}", e.toString());
                initializationError = true;
            }
        }

        if (!initializationError) {
            LOG.info("SQLManager initialized");
        } else {
            LOG.error("SQLManager failed to initialize");
        }
    }

    /**
     * Create and return a connection to the PostgreSQL database.
     * @param autoCommit Whether or not to commit each individual statement in its own transaction
     * @return SQL Connection
     * @throws SQLException if connection fails
     */
    public static Connection getConnection(boolean autoCommit) throws SQLException {
        Connection connection = null;
        try {
            connection = sDataSource.getConnection();
            connection.setAutoCommit(autoCommit);
        } catch (SQLException e) {
            LOG.error("Failed to get SQL connection: {}", e.toString());
            throw e;
        }

        return connection;
    }

    /**
     * Get the PostgreSQL connection URL
     */
    private static String getUrl() {
        String URL;
        String configParameters = "&useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&failOverReadOnly=false" +
                "&maxReconnects=5";

        if (CommonsConstants.SERVER_URL == CommonsConstants.Server.PRODUCTION) {
            URL = "jdbc:postgresql://127.0.0.1:5432/" + DbConstants.SQL.ORG_MAIN_DATABASE +
                    "?user=" + DbConstants.SQL.ADMIN_DATABASE_USER + configParameters;
        } else {
            URL = "jdbc:postgresql://127.0.0.1:5432/" + DbConstants.SQL.ORG_MAIN_DATABASE +
                    "?user=" + DbConstants.SQL.ADMIN_DATABASE_USER + configParameters;
        }

        return URL;
    }

    public static void closeQuietly(ResultSet resultSet) {
        try { resultSet.close(); } catch (Exception ignored) { }
    }

    public static void closeQuietly(Statement statement) {
        try { statement.close(); } catch (Exception ignored) { }
    }

    public static void closeQuietly(Connection connection) {
        try { connection.close(); } catch (Exception ignored) { }
    }

}




// In the future, attempt to further optimize PostgreSQL with these steps:
// http://www.precisejava.com/javaperf/j2ee/JDBC.htm#JDBC103
