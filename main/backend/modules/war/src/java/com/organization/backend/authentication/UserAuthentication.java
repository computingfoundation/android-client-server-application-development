package com.organization.backend.authentication;

import com.organization.backend.database.DbConstants;
import com.organization.backend.database.SQLManager;
import com.organization.commons.base.DateUtils;
import com.organization.commons.configuration.RegulationConfigurations;
import com.organization.commons.internal.CommonsConstants;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;

public class UserAuthentication extends AuthenticationBase {
    private static final Logger LOG = LoggerFactory.getLogger(UserAuthentication.class);

    public static String createToken(JSONObject userJsonObje) {
        long userId = userJsonObje.getLong("id");
        String key = null;
        Date createdAt = null;

        try (Connection conn = SQLManager.getConnection(false)) {
            ResultSet reslSet = conn.createStatement().executeQuery("SELECT key,created_at FROM " +
                    DbConstants.SQL.AUTHENTICATION.TBL_USER + " WHERE user_id = " + userId);

            while (reslSet.next()) {
                key = reslSet.getString("key");
                createdAt = reslSet.getTimestamp("created_at", DateUtils.getUtcCalendar());
            }

            if (createdAt == null || System.currentTimeMillis() - createdAt.getTime() >
                    CommonsConstants.Time.TWENTY_FOUR_HOURS) {
                Statement stmt = conn.createStatement();

                stmt.execute("UPDATE " + DbConstants.SQL.AUTHENTICATION.TBL_USER + " SET key = '" +
                        Base64.encodeBase64String(s128BitKeyGenerator.generateKey().getEncoded()) +
                        "' WHERE user_id = " + userId + " RETURNING key,created_at");
                reslSet = stmt.getResultSet();

                while (reslSet.next()) {
                    key = reslSet.getString("key");
                    createdAt = reslSet.getTimestamp("created_at", DateUtils.getUtcCalendar());
                }
            }

            conn.commit();
        } catch (SQLException e) {
            LOG.error("Failed to create user authentication token: {}", e.toString());
            return null;
        }
        return key + "." + Base64.encodeBase64String(new JSONObject().put("createdAt",
                createdAt.getTime()).toString().getBytes());
    }

    /**
     * Determine if a token is valid.
     * @param userId ID of user providing the token
     */
    public static boolean isTokenValid(String token, long userId) {
        String[] tokenArry = token.split("\\.");

        if (tokenArry.length == 2 && tokenArry[0].length() > 0 && tokenArry[1].length() > 0) {
            try (Connection conn = SQLManager.getConnection(true)) {
                ResultSet reslSet = conn.createStatement().executeQuery("SELECT key,created_at FROM " +
                        DbConstants.SQL.AUTHENTICATION.TBL_USER + " WHERE user_id = " + userId);

                if (reslSet.next()) {
                    if (!tokenArry[0].equals(reslSet.getString("key"))) {
                        return false;
                    }

                    Date createdAt = reslSet.getTimestamp("created_at", DateUtils.getUtcCalendar());

                    if (!tokenArry[1].equals(Base64.encodeBase64String(new JSONObject().put("createdAt",
                            createdAt.getTime()).toString().getBytes()))) {
                        return false;
                    }

                    if (System.currentTimeMillis() - createdAt.getTime() >
                            RegulationConfigurations.Internal.Authentication.userTokenLifetime()) {
                        return false;
                    }
                }
            } catch (SQLException e) {
                LOG.error("Failed to execute query on table \"{}\": {}", DbConstants.SQL.AUTHENTICATION.TBL_USER,
                        e.toString());
                return false;
            }
            return true;
        }
        return false;
    }

}