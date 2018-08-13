package com.organization.backend.rest;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.organization.backend.authentication.EndpointAuthenticationUtils;
import com.organization.backend.authentication.UserAuthentication;
import com.organization.backend.database.Cassandra;
import com.organization.backend.database.DbConstants;
import com.organization.backend.database.SQLManager;
import com.organization.commons.base.DateUtils;
import com.organization.commons.base.DevLog;
import com.organization.commons.base.JSONUtils;
import com.organization.commons.configuration.RegulationConfigurations;
import com.organization.commons.model.LoginService;
import com.organization.commons.model.User;
import com.organization.commons.rest.InputResponse;
import com.organization.commons.rest.UpdateRequest;
import com.organization.commons.rest.ValueRequest;
import com.organization.commons.validation.UserValidator;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.organization.backend.rest.ClientFatalError.TYPE.*;
import static com.organization.backend.rest.ServerFatalError.TYPE.DATABASE_TRANSACTION;
import static com.organization.commons.rest.ServerResponseGeneralError.TYPE.*;

@Path("/users")
public class UsersEndpoint {

    /**
     * Log in a user.
     */
    @POST @Path("/log_in/{phoneNumberEmailAddressOrName}")
    @Consumes("application/x-www-form-urlencoded") @Produces("application/json")
    public Response logInRequest(@Context HttpServletRequest reqs, @Context HttpHeaders hh,
                                 @PathParam("phoneNumberEmailAddressOrName") String phoneNumberEmailAddressOrName,
                                 @FormParam("password") String password) {
        if (!EndpointAuthenticationUtils.isSessionTokenValid(reqs, hh)) {
            return EndpointErrorUtils.processClientFatalError(reqs, UNAUTHORIZED, "Invalid session token")
                    .toResponse();
        } else if (password == null || password.equals("")) {
            return EndpointErrorUtils.processClientFatalError(reqs, INVALID_PARAMETER, "User log in: Parameter" +
                    " \"password\" null or empty.").toResponse();
        }

        try (Connection conn = SQLManager.getConnection(false)) {
            JSONObject userJsonObje = executeLogInQuery(phoneNumberEmailAddressOrName, password, conn);

            if (userJsonObje == null) {
                return new Result(INVALID_CREDENTIALS, "Invalid credentials.").toResponse();
            }

            Result res = finalizeLogInRequest(userJsonObje, conn);
            conn.commit();
            return res.toResponse();
        } catch (SQLException e) {
            return EndpointErrorUtils.processServerFatalError(DATABASE_TRANSACTION, "Failed to execute query in" +
                    " logInRequest(): {}", e.toString()).toResponse();
        }
    }

    /**
     * Execute a log in query.
     * @param phoneNumberEmailAddressOrName User phone number, email address or name
     * @param passw Organization user account password
     * @param conn SQL connection to use; null to create one
     * @return Row JSON of user account
     * @throws SQLException If the query statement fails
     */
    private static JSONObject executeLogInQuery(String phoneNumberEmailAddressOrName, String passw, Connection conn)
            throws SQLException {
        String sqlLogInMethod;

        if (phoneNumberEmailAddressOrName.matches("^[0-9\\s()-+]+$")) {
            sqlLogInMethod = "phone_number";
            phoneNumberEmailAddressOrName = phoneNumberEmailAddressOrName.replaceAll("[^\\d]", "");
        } else if (phoneNumberEmailAddressOrName.contains("@")) {
            sqlLogInMethod = "email";
        } else {
            sqlLogInMethod = "name";
        }

        Connection finlConn = conn;
        ResultSet rs = null;
        JSONObject rowJsonObj = null;

        if (conn == null) {
            finlConn = SQLManager.getConnection(true);
        }

        try (PreparedStatement ps = finlConn.prepareStatement("SELECT row_to_json(sq) FROM (SELECT * FROM" +
                " log_in_user_by_" + sqlLogInMethod + "(?, ?)) AS sq")) {
            ps.setString(1, phoneNumberEmailAddressOrName);
            ps.setString(2, passw);
            rs = ps.executeQuery(); // TODO: Find out how to limit to one

            if (rs.next()) {
                rowJsonObj = new JSONObject(rs.getString("row_to_json"));
            }
        } finally {
            SQLManager.closeQuietly(rs);
            if (conn == null) {
                SQLManager.closeQuietly(finlConn);
            }
        }

        if (rowJsonObj == null) {
            return null;
        } else {
            return finalizeLogInQueryJson(rowJsonObj);
        }
    }

    /**
     * Execute a log in query by user ID.
     * @param userId Organization user account ID
     * @param userPassword Organization user account password; may be null
     * @param conn SQL connection to use; null to create one
     * @throws SQLException If the query statement fails
     */
    private static JSONObject executeLogInQuery(long userId, String userPassword, Connection conn) throws SQLException {
        Connection finalConn = conn;
        ResultSet reslSet = null;
        JSONObject rowJsonObje = null;

        if (conn == null) {
            finalConn = SQLManager.getConnection(true);
        }

        String stmt = "SELECT row_to_json(sq) FROM (SELECT * FROM " + DbConstants.SQL.USERS.TBL_USER +
                " WHERE id = ?";

        // TODO: After removing external login service, make password required.
        if (userPassword != null) {
            stmt += " AND passw_hash = crypt(?, " + DbConstants.SQL.USERS.TBL_USER + ".passw_hash)";
        }
        stmt += ") sq;";

        try (PreparedStatement ps = finalConn.prepareStatement(stmt)) {
            ps.setLong(1, userId);
            if (userPassword != null) {
                ps.setString(2, userPassword);
            }
            reslSet = ps.executeQuery();

            if (reslSet.next()) {
                rowJsonObje = new JSONObject(reslSet.getString("row_to_json"));
            }
        } finally {
            SQLManager.closeQuietly(reslSet);
            if (conn == null) {
                SQLManager.closeQuietly(finalConn);
            }
        }

        if (rowJsonObje == null) {
            return null;
        } else {
            return finalizeLogInQueryJson(rowJsonObje);
        }
    }

    /**
     * Finalize the JSON of a log in query.
     * @param userJsonObj Row JSON of user account.
     */
    private static JSONObject finalizeLogInQueryJson(JSONObject userJsonObj) throws SQLException {
        userJsonObj.remove("passw_hash");
        userJsonObj.remove("created_at");
        JSONUtils.formatKeysToCamelCase(userJsonObj);
        return userJsonObj;
    }

    /**
     * Finalize a log in request.
     * @param userJsonObje Row JSON of user account
     * @param conn SQL connection to use
     */
    private static Result finalizeLogInRequest(JSONObject userJsonObje, Connection conn) throws SQLException {
        PreparedStatement ps = null;
        ResultSet rs = null;
        JSONObject rootJsonObje = new JSONObject();

        rootJsonObje.put("user", userJsonObje);

        try {
            long userId = userJsonObje.getLong("id");

            // Do not allow the user to log out and log in until a small interval time has elapsed to ensure user data
            // stored in the Apache Cassandra database has been synced across nodes before the user retrieves it. Note:
            // This will not apply if a user logs in from another device while still being logged in on one device.
            ps = conn.prepareStatement("SELECT logged_out_at FROM " + DbConstants.SQL.USERS.TBL_USER_LOG +
                    " WHERE user_id = ?");
            ps.setLong(1, userId);
            rs = ps.executeQuery();

            if (rs.next()) {
                long minLogOutLogInIntr = 8000;
                Date loggedOutAt = rs.getTimestamp("logged_out_at", DateUtils.getUtcCalendar());
                DevLog.print(loggedOutAt);
                long logOutLogInDelta = System.currentTimeMillis() - loggedOutAt.getTime();

                if (logOutLogInDelta < minLogOutLogInIntr) {
                    try {
                        Thread.sleep(minLogOutLogInIntr - logOutLogInDelta);
                    } catch (InterruptedException e) {
                        LoggerFactory.getLogger(UsersEndpoint.class).error(
                                "Failed to pause thread in finalizeLogInRequest(): {}", e.toString());
                    }

                }
            }

            ps = conn.prepareStatement("UPDATE " + DbConstants.SQL.USERS.TBL_USER_LOG + " SET logged_in_at = '" +
                    DateUtils.toIso8601(new Date()) + "', times_logged_in = times_logged_in + 1 WHERE user_id = ?");
            ps.setLong(1, userId);
            ps.executeUpdate();

            ps = conn.prepareStatement("SELECT row_to_json(sq) FROM (SELECT * FROM " +
                    DbConstants.SQL.USERS.TBL_USER_ACTIVITY + " WHERE user_id = ?) sq;");
            ps.setLong(1, userId);
            rs = ps.executeQuery();

            if (rs.next()) {
                JSONObject jsonObje = new JSONObject(rs.getString("row_to_json"));
                JSONUtils.formatKeysToCamelCase(jsonObje); // TODO: make function return json
                rootJsonObje.put("userActivity", jsonObje);
            }
        } finally {
            SQLManager.closeQuietly(rs);
            SQLManager.closeQuietly(ps);
        }

        try {
            com.datastax.driver.core.ResultSet reslSet = Cassandra.getSession(Cassandra.Node.NODE_1).execute(
                    Cassandra.StatementCache.getStatement("SELECT section_id,voted_at FROM " +
                            DbConstants.CQL.TBL_USER_VOTES + " WHERE user_id = ?").bind(userJsonObje.getLong(
                                    "id")));
            if (reslSet == null) {
                return EndpointErrorUtils.processServerFatalError(DATABASE_TRANSACTION, "Failed to execute" +
                                " statement on table \"{}\" in finalizeLogInRequest(): ResultSet null",
                        DbConstants.CQL.TBL_USER_VOTES);
            } else {
                List<Row> rows = reslSet.all();
                JSONObject votesJsonObje = new JSONObject();

                for (Row row : rows) {
                    votesJsonObje.put(row.getString("section_id"), row.getTimestamp("voted_at").getTime());
                }
                rootJsonObje.put("userVotes", votesJsonObje);
            }
        } catch (NoHostAvailableException e) {
            return EndpointErrorUtils.processServerFatalError(DATABASE_TRANSACTION, "Failed to execute statement" +
                    " in finalizeLogInRequest(): {}", e.toString());
        }

        rootJsonObje.put("token", UserAuthentication.createToken(userJsonObje));
        return new Result(rootJsonObje);
    }

    /**
     * Log out a user.
     * @param userId Organization user account ID
     */
    @POST @Path("/log_out/{userId}")
    @Consumes("application/json") @Produces("application/json")
    public Response logOutRequest(@Context HttpServletRequest reqs, @Context HttpHeaders hh,
                                  @PathParam("userId") long userId) {
        if (!EndpointAuthenticationUtils.isUserTokenValid(hh, userId)) {
            return EndpointErrorUtils.processClientFatalError(reqs, UNAUTHORIZED, "Invalid user token")
                    .toResponse();
        }

        PreparedStatement ps = null;

        try (Connection conn = SQLManager.getConnection(true)) {
            ps = conn.prepareStatement("UPDATE " + DbConstants.SQL.USERS.TBL_USER_LOG + " SET logged_out_at = '" +
                    DateUtils.toIso8601(new Date()) + "' WHERE user_id = ?");
            ps.setLong(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            return EndpointErrorUtils.processServerFatalError(DATABASE_TRANSACTION, "Failed to execute update"
                    + " for table '{}' in logOutRequest(): " + e.toString(), DbConstants.SQL.USERS.TBL_USER_LOG)
                    .toResponse();
        } finally {
            SQLManager.closeQuietly(ps);
        }

        return new Result("User " + userId + " logged out").toResponse();
    }

    /**
     * Add and log in a new user.
     * @param user New user to add
     */
    @POST @Path("/add")
    @Consumes("application/json") @Produces("application/json")
    public Response addUserAndLogInRequest(@Context HttpServletRequest reqs, @Context HttpHeaders hh, User user) {
        if (!EndpointAuthenticationUtils.isSessionTokenValid(reqs, hh)) {
            return EndpointErrorUtils.processClientFatalError(reqs, UNAUTHORIZED, "Invalid session token")
                    .toResponse();
        }

        try (Connection conn = SQLManager.getConnection(false)) {
            Result res = executeInsertNewUser(reqs, user, LoginService.ORGANIZATION, conn);
            if (res.getError() != null) return res.toResponse();

            res = finalizeLogInRequest(executeLogInQuery(user.getName(), user.getPassword(), conn), conn);

            conn.commit();
            return res.toResponse();
        } catch (SQLException e) {
            return EndpointErrorUtils.processServerFatalError(DATABASE_TRANSACTION, "Failed to execute statement" +
                    " in addUserAndLogInRequest(): {}", e.toString()).toResponse();
        }
    }

    /**
     * Add a user.
     * @param user User to add
     * @return Result; if the user name already exists, an ALREADY_EXISTS error will be returned.
     */
    private static Result executeInsertNewUser(HttpServletRequest reqs, User user, LoginService loginService,
                                               Connection conn) {
        ArrayList<UserValidator.Error.TYPE.GROUP> vldtGrps = new ArrayList<>(3);
        vldtGrps.add(UserValidator.Error.TYPE.GROUP.NAME);

        String name = user.getName();
        String password = user.getPassword();
        int countryCode = user.getCountryCode();
        String phoneNumber = user.getPhoneNumber();
        String email = user.getEmail();

        if (phoneNumber != null && !phoneNumber.equals("")) {
            phoneNumber = phoneNumber.replaceAll("[^\\d]", "");
            user.setPhoneNumber(phoneNumber);
        }

        if (loginService == LoginService.ORGANIZATION) {
            vldtGrps.add(UserValidator.Error.TYPE.GROUP.PASSWORD);

            if (phoneNumber != null && !phoneNumber.equals("")) {
                vldtGrps.add(UserValidator.Error.TYPE.GROUP.PHONE_NUMBER);
            }
            if (email != null && !email.equals("")) {
                vldtGrps.add(UserValidator.Error.TYPE.GROUP.EMAIL);
            }
        }

        Result res = EndpointResourceValidationUtils.ensureConfigurationsAreInitialized();
        if (res.getError() != null) return res;

        List<UserValidator.Error> errs = UserValidator.validate(user, vldtGrps.toArray(
                new UserValidator.Error.TYPE.GROUP[0]));
        if (errs.size() > 0) {
            String msg = EndpointResourceValidationUtils.formatValidationErrorsResponseMessage(EndpointType.USERS,
                    errs);
            return EndpointErrorUtils.processClientFatalError(reqs, INVALID_RESOURCE, msg);
        }

        PreparedStatement ps = null;
        Statement stmt = null;
        ResultSet rs = null;
        long orgUserId;

        try {
            ps = conn.prepareStatement("SELECT name FROM " + DbConstants.SQL.USERS.TBL_USER +
                    " WHERE name = ?");
            ps.setString(1, name);
            rs = ps.executeQuery();

            if (rs.next()) {
                return new Result(ALREADY_EXISTS, "Username not available.");
            }

            if (phoneNumber != null && !phoneNumber.equals("")) {
                ps = conn.prepareStatement("SELECT phone_number FROM " + DbConstants.SQL.USERS.TBL_USER +
                        " WHERE country code = ? AND phone_number  = ?");
                ps.setInt(1, countryCode);
                ps.setString(2, phoneNumber);
                rs = ps.executeQuery();

                if (rs.next()) {
                    return new Result(ALREADY_EXISTS, "An account with phone number \"+" + countryCode +
                            " " + phoneNumber + "\" already exists.");
                }
            }
            if (email != null && !email.equals("")) {
                ps = conn.prepareStatement("SELECT email FROM " + DbConstants.SQL.USERS.TBL_USER +
                        " WHERE email = ?");
                ps.setString(1, email);
                rs = ps.executeQuery();

                if (rs.next()) {
                    return new Result(ALREADY_EXISTS, "An account with email \"" + email + "\" already" +
                            " exists.");
                }
            }

            StringBuilder sb = new StringBuilder(150);

            sb.append("INSERT INTO ");
            sb.append(DbConstants.SQL.USERS.TBL_USER);
            sb.append(" (name,");

            if (password != null && !password.equals("")) {
                sb.append(" passw_hash,");
            }
            if (phoneNumber != null && !phoneNumber.equals("")) {
                sb.append(" country_code,");
                sb.append(" phone_number,");
            }
            if (email != null && !email.equals("")) {
                sb.append(" email,");
            }

            sb.append(" created_at) VALUES ('");
            sb.append(name);
            sb.append("',");

            if (password != null && !password.equals("")) {
                sb.append(" generate_password_hash('");
                sb.append(password);
                sb.append("'),");
            }
            if (phoneNumber != null && !phoneNumber.equals("")) {
                sb.append(" '");
                sb.append(countryCode);
                sb.append("', '");
                sb.append(phoneNumber);
                sb.append("',");
            }
            if (email != null && !email.equals("")) {
                sb.append(" '");
                sb.append(email);
                sb.append("',");
            }

            sb.append(" '");
            sb.append(DateUtils.toIso8601(new Date()));
            sb.append("')");

            stmt = conn.createStatement();
            stmt.executeUpdate(sb.toString(), Statement.RETURN_GENERATED_KEYS);

            rs = stmt.getGeneratedKeys();
            rs.next();
            orgUserId = rs.getLong("id");
        } catch (SQLException e) {
            return EndpointErrorUtils.processServerFatalError(DATABASE_TRANSACTION, "Failed to execute statement" +
                    " in executeInsertNewUser(): " + e.toString());
        } finally {
            SQLManager.closeQuietly(rs);
            SQLManager.closeQuietly(ps);
            SQLManager.closeQuietly(stmt);
        }
        return new Result(orgUserId);
    }

    /**
     * Change a user user name.
     * @param userId Organization user account ID
     * @param userPassword User userPassword
     */
    @POST @Path("/change/name/{userId}")
    @Consumes("application/x-www-form-urlencoded") @Produces("application/json")
    public Response changeUserNameRequest(@Context HttpServletRequest reqs, @Context HttpHeaders hh,
                                          @PathParam("userId") long userId,
                                          @FormParam("userPassword") String userPassword,
                                          @FormParam("newName") String newName) {
        if (!EndpointAuthenticationUtils.isUserTokenValid(hh, userId)) {
            return EndpointErrorUtils.processClientFatalError(reqs, UNAUTHORIZED, "Invalid user token")
                    .toResponse();
        } else if (userPassword == null || userPassword.equals("")) {
            return EndpointErrorUtils.processClientFatalError(reqs, INVALID_PARAMETER, "Change user name: " +
                    "Parameter \"userPassword\" null or empty.").toResponse();
        } else if (newName == null || newName.equals("")) {
            return EndpointErrorUtils.processClientFatalError(reqs, INVALID_PARAMETER, "Change user name: " +
                    "Parameter \"newName\" null or empty.").toResponse();
        }

        Result res = EndpointResourceValidationUtils.ensureConfigurationsAreInitialized();
        if (res.getError() != null) return res.toResponse();

        List<UserValidator.Error> errs = UserValidator.validateName(newName);
        if (errs.size() > 0) {
            String msg = EndpointResourceValidationUtils.formatValidationErrorsResponseMessage(EndpointType.USERS,
                    errs);
            return EndpointErrorUtils.processClientFatalError(reqs, INVALID_RESOURCE, msg).toResponse();
        }

        PreparedStatement ps = null;
        ResultSet rs = null;

        try (Connection conn = SQLManager.getConnection(false)) {
            ps = conn.prepareStatement("SELECT id FROM " + DbConstants.SQL.USERS.TBL_USER + " WHERE id = ?");
            ps.setLong(1, userId);
            rs = ps.executeQuery();

            if (!rs.next()) {
                return EndpointErrorUtils.processClientFatalError(reqs, RESOURCE_NOT_FOUND, "Change user name:" +
                        " User  ID " + userId + " not found.").toResponse();
            }

            JSONObject jsonObje = executeLogInQuery(userId, userPassword, conn);

            if (jsonObje == null) {
                return new Result(INVALID_CREDENTIALS, "Password incorrect.").toResponse();
            }

            ps = conn.prepareStatement("SELECT changed_name_at FROM " + DbConstants.SQL.USERS.TBL_USER_LOG +
                    " WHERE user_id = ?");
            ps.setLong(1, userId);
            rs = ps.executeQuery();

            if (rs.next()) {
                Date date = rs.getTimestamp("changed_name_at", DateUtils.getUtcCalendar());

                if (date != null) {
                    long minTime = RegulationConfigurations.UserAccount.changeUserNameInterval();
                    if (System.currentTimeMillis() - date.getTime() < minTime) {
                        return new Result(NOT_ALLOWED, "It has been less than " + DateUtils
                                .formatMillisecondsToDays(minTime) + " days since this username has been changed.")
                                .toResponse();
                    }
                }
            }

            ps = conn.prepareStatement("SELECT name FROM " + DbConstants.SQL.USERS.TBL_USER +
                    " WHERE name = ?");
            ps.setString(1, newName);
            rs = ps.executeQuery();

            if (rs.next()) {
                return new Result(ALREADY_EXISTS, "Username not available.").toResponse();
            }

            ps = conn.prepareStatement("UPDATE " + DbConstants.SQL.USERS.TBL_USER +
                    " SET name = ? WHERE id = ?");
            ps.setString(1, newName);
            ps.setLong(2, userId);
            ps.executeUpdate();

            ps = conn.prepareStatement("UPDATE " + DbConstants.SQL.USERS.TBL_USER_LOG + " SET changed_name_at = '" +
                    DateUtils.toIso8601NoMillis(new Date()) + "' WHERE user_id = ?");
            ps.setLong(1, userId);
            ps.executeUpdate();

            conn.commit();
        } catch (SQLException e) {
            return EndpointErrorUtils.processServerFatalError(DATABASE_TRANSACTION, "Failed to execute statement" +
                    " in changeUserNameRequest(): {}", e.toString()).toResponse();
        } finally {
            SQLManager.closeQuietly(rs);
            SQLManager.closeQuietly(ps);
        }
        return new Result(new UpdateRequest("user name", newName)).toResponse();
    }

    /**
     * Change a user password.
     * @param userId Organization user account ID.
     */
    @POST @Path("/change/password/{userId}")
    @Consumes("application/x-www-form-urlencoded") @Produces("application/json")
    public Response changeUserPasswordRequest(@Context HttpServletRequest reqs, @Context HttpHeaders hh,
                                              @PathParam("userId") long userId,
                                              @FormParam("userPassword") String userPassword,
                                              @FormParam("newPassword") String newPassword) {
        if (!EndpointAuthenticationUtils.isUserTokenValid(hh, userId)) {
            return EndpointErrorUtils.processClientFatalError(reqs, UNAUTHORIZED, "Invalid user token")
                    .toResponse();
        } else if (userPassword == null || userPassword.equals("")) {
            return EndpointErrorUtils.processClientFatalError(reqs, INVALID_PARAMETER, "Change user password: " +
                    "Parameter \"password\" null or empty.").toResponse();
        } else if (newPassword == null || newPassword.equals("")) {
            return EndpointErrorUtils.processClientFatalError(reqs, INVALID_PARAMETER, "Change user password: " +
                    "Parameter \"newPassword\" null or empty.").toResponse();
        }

        PreparedStatement ps = null;

        try (Connection conn = SQLManager.getConnection(false)) {
            JSONObject rowJsonObje = executeLogInQuery(userId, userPassword, conn);

            if (rowJsonObje == null) {
                return new Result(INVALID_CREDENTIALS, "Password incorrect.").toResponse();
            }

            Result res = EndpointResourceValidationUtils.ensureConfigurationsAreInitialized();
            if (res.getError() != null) return res.toResponse();

            List<UserValidator.Error> errors = UserValidator.validatePassword(newPassword);

            if (errors.size() > 0) {
                return EndpointErrorUtils.processClientFatalError(reqs, INVALID_RESOURCE,
                        EndpointResourceValidationUtils.formatValidationErrorsResponseMessage(EndpointType.USERS,
                                errors)).toResponse();
            }

            ps = conn.prepareStatement("UPDATE " + DbConstants.SQL.USERS.TBL_USER + " SET passw_hash =" +
                    " generate_password_hash(?) WHERE id = ?");
            ps.setString(1, newPassword);
            ps.setLong(2, userId);
            ps.executeUpdate();

            conn.commit();
        } catch (SQLException e) {
            return EndpointErrorUtils.processServerFatalError(DATABASE_TRANSACTION, "Failed to execute statement" +
                    " in changeUserPasswordRequest(): {}", e.toString()).toResponse();
        } finally {
            SQLManager.closeQuietly(ps);
        }
        return new Result(new UpdateRequest("user password")).toResponse();
    }

    /**
     * Change a user phone number.
     * @param userId Organization user account ID
     */
    @POST @Path("/change/phone_number/{userId}")
    @Consumes("application/x-www-form-urlencoded") @Produces("application/json")
    public Response changeUserPhoneNumberRequest(@Context HttpServletRequest reqs, @Context HttpHeaders hh,
                                                 @PathParam("userId") long userId,
                                                 @FormParam("newCountryCode") int newCountryCode,
                                                 @FormParam("newPhoneNumber") String newPhoneNumber) {
        if (!EndpointAuthenticationUtils.isUserTokenValid(hh, userId)) {
            return EndpointErrorUtils.processClientFatalError(reqs, UNAUTHORIZED, "Invalid user token")
                    .toResponse();
        } else if (newCountryCode == 0) {
            return EndpointErrorUtils.processClientFatalError(reqs, INVALID_PARAMETER, "Change phone number:" +
                    " Parameter \"newCountryCode\" equals 0.").toResponse();
        } else if (newPhoneNumber == null || newPhoneNumber.equals("")) {
            return EndpointErrorUtils.processClientFatalError(reqs, INVALID_PARAMETER, "Change phone number:" +
                    " Parameter \"newPhoneNumber\" null or empty.").toResponse();
        }

        Result res = EndpointResourceValidationUtils.ensureConfigurationsAreInitialized();
        if (res.getError() != null) return res.toResponse();

        List<UserValidator.Error> errs = UserValidator.validatePhoneNumber(newCountryCode +
                newPhoneNumber);
        if (errs.size() > 0) {
            String msg = EndpointResourceValidationUtils.formatValidationErrorsResponseMessage(EndpointType.USERS,
                    errs);
            return EndpointErrorUtils.processClientFatalError(reqs, INVALID_RESOURCE, msg).toResponse();
        }

        PreparedStatement ps = null;
        ResultSet rs = null;

        try (Connection conn = SQLManager.getConnection(false)) {
            ps = conn.prepareStatement("SELECT id FROM " + DbConstants.SQL.USERS.TBL_USER + " WHERE id = ?");
            ps.setLong(1, userId);
            rs = ps.executeQuery();

            if (!rs.next()) {
                return EndpointErrorUtils.processClientFatalError(reqs, INVALID_PARAMETER, "Change phone number:" +
                        " User ID " + userId + " not found.").toResponse();
            }

            ps = conn.prepareStatement("SELECT id FROM " + DbConstants.SQL.USERS.TBL_USER +
                    " WHERE country_code = ? AND phone_number = ?");
            ps.setInt(1, newCountryCode);
            ps.setString(2, newPhoneNumber);
            rs = ps.executeQuery();

            if (rs.next()) {
                return new Result(ALREADY_EXISTS, "An account with phone number \"" + newCountryCode +
                        newPhoneNumber + "\" already exists.").toResponse();
            }

            ps = conn.prepareStatement("UPDATE " + DbConstants.SQL.USERS.TBL_USER + " SET country_code = ?," +
                    " phone_number = ? WHERE id = ?");
            ps.setInt(1, newCountryCode);
            ps.setString(2, newPhoneNumber);
            ps.setLong(3, userId);
            ps.executeUpdate();

            conn.commit();
        } catch (SQLException e) {
            return EndpointErrorUtils.processServerFatalError(DATABASE_TRANSACTION, "Failed to execute query or" +
                    " update in changeUserPhoneNumberRequest(): {}", e.toString()).toResponse();
        } finally {
            SQLManager.closeQuietly(rs);
            SQLManager.closeQuietly(ps);
        }
        return new Result(new UpdateRequest("user phone number", String.valueOf(newCountryCode),
                newPhoneNumber)).toResponse();
    }

    /**
     * Change a user email address.
     * @param userId Organization user account ID
     */
    @POST @Path("/change/email_address/{userId}")
    @Consumes("application/x-www-form-urlencoded") @Produces("application/json")
    public Response changeUserEmailAddressRequest(@Context HttpServletRequest reqs, @Context HttpHeaders hh,
                                                  @PathParam("userId") long userId,
                                                  @FormParam("newEmailAddress") String newEmailAddress) {
        if (!EndpointAuthenticationUtils.isUserTokenValid(hh, userId)) {
            return EndpointErrorUtils.processClientFatalError(reqs, UNAUTHORIZED, "Invalid user token")
                    .toResponse();
        } else if (newEmailAddress == null || newEmailAddress.equals("")) {
            return EndpointErrorUtils.processClientFatalError(reqs, INVALID_PARAMETER, "Change email address:" +
                    " Parameter \"newEmailAddress\" null or empty.").toResponse();
        }

        Result res = EndpointResourceValidationUtils.ensureConfigurationsAreInitialized();
        if (res.getError() != null) return res.toResponse();

        List<UserValidator.Error> errs = UserValidator.validateEmail(newEmailAddress);
        if (errs.size() > 0) {
            return EndpointErrorUtils.processClientFatalError(reqs, INVALID_RESOURCE, EndpointResourceValidationUtils
                    .formatValidationErrorsResponseMessage(EndpointType.USERS, errs)).toResponse();
        }

        PreparedStatement ps = null;
        ResultSet rs = null;

        try (Connection conn = SQLManager.getConnection(false)) {
            ps = conn.prepareStatement("SELECT id FROM " + DbConstants.SQL.USERS.TBL_USER +
                    " WHERE id = ?");
            ps.setLong(1, userId);
            rs = ps.executeQuery();

            if (!rs.next()) {
                return EndpointErrorUtils.processClientFatalError(reqs, INVALID_PARAMETER, "Change email" +
                        " address: User ID " + userId + " not found.").toResponse();
            }

            ps = conn.prepareStatement("SELECT id FROM " + DbConstants.SQL.USERS.TBL_USER +
                    " WHERE email = ?");
            ps.setString(1, newEmailAddress);
            rs = ps.executeQuery();

            if (rs.next()) {
                return new Result(ALREADY_EXISTS, "An account with email address \"" + newEmailAddress + "\"" +
                        " already exists.").toResponse();
            }

            ps = conn.prepareStatement("UPDATE " + DbConstants.SQL.USERS.TBL_USER +
                    " SET email = ? WHERE id = ?");
            ps.setString(1, newEmailAddress);
            ps.setLong(2, userId);
            ps.executeUpdate();

            conn.commit();
        } catch (SQLException e) {
            return EndpointErrorUtils.processServerFatalError(DATABASE_TRANSACTION, "Failed to execute query or" +
                    " update in changeUserEmailAddressRequest(): {}", e.toString()).toResponse();
        } finally {
            SQLManager.closeQuietly(rs);
            SQLManager.closeQuietly(ps);
        }
        return new Result(new UpdateRequest("user email address", newEmailAddress)).toResponse();
    }

    /**
     * Remove a user.
     * @param userId ID of user to remove
     * @param userPassword User password
     */
    @POST @Path("/remove/{userId}")
    @Consumes("application/x-www-form-urlencoded") @Produces("application/json")
    public Response removeUserRequest(@Context HttpServletRequest reqs, @Context HttpHeaders hh,
                                      @PathParam("userId") long userId,
                                      @FormParam("userPassword") String userPassword) {
        if (!EndpointAuthenticationUtils.isUserTokenValid(hh, userId)) {
            return EndpointErrorUtils.processClientFatalError(reqs, UNAUTHORIZED, "Invalid user token")
                    .toResponse();
        } else if (userPassword == null || userPassword.equals("")) {
            return EndpointErrorUtils.processClientFatalError(reqs, INVALID_PARAMETER, "Remove user: Parameter" +
                    " \"userPassword\" null or empty.").toResponse();
        }

        PreparedStatement prepStmt = null;

        try (Connection conn = SQLManager.getConnection(false)) {
            JSONObject rowJsonObje = executeLogInQuery(userId, userPassword, conn);

            if (rowJsonObje == null) {
                return new Result(INVALID_CREDENTIALS, "Password incorrect.").toResponse();
            }

            prepStmt = conn.prepareStatement("DELETE FROM " + DbConstants.SQL.USERS.TBL_USER +
                    " WHERE id = ?");
            prepStmt.setLong(1, userId);
            prepStmt.executeUpdate();

            conn.commit();
        } catch (SQLException e) {
            return EndpointErrorUtils.processServerFatalError(DATABASE_TRANSACTION, "Failed to execute statement" +
                    " in removeUserRequest(): {}", e.toString()).toResponse();
        } finally {
            SQLManager.closeQuietly(prepStmt);
        }
        return new Result(new UpdateRequest("remove user")).toResponse();
    }



    /**
     * Check if a name exists.
     * @param name Name
     */
    @POST @Path("/exists/name/{name}")
    @Consumes("application/json") @Produces("application/json")
    public Response checkIfNameExistsRequest(@Context HttpServletRequest reqs, @Context HttpHeaders hh,
                                             @PathParam("name") String name) {
        if (!EndpointAuthenticationUtils.isSessionTokenValid(reqs, hh)) {
            return EndpointErrorUtils.processClientFatalError(reqs, UNAUTHORIZED, "Invalid session token")
                    .toResponse();
        }
        return checkIfUserTableRowColumnValueExistsHelper("name", name).toResponse();
    }

    /**
     * Check if a phone number exists.
     * @param countryCode Country code of phone number
     * @param phoneNumber Phone number
     */
    @POST @Path("/exists/phone_number/{countryCode}/{phoneNumber}")
    @Consumes("application/json") @Produces("application/json")
    public Response checkIfPhoneNumberExistsRequest(@Context HttpServletRequest reqs, @Context HttpHeaders hh,
                                                    @PathParam("countryCode") int countryCode,
                                                    @PathParam("phoneNumber") String phoneNumber) {
        if (!EndpointAuthenticationUtils.isSessionTokenValid(reqs, hh)) {
            return EndpointErrorUtils.processClientFatalError(reqs, UNAUTHORIZED, "Invalid session token")
                    .toResponse();
        }

        PreparedStatement ps = null;
        ResultSet rs = null;

        try (Connection conn = SQLManager.getConnection(true)) {
            ps = conn.prepareStatement("SELECT id FROM " + DbConstants.SQL.USERS.TBL_USER +
                    " WHERE country_code = ? AND phone_number = ?");
            ps.setInt(1, countryCode);
            ps.setString(2, phoneNumber);
            rs = ps.executeQuery();

            if (rs.next()) {
                return new Result(new ValueRequest("true")).toResponse();
            }
        } catch (SQLException e) {
            return EndpointErrorUtils.processServerFatalError(DATABASE_TRANSACTION, "Failed to execute query in" +
                    " checkIfPhoneNumberExistsRequest(): " + e.toString()).toResponse();
        } finally {
            SQLManager.closeQuietly(rs);
            SQLManager.closeQuietly(ps);
        }
        return new Result(new ValueRequest("false")).toResponse();
    }

    /**
     * Check if an email address exists.
     * @param emailAddress Email address
     */
    @POST @Path("/exists/emailAddress/{emailAddress}")
    @Consumes("application/json") @Produces("application/json")
    public Response checkIfEmailAddressExistsRequest(@Context HttpServletRequest reqs, @Context HttpHeaders hh,
                                                     @PathParam("emailAddress") String emailAddress) {
        if (!EndpointAuthenticationUtils.isSessionTokenValid(reqs, hh)) {
            return EndpointErrorUtils.processClientFatalError(reqs, UNAUTHORIZED, "Invalid session token")
                    .toResponse();
        }
        return checkIfUserTableRowColumnValueExistsHelper("emailAddress", emailAddress).toResponse();
    }

    /**
     * Helper to check if a value for a column exists.
     * @param column Column of value to check.
     * @param value Value to check.
     */
    private Result checkIfUserTableRowColumnValueExistsHelper(String column, String value) {
        PreparedStatement ps = null;
        ResultSet rs = null;

        try (Connection conn = SQLManager.getConnection(true)) {
            ps = conn.prepareStatement("SELECT id FROM " + DbConstants.SQL.USERS.TBL_USER + " WHERE " +
                    column + " = ?");
            ps.setString(1, value);
            rs = ps.executeQuery();

            if (rs.next()) {
                return new Result(new ValueRequest("true"));
            }
        } catch (SQLException e) {
            return EndpointErrorUtils.processServerFatalError(DATABASE_TRANSACTION, "Failed to execute query in" +
                    " checkIfUserTableRowColumnValueExistsHelper(): " + e.toString());
        } finally {
            SQLManager.closeQuietly(rs);
            SQLManager.closeQuietly(ps);
        }
        return new Result(new ValueRequest("false"));
    }

}
