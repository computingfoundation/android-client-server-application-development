package com.fencedin.backend.rest;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.fencedin.backend.authentication.EndpointAuthenticationUtils;
import com.fencedin.backend.authentication.UserAuthentication;
import com.fencedin.backend.database.CQLManager;
import com.fencedin.backend.database.DbConstants;
import com.fencedin.backend.database.SQLManager;
import com.fencedin.commons.base.*;
import com.fencedin.commons.configuration.RegulationConfigurations;
import com.fencedin.commons.model.LoginService;
import com.fencedin.commons.model.User;
import com.fencedin.commons.model.UserCredits;
import com.fencedin.commons.model.UserSuiteVotes;
import com.fencedin.commons.rest.ClientGeneralEndpointsRequestError;
import com.fencedin.commons.rest.CommonResponseBody;
import com.fencedin.commons.validation.UserValidator;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.fencedin.backend.rest.ClientFatalEndpointsRequestError.TYPE.*;
import static com.fencedin.backend.rest.ServerFatalEndpointsRequestError.TYPE.DATABASE_TRANSACTION;
import static com.fencedin.backend.rest.ServerFatalEndpointsRequestError.TYPE.ILLEGAL_STATE;
import static com.fencedin.commons.rest.ClientGeneralEndpointsRequestError.TYPE.*;

/**
 * Users endpoint
 */
@Path("/users")
public class UsersEndpoint {
    /**
     * The user minimum log out-log in interval
     */
    private static final int USER_MINIMUM_LOG_OUT_LOG_IN_INTERVAL = 6500;

    /**
     * A type of a user identity entity.
     */
    public enum USER_IDENTITY_ENTITY_TYPE {
        NAME("username"),
        PHONE_NUMBER("phone number"),
        EMAIL_ADDRESS("email address");

        /**
         * The client representation
         */
        private String mClientRepresentation;

        USER_IDENTITY_ENTITY_TYPE(String clientStringRepresentation) {
            mClientRepresentation = clientStringRepresentation;
        }

        public String getClientStringRepresentation() {
            return mClientRepresentation;
        }

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }

    /**
     * Log in a user and get user log in data.
     */
    @POST @Path("/{userPhoneNumberEmailAddressOrName}/{userPassword}/log_in")
    @Produces("application/json")
    public Response logInUserAndGetUserLogInDataRequest(@Context HttpServletRequest reqs, @Context HttpHeaders hh,
                            @PathParam("userPhoneNumberEmailAddressOrName") String userPhoneNumberEmailAddressOrName,
                            @PathParam("userPassword") String userPassword) {
        if (!EndpointAuthenticationUtils.isSessionTokenValid(reqs, hh)) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(UNAUTHORIZED, "Session token invalid.")));
        } else if (userPassword == null || userPassword.equals("")) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(INVALID_PARAMETER,
                                    "Log in and get user: Parameter \"userPassword\" null or empty.")));
        }

        Connection conn;
        try {
            conn = SQLManager.getConnection(false);
        } catch (SQLException e) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForServerFatalEndpointsRequestError(
                            new ServerFatalEndpointsRequestError(DATABASE_TRANSACTION,
                                    "Failed to get SQL connection in logInUserAndGetUserLogInDataRequest():" +
                                            " {}", e.toString())));
        }

        PreparedStatement prepStmt = null;
        ResultSet reslSet = null;
        JSONObject userJsonObje;
        long userId;
        try {
            userJsonObje = executeLogInAndGetUserQuery(userPhoneNumberEmailAddressOrName, userPassword, conn);
            if (userJsonObje == null) {
                conn.commit();
                return EndpointsResponseProcessor.createResponseFromCommonResponseBody(new CommonResponseBody(
                        new ClientGeneralEndpointsRequestError(INVALID_CREDENTIALS,
                                "The provided credentials are invalid.", getUserIdentityEntityType(
                                        userPhoneNumberEmailAddressOrName).getClientStringRepresentation())));
            }

            userId = userJsonObje.getLong("id");

            // Wait a minimum log out-log in interval to ensure user data in the Cassandra database has been synced
            // across nodes. Note: This does not apply if the user logs in from another device while still being
            // logged in on one device. (This feature should be added in the future.)
            // TODO: The thread sleeping/scheduling process still has to be implemented.
            // A scheduler should be used as described at
            // https://stackoverflow.com/a/8202580 and https://stackoverflow.com/a/16178252. For now, a
            // MINIMUM_INTERVAL_NOT_ELAPSED error is returned and the thread sleeping operation is implemented in
            // the client application.
            prepStmt = conn.prepareStatement("SELECT logged_out_at FROM " + DbConstants.SQL.USERS.TBL_USER_LOG +
                    " WHERE user_id = ?");
            prepStmt.setLong(1, userId);
            reslSet = prepStmt.executeQuery();

            if (reslSet.next()) {
                Date logdOutAt = reslSet.getTimestamp("logged_out_at", DateUtils.getUTCCalendar());

                if (logdOutAt != null) {
                    if (System.currentTimeMillis() - logdOutAt.getTime() < USER_MINIMUM_LOG_OUT_LOG_IN_INTERVAL) {
                        conn.commit();
                        return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                                new CommonResponseBody(new ClientGeneralEndpointsRequestError(
                                        MINIMUM_INTERVAL_NOT_ELAPSED,
                                        "The minimum log out-log in interval has not elapsed.")));
                    }
                }
            }
        } catch (SQLException e) {
            SQLManager.closeQuietly(conn);
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForServerFatalEndpointsRequestError(
                            new ServerFatalEndpointsRequestError(DATABASE_TRANSACTION,
                                    "Failed to execute query in logInUserAndGetUserLogInDataRequest(): {}",
                                    e.toString())));
        } finally {
            SQLManager.closeQuietly(prepStmt);
            SQLManager.closeQuietly(reslSet);
        }

        JSONObject userLogInDataJsonObje;
        try {
            // TODO: separate user log query and other queries in executeUserLogInDataQueriesAndCreateUserLogInDataJson
            userLogInDataJsonObje = executeUserLogInDataQueriesAndCreateUserLogInDataJson(conn, userJsonObje);
            userLogInDataJsonObje.put("user", userJsonObje);
        } catch (SQLException | NoHostAvailableException | IllegalStateException e) {
            SQLManager.closeQuietly(conn);
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForServerFatalEndpointsRequestError(
                            new ServerFatalEndpointsRequestError(DATABASE_TRANSACTION,
                                    "Failed to execute query in logInUserAndGetUserLogInDataRequest(): {}",
                                    e.toString())));
        }

        try {
            userLogInDataJsonObje.put("userLog", executeUserLogQuery(conn, userId, true));

            JSONObject userPrdcPurcsQueryJsonObjc = executeUserProductPurchasesQuery(conn, userId);
            JSONObject userCrdtsJsonObjc;
            if (userPrdcPurcsQueryJsonObjc != null && userPrdcPurcsQueryJsonObjc.getJSONArray(
                    "productIds").length() > 0) {
                userCrdtsJsonObjc = new JSONObject(SerializeUtils.serialize(new UserCredits(userId,
                        1385)));
                userCrdtsJsonObjc.remove("userId");
            } else {
                userCrdtsJsonObjc = new JSONObject();
            }
            userLogInDataJsonObje.put("userCredits", userCrdtsJsonObjc);
        } catch (SQLException e) {
            SQLManager.closeQuietly(conn);
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForServerFatalEndpointsRequestError(
                            new ServerFatalEndpointsRequestError(DATABASE_TRANSACTION,
                                    "Failed to execute query in logInUserAndGetUserLogInDataRequest(): {}",
                                    e.toString())));
        }

        try {
            conn.commit();
        } catch (SQLException e) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForServerFatalEndpointsRequestError(
                            new ServerFatalEndpointsRequestError(DATABASE_TRANSACTION,
                                    "Failed to commit queries in logInUserAndGetUserLogInDataRequest(): {}",
                                    e.toString())));
        } finally {
            SQLManager.closeQuietly(conn);
        }
        return EndpointsResponseProcessor.createResponseWithJsonBodyAndStatusOk(userLogInDataJsonObje);
    }

    /**
     * Execute a log in and get user query.
     * @param userPhoneNumberEmailAddressOrName The user phone number, email address or name
     * @param userPassword The FencedIn user account password
     * @param connection The SQL connection to use or null to create one
     * @return A finalized user query SQL row JSON or null if the log in credentials are invalid
     * @throws SQLException If the SQL query fails
     */
    private static JSONObject executeLogInAndGetUserQuery(String userPhoneNumberEmailAddressOrName, String userPassword,
                                                          Connection connection) throws SQLException {
        USER_IDENTITY_ENTITY_TYPE userIdntType = getUserIdentityEntityType(
                userPhoneNumberEmailAddressOrName);
        String sqlLogInMethod = userIdntType.toString().replaceAll(" ", "_");

        if (sqlLogInMethod.equals("phone_number")) {
            userPhoneNumberEmailAddressOrName = userPhoneNumberEmailAddressOrName.replaceAll("[^\\d]", "");
        }

        Connection usedConn = connection;
        ResultSet reslSet = null;

        if (connection == null) {
            usedConn = SQLManager.getConnection(true);
        }

        try (PreparedStatement prepStmt = usedConn.prepareStatement("SELECT row_to_json(sq) FROM (SELECT * FROM" +
                " log_in_user_by_" + sqlLogInMethod + "(?, ?)) AS sq")) {
            prepStmt.setString(1, userPhoneNumberEmailAddressOrName);
            prepStmt.setString(2, userPassword);
            reslSet = prepStmt.executeQuery(); // TODO: Limit ResultSet to one result

            if (reslSet.next()) {
                JSONObject rowToJsonObje = new JSONObject(reslSet.getString("row_to_json"));

                SQLManager.closeQuietly(prepStmt);
                return finalizeLogInQueryJson(rowToJsonObje);
            } else {
                SQLManager.closeQuietly(prepStmt);
                return null;
            }
        } finally {
            SQLManager.closeQuietly(reslSet);
            if (connection == null) {
                SQLManager.closeQuietly(usedConn);
            }
        }
    }

    /**
     * Execute a log in and get user query by user ID.
     * @param userId The ID of the user
     * @param password FencedIn user account password; may be null
     * @param conn SQL connection to use; null to create one
     * @throws SQLException If the query statement fails
     */
    private static JSONObject executeLogInAndGetUserQueryByUserId(long userId, String password, Connection conn)
            throws SQLException {
        Connection usedConn = conn;
        ResultSet reslSet = null;

        if (conn == null) {
            usedConn = SQLManager.getConnection(true);
        }

        try (PreparedStatement prepStmt = usedConn.prepareStatement("SELECT row_to_json(sq) FROM (SELECT * FROM " +
                DbConstants.SQL.USERS.TBL_USER + " WHERE id = ? AND passw_hash = crypt(?, " +
                DbConstants.SQL.USERS.TBL_USER + ".passw_hash)) sq;")) {
            prepStmt.setLong(1, userId);
            if (password != null) {
                prepStmt.setString(2, password);
            }
            reslSet = prepStmt.executeQuery();

            if (reslSet.next()) {
                JSONObject rowToJsonObje = new JSONObject(reslSet.getString("row_to_json"));

                SQLManager.closeQuietly(prepStmt);
                return finalizeLogInQueryJson(rowToJsonObje);
            } else {
                SQLManager.closeQuietly(prepStmt);
                return null;
            }
        } finally {
            SQLManager.closeQuietly(reslSet);
            if (conn == null) {
                SQLManager.closeQuietly(usedConn);
            }
        }
    }

    /**
     * Execute the user log in data queries.
     * @param userJsonObject The JSON object of the user to query log in data against
     * @param connection The SQL connection to use
     * @return A JSON object containing user log in data
     * @throws SQLException If the SQL query fails
     * @throws NoHostAvailableException If no Cassandra host could be connected to
     * @throws IllegalStateException If the CQL query returned a null ResultSet
     */
    private static JSONObject executeUserLogInDataQueriesAndCreateUserLogInDataJson(Connection connection,
                                                                                    JSONObject userJsonObject)
            throws SQLException, NoHostAvailableException, IllegalStateException {
        long userId = userJsonObject.getLong("id");

        PreparedStatement prepStmt = null;
        ResultSet reslSet = null;
        JSONObject newRootJsonObje = new JSONObject();

        try {
            prepStmt = connection.prepareStatement("UPDATE " + DbConstants.SQL.USERS.TBL_USER_LOG +
                    " SET logged_in_at = '" + DateUtils.toIso8601(new Date()) +
                    "', times_logged_in = times_logged_in + 1 WHERE user_id = ?");
            prepStmt.setLong(1, userId);
            prepStmt.executeUpdate();

            prepStmt = connection.prepareStatement("SELECT row_to_json(sq) FROM (SELECT * FROM " +
                    DbConstants.SQL.USERS.TBL_USER_ACTIVITY + " WHERE user_id = ?) sq;");
            prepStmt.setLong(1, userId);
            reslSet = prepStmt.executeQuery();
            reslSet.next();

            JSONObject userActvJsonObje = new JSONObject(reslSet.getString("row_to_json"));
            userActvJsonObje.remove("user_id");
            JsonUtils.FormattingUtils.formatKeysToCamelCase(userActvJsonObje); // TODO: make function return json

            newRootJsonObje.put("userActivity", userActvJsonObje);
        } finally {
            SQLManager.closeQuietly(reslSet);
            SQLManager.closeQuietly(prepStmt);
        }

        Session sess = CQLManager.getSession();

        com.datastax.driver.core.ResultSet cqlReslSet = sess.execute(CQLManager.StatementCache.getStatement(
                "SELECT suite_id FROM " + DbConstants.CQL.Tables.USER_RECENT_SUITE_VOTES +
                        " WHERE user_id = ?").bind(userId));

        List<String> suiteIdList = CQLUtils.RowUtils.extractColumnUniqueValuesFromRows("suite_id",
                String.class, cqlReslSet.all());

        cqlReslSet = sess.execute(CQLManager.StatementCache.getStatement("SELECT suite_id,voted_at FROM " +
                DbConstants.CQL.Tables.USER_RECENT_SUITE_VOTES +
                " WHERE user_id = ? AND suite_id IN ? AND voted_at > ?").bind(userId, suiteIdList, new Timestamp(
                        System.currentTimeMillis() - (Times.TWENTY_FOUR_HOURS * 365L))));
        if (cqlReslSet == null) {
            throw new IllegalStateException(
                    "Failed to execute query in executeUserLogInDataQueriesAndCreateUserLogInDataJson(): ResultSet" +
                            " null");
        }

        ArrayList<UserSuiteVotes.UserSuiteVote> userSuiteVoteList = new ArrayList<>();
        for (Row row : cqlReslSet.all()) {
            userSuiteVoteList.add(new UserSuiteVotes.UserSuiteVote(
                    row.getString("suite_id"),
                    row.getTimestamp("voted_at")));
        }

        newRootJsonObje.put("userSuiteVotes", new JSONArray(SerializeUtils.serialize(new UserSuiteVotes(
                userSuiteVoteList))));
        newRootJsonObje.put("token", UserAuthentication.createToken(userJsonObject));
        return newRootJsonObje;
    }

    /**
     * Finalize the log in query JSON.
     * @param userJsonObje Row JSON of user account.
     */
    private static JSONObject finalizeLogInQueryJson(JSONObject userJsonObje) {
        userJsonObje.remove("passw_hash");
        userJsonObje.remove("created_at");
        JsonUtils.FormattingUtils.formatKeysToCamelCase(userJsonObje);
        return userJsonObje;
    }

    /**
     * Execute a user log query.
     * @param userId The ID of the user
     * @param returnOnlyColumnsForPublicRequest Whether or not to return only columns for a public request
     * @return A finalized user log query SQL row JSON or null if the user does not exist
     * @throws SQLException If the SQL query fails
     */
    private static JSONObject executeUserLogQuery(Connection connection, long userId,
                                                  boolean returnOnlyColumnsForPublicRequest) throws SQLException {
        Connection usedConn = connection;
        ResultSet reslSet = null;

        if (connection == null) {
            usedConn = SQLManager.getConnection(true);
        }

        String selcClauseColms;
        if (returnOnlyColumnsForPublicRequest) {
            selcClauseColms = "changed_name_at";
        } else {
            selcClauseColms = "*";
        }

        try (PreparedStatement prepStmt = usedConn.prepareStatement("SELECT row_to_json(sq) FROM (SELECT " +
                selcClauseColms + " FROM " + DbConstants.SQL.USERS.TBL_USER_LOG + " WHERE user_id = ?) AS sq")) {
            prepStmt.setLong(1, userId);
            reslSet = prepStmt.executeQuery(); // TODO: Limit ResultSet to one result

            if (reslSet.next()) {
                JSONObject rowToJsonObje = new JSONObject(reslSet.getString("row_to_json"));
                rowToJsonObje.remove("user_id");

                SQLManager.closeQuietly(prepStmt);
                JsonUtils.FormattingUtils.formatKeysToCamelCase(rowToJsonObje);
                return rowToJsonObje;
            } else {
                SQLManager.closeQuietly(prepStmt);
                return null;
            }
        } finally {
            SQLManager.closeQuietly(reslSet);
            if (connection == null) {
                SQLManager.closeQuietly(usedConn);
            }
        }
    }

    /**
     * Execute a user product purchases query.
     * @param userId The ID of the user
     * @return A finalized user log query SQL row JSON or null if the user does not exist
     * @throws SQLException If the SQL query fails
     */
    private static JSONObject executeUserProductPurchasesQuery(Connection connection, long userId) throws SQLException {
        Connection usedConn = connection;
        ResultSet reslSet = null;

        if (connection == null) {
            usedConn = SQLManager.getConnection(true);
        }

        try (PreparedStatement prepStmt = usedConn.prepareStatement("SELECT row_to_json(sq) FROM (SELECT * FROM " +
                DbConstants.SQL.USERS.TBL_USER_PRODUCT_PURCHASES + " WHERE user_id = ?) AS sq")) {
            prepStmt.setLong(1, userId);
            reslSet = prepStmt.executeQuery(); // TODO: Limit ResultSet to one result

            if (reslSet.next()) {
                JSONObject rowToJsonObje = new JSONObject(reslSet.getString("row_to_json"));
                // TODO: Unlike other queries, this one does not remove key "user_id". This should likely be the same
                //       for all other queries.
                //rowToJsonObje.remove("user_id");

                SQLManager.closeQuietly(prepStmt);
                JsonUtils.FormattingUtils.formatKeysToCamelCase(rowToJsonObje);
                return rowToJsonObje;
            } else {
                SQLManager.closeQuietly(prepStmt);
                return null;
            }
        } finally {
            SQLManager.closeQuietly(reslSet);
            if (connection == null) {
                SQLManager.closeQuietly(usedConn);
            }
        }
    }

    /**
     * Log out a user.
     * @param userId The ID of the user
     */
    @POST @Path("/{userId}/log_out")
    @Produces("application/json")
    public Response logOutUserRequest(@Context HttpServletRequest reqs, @Context HttpHeaders hh,
                                      @PathParam("userId") long userId) {
        if (!EndpointAuthenticationUtils.isUserTokenValid(hh, userId)) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(UNAUTHORIZED, "User token invalid.")));
        }

        PreparedStatement prepStmt = null;

        try (Connection conn = SQLManager.getConnection(true)) {
            prepStmt = conn.prepareStatement("UPDATE " + DbConstants.SQL.USERS.TBL_USER_LOG +
                    " SET logged_out_at = '" + DateUtils.toIso8601(new Date()) + "' WHERE user_id = ?");
            prepStmt.setLong(1, userId);
            prepStmt.executeUpdate();
        } catch (SQLException e) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForServerFatalEndpointsRequestError(
                            new ServerFatalEndpointsRequestError(DATABASE_TRANSACTION,
                                    "Failed to execute update query on table {} in logOutUserRequest(): {}",
                                    DbConstants.SQL.USERS.TBL_USER_LOG, e.toString())));
        } finally {
            SQLManager.closeQuietly(prepStmt);
        }
        return EndpointsResponseProcessor.createResponseWithEmptyBodyAndStatusOk();
    }

    // TODO: Create a single @PUT user method by making it allow an options user password as a query parameter.

    /**
     * Add and log in a user and get user log in data.
     */
    @POST
    @Consumes("application/json") @Produces("application/json")
    public Response addAndLogInUserAndGetUserLogInDataRequest(@Context HttpServletRequest reqs, @Context HttpHeaders hh,
                                                              User user) {
        if (!EndpointAuthenticationUtils.isSessionTokenValid(reqs, hh)) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(UNAUTHORIZED, "Session token invalid.")));
        }

        // Note: The connection used for this insert user query cannot be reused for the log in query because the log in
        // query depends on database data created by triggers run after a user has been inserted.
        CommonResponseBody commRespBody = executeInsertUserQuery(user, LoginService.FENCEDIN);
        if (commRespBody.getEndpointsRequestError() instanceof ClientFatalEndpointsRequestError) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor
                            .processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs, (
                                    ClientFatalEndpointsRequestError) commRespBody.getEndpointsRequestError()));
        } else if (commRespBody.getEndpointsRequestError() instanceof ServerFatalEndpointsRequestError) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor
                            .processAndCreateCommonResponseBodyForServerFatalEndpointsRequestError((
                                    ServerFatalEndpointsRequestError) commRespBody.getEndpointsRequestError()));
        } else if (commRespBody.getEndpointsRequestError() != null) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(commRespBody);
        }

        try (Connection conn = SQLManager.getConnection(false)) {
            JSONObject userJsonObje = executeLogInAndGetUserQuery(user.getName(), user.getPassword(), conn);
            JSONObject userLogInDataJsonObje = executeUserLogInDataQueriesAndCreateUserLogInDataJson(conn,
                    userJsonObje);
            userLogInDataJsonObje.put("user", userJsonObje);

            // TODO: Move this userLogQuery after the user log query and other quries in
            // executeUserLogInDataQueriesAndCreateUserLogInDataJson have been separated because it does not throw
            // Exceptions NoHostAvailableException and IllegalStateException.
            userLogInDataJsonObje.put("userLog", executeUserLogQuery(conn, userJsonObje.getLong(
                    "id"), true));

            userLogInDataJsonObje.put("userCredits", new JSONObject());

            conn.commit();
            return EndpointsResponseProcessor.createResponseWithJsonBodyAndStatusOk(userLogInDataJsonObje);
        } catch (SQLException | NoHostAvailableException | IllegalStateException e) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForServerFatalEndpointsRequestError(
                            new ServerFatalEndpointsRequestError(DATABASE_TRANSACTION,
                                    "Failed to execute query in addAndLogInUserAndGetUserLogInDataRequest():" +
                                            " {}", e.toString())));
        }
    }

    /**
     * Execute the insert user query.
     * @param user User to insert
     * @return A CommonResponseBody; if the user name already exists, an ALREADY_EXISTS error will be returned in it.
     */
    private static CommonResponseBody executeInsertUserQuery(User user, LoginService loginService) {
        ArrayList<UserValidator.Error.TYPE.GROUP> vldtGrps = new ArrayList<>(3);
        vldtGrps.add(UserValidator.Error.TYPE.GROUP.NAME);

        String name = user.getName();
        String passw = user.getPassword();
        int contCode = user.getCountryCode();
        String phoneNumb = user.getPhoneNumber();
        String emailAddr = user.getEmailAddress();

        if (phoneNumb != null && !phoneNumb.equals("")) {
            phoneNumb = phoneNumb.replaceAll("[^\\d]", "");
            user.setPhoneNumber(phoneNumb);
        }

        if (loginService == LoginService.FENCEDIN) {
            vldtGrps.add(UserValidator.Error.TYPE.GROUP.PASSWORD);

            if (phoneNumb != null && !phoneNumb.equals("")) {
                vldtGrps.add(UserValidator.Error.TYPE.GROUP.COUNTRY_CODE);
                vldtGrps.add(UserValidator.Error.TYPE.GROUP.PHONE_NUMBER);
            }
            if (emailAddr != null && !emailAddr.equals("")) {
                vldtGrps.add(UserValidator.Error.TYPE.GROUP.EMAIL_ADDRESS);
            }
        }

        if (!EndpointsUtils.ResourceValidationUtils.ensureNecessaryConfigurationsAreInitialized()) {
            return new CommonResponseBody(new ServerFatalEndpointsRequestError(ILLEGAL_STATE,
                    "Failed to initialize configurations."));
        }

        List<UserValidator.Error> vldtErrorList = UserValidator.validate(user, vldtGrps.toArray(
                new UserValidator.Error.TYPE.GROUP[0]));

        if (vldtErrorList.size() > 0) {
            return new CommonResponseBody(new ClientFatalEndpointsRequestError(INVALID_RESOURCE,
                    EndpointsUtils.ResourceValidationUtils.createResourceValidationErrorsResponseMessage(
                            ENDPOINT_TYPE.USERS, vldtErrorList)));
        }

        PreparedStatement prepStmt = null;
        Statement stmt = null;
        ResultSet reslSet = null;
        long fiUserId;

        try (Connection conn = SQLManager.getConnection(false)) {
            prepStmt = conn.prepareStatement("SELECT name FROM " + DbConstants.SQL.USERS.TBL_USER +
                    " WHERE name = ?");
            prepStmt.setString(1, name);
            reslSet = prepStmt.executeQuery();

            if (reslSet.next()) {
                conn.commit();
                return new CommonResponseBody(new ClientGeneralEndpointsRequestError(ALREADY_EXISTS,
                        "Username already exists."));
            }

            if (phoneNumb != null && !phoneNumb.equals("")) {
                prepStmt = conn.prepareStatement("SELECT phone_number FROM " + DbConstants.SQL.USERS.TBL_USER +
                        " WHERE country_code = ? AND phone_number  = ?");
                prepStmt.setInt(1, contCode);
                prepStmt.setString(2, phoneNumb);
                reslSet = prepStmt.executeQuery();

                if (reslSet.next()) {
                    conn.commit();
                    return new CommonResponseBody(new ClientGeneralEndpointsRequestError(ALREADY_EXISTS,
                            "An account with phone number {}{} already exists.", contCode, phoneNumb));
                }
            }
            if (emailAddr != null && !emailAddr.equals("")) {
                prepStmt = conn.prepareStatement("SELECT email_address FROM " + DbConstants.SQL.USERS.TBL_USER +
                        " WHERE email_address = ?");
                prepStmt.setString(1, emailAddr);
                reslSet = prepStmt.executeQuery();

                if (reslSet.next()) {
                    conn.commit();
                    return new CommonResponseBody(new ClientGeneralEndpointsRequestError(ALREADY_EXISTS,
                            "An account with email address {} already exists.", emailAddr));
                }
            }

            StringBuilder strnBuld = new StringBuilder(150);

            strnBuld.append("INSERT INTO ");
            strnBuld.append(DbConstants.SQL.USERS.TBL_USER);
            strnBuld.append(" (name,");

            if (passw != null && !passw.equals("")) {
                strnBuld.append(" passw_hash,");
            }
            if (phoneNumb != null && !phoneNumb.equals("")) {
                strnBuld.append(" country_code,");
                strnBuld.append(" phone_number,");
            }
            if (emailAddr != null && !emailAddr.equals("")) {
                strnBuld.append(" email_address,");
            }
            strnBuld.append(" created_at) VALUES ('");

            strnBuld.append(name);
            strnBuld.append("',");
            if (passw != null && !passw.equals("")) {
                strnBuld.append(" generate_password_hash('");
                strnBuld.append(passw);
                strnBuld.append("'),");
            }
            if (phoneNumb != null && !phoneNumb.equals("")) {
                strnBuld.append(" '");
                strnBuld.append(contCode);
                strnBuld.append("', '");
                strnBuld.append(phoneNumb);
                strnBuld.append("',");
            }
            if (emailAddr != null && !emailAddr.equals("")) {
                strnBuld.append(" '");
                strnBuld.append(emailAddr);
                strnBuld.append("',");
            }
            strnBuld.append(" '");
            strnBuld.append(DateUtils.toIso8601NoMillis(new Date()));
            strnBuld.append("')");

            stmt = conn.createStatement();
            stmt.executeUpdate(strnBuld.toString(), Statement.RETURN_GENERATED_KEYS);

            reslSet = stmt.getGeneratedKeys();
            reslSet.next();
            fiUserId = reslSet.getLong("id");

            conn.commit();
        } catch (SQLException e) {
            return new CommonResponseBody(new ServerFatalEndpointsRequestError(DATABASE_TRANSACTION,
                    "Failed to execute query in executeInsertUserQuery(): {}", e.toString()));
        } finally {
            SQLManager.closeQuietly(reslSet);
            SQLManager.closeQuietly(prepStmt);
            SQLManager.closeQuietly(stmt);
        }
        return new CommonResponseBody(new CommonResponseBody.Subresult(fiUserId));
    }

    /**
     * Change a user name.
     * @param userId The ID of the user
     * @param userPassword The password of the user
     */
    @PUT @Path("/{userId}/name/{userNewName}/{userPassword}")
    @Produces("application/json")
    public Response changeUserNameRequest(@Context HttpServletRequest reqs, @Context HttpHeaders hh,
                                          @PathParam("userId") long userId,
                                          @PathParam("userNewName") String userNewName,
                                          @PathParam("userPassword") String userPassword) {
        if (!EndpointAuthenticationUtils.isUserTokenValid(hh, userId)) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(UNAUTHORIZED, "User token invalid.")));
        } else if (userNewName == null || userNewName.equals("")) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(INVALID_PARAMETER,
                                    "Change user name: Parameter \"userNewName\" null or empty.")));
        } else if (userPassword == null || userPassword.equals("")) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(INVALID_PARAMETER,
                                    "Change user name: Parameter \"userPassword\" null or empty.")));
        }

        if (!EndpointsUtils.ResourceValidationUtils.ensureNecessaryConfigurationsAreInitialized()) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForServerFatalEndpointsRequestError(
                            new ServerFatalEndpointsRequestError(ILLEGAL_STATE,
                                    "Failed to initialize configurations.")));
        }

        List<UserValidator.Error> valdErrors = UserValidator.validateName(userNewName);
        if (valdErrors.size() > 0) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(INVALID_RESOURCE,
                                    EndpointsUtils.ResourceValidationUtils
                                            .createResourceValidationErrorsResponseMessage(ENDPOINT_TYPE.USERS,
                                                    valdErrors))));
        }

        PreparedStatement prepStmt = null;
        ResultSet reslSet = null;

        try (Connection conn = SQLManager.getConnection(false)) {
            prepStmt = conn.prepareStatement("SELECT id FROM " + DbConstants.SQL.USERS.TBL_USER + " WHERE id = ?");
            prepStmt.setLong(1, userId);
            reslSet = prepStmt.executeQuery();
            if (!reslSet.next()) {
                return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                        EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                                new ClientFatalEndpointsRequestError(RESOURCE_DOES_NOT_EXIST,
                                        "Change user name: User {} does not exist.", userId)));
            }

            if (executeLogInAndGetUserQueryByUserId(userId, userPassword, conn) == null) {
                return EndpointsResponseProcessor.createResponseFromCommonResponseBody(new CommonResponseBody(
                        new ClientGeneralEndpointsRequestError(INVALID_CREDENTIALS,
                                "The password is incorrect.")));
            }

            prepStmt = conn.prepareStatement("SELECT changed_name_at FROM " + DbConstants.SQL.USERS.TBL_USER_LOG +
                    " WHERE user_id = ?");
            prepStmt.setLong(1, userId);
            reslSet = prepStmt.executeQuery();
            if (reslSet.next()) {
                Date date = reslSet.getTimestamp("changed_name_at", DateUtils.getUTCCalendar());
                if (date != null) {
                    long minTime = RegulationConfigurations.UserAccount.changeUserNameMinimumInterval();
                    if (System.currentTimeMillis() - date.getTime() < minTime) {
                        return EndpointsResponseProcessor.createResponseFromCommonResponseBody(new CommonResponseBody(
                                new ClientGeneralEndpointsRequestError(MINIMUM_INTERVAL_NOT_ELAPSED,
                                        "It has been less than {} days since this username has been changed.",
                                        DateUtils.formatMillisecondsToDays(minTime))));
                    }
                }
            }

            prepStmt = conn.prepareStatement("SELECT name FROM " + DbConstants.SQL.USERS.TBL_USER +
                    " WHERE name = ?");
            prepStmt.setString(1, userNewName);
            reslSet = prepStmt.executeQuery();
            if (reslSet.next()) {
                return EndpointsResponseProcessor.createResponseFromCommonResponseBody(new CommonResponseBody(
                        new ClientGeneralEndpointsRequestError(ALREADY_EXISTS, "Username already exists.")));
            }

            prepStmt = conn.prepareStatement("UPDATE " + DbConstants.SQL.USERS.TBL_USER +
                    " SET name = ? WHERE id = ?");
            prepStmt.setString(1, userNewName);
            prepStmt.setLong(2, userId);
            prepStmt.executeUpdate();

            prepStmt = conn.prepareStatement("UPDATE " + DbConstants.SQL.USERS.TBL_USER_LOG +
                    " SET changed_name_at = '" + DateUtils.toIso8601NoMillis(new Date()) + "' WHERE user_id = ?");
            prepStmt.setLong(1, userId);
            prepStmt.executeUpdate();

            conn.commit();
        } catch (SQLException e) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForServerFatalEndpointsRequestError(
                            new ServerFatalEndpointsRequestError(DATABASE_TRANSACTION,
                                    "Failed to execute query in changeUserNameRequest(): {}", e.toString())));
        } finally {
            SQLManager.closeQuietly(reslSet);
            SQLManager.closeQuietly(prepStmt);
        }
        return EndpointsResponseProcessor.createResponseWithEmptyBodyAndStatusOk();
    }

    /**
     * Change a user phone number.
     * @param userId The ID of the user
     */
    @PUT @Path("/{userId}/phone_number/{userNewCountryCode}/{userNewPhoneNumber}")
    @Produces("application/json")
    public Response changeUserPhoneNumberRequest(@Context HttpServletRequest reqs, @Context HttpHeaders hh,
                                                 @PathParam("userId") long userId,
                                                 @PathParam("userNewCountryCode") int userNewCountryCode,
                                                 @PathParam("userNewPhoneNumber") String userNewPhoneNumber) {
        if (!EndpointAuthenticationUtils.isUserTokenValid(hh, userId)) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(UNAUTHORIZED, "User token invalid.")));
        } else if (userNewCountryCode == 0) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(INVALID_PARAMETER,
                                    "Change user phone number: Parameter \"userNewCountryCode\" equals 0.")));
        } else if (userNewPhoneNumber == null || userNewPhoneNumber.equals("")) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(INVALID_PARAMETER,
                                    "Change user phone number: Parameter \"userNewPhoneNumber\" null or" +
                                            " empty.")));
        }

        if (!EndpointsUtils.ResourceValidationUtils.ensureNecessaryConfigurationsAreInitialized()) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForServerFatalEndpointsRequestError(
                            new ServerFatalEndpointsRequestError(ILLEGAL_STATE,
                                    "Failed to initialize configurations.")));
        }

        List<UserValidator.Error> valdErrors = UserValidator.validateCountryCode(userNewCountryCode);
        valdErrors.addAll(UserValidator.validatePhoneNumber(userNewPhoneNumber));
        if (valdErrors.size() > 0) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(INVALID_PARAMETER,
                                    EndpointsUtils.ResourceValidationUtils
                                            .createResourceValidationErrorsResponseMessage(ENDPOINT_TYPE.USERS,
                                                    valdErrors))));
        }

        PreparedStatement prepStmt = null;
        ResultSet reslSet = null;

        try (Connection conn = SQLManager.getConnection(false)) {
            prepStmt = conn.prepareStatement("SELECT id FROM " + DbConstants.SQL.USERS.TBL_USER + " WHERE id = ?");
            prepStmt.setLong(1, userId);
            reslSet = prepStmt.executeQuery();

            if (!reslSet.next()) {
                return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                        EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                                new ClientFatalEndpointsRequestError(RESOURCE_DOES_NOT_EXIST,
                                        "Change user phone number: User {} does not exist.", userId)));
            }

            prepStmt = conn.prepareStatement("SELECT id FROM " + DbConstants.SQL.USERS.TBL_USER +
                    " WHERE country_code = ? AND phone_number = ?");
            prepStmt.setInt(1, userNewCountryCode);
            prepStmt.setString(2, userNewPhoneNumber);
            reslSet = prepStmt.executeQuery();

            if (reslSet.next()) {
                return EndpointsResponseProcessor.createResponseFromCommonResponseBody(new CommonResponseBody(
                        new ClientGeneralEndpointsRequestError(ALREADY_EXISTS,
                                "An account with phone number {}{} already exists.", userNewCountryCode,
                                userNewPhoneNumber)));
            }

            prepStmt = conn.prepareStatement("UPDATE " + DbConstants.SQL.USERS.TBL_USER + " SET country_code = ?," +
                    " phone_number = ? WHERE id = ?");
            prepStmt.setInt(1, userNewCountryCode);
            prepStmt.setString(2, userNewPhoneNumber);
            prepStmt.setLong(3, userId);
            prepStmt.executeUpdate();

            conn.commit();
        } catch (SQLException e) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForServerFatalEndpointsRequestError(
                            new ServerFatalEndpointsRequestError(DATABASE_TRANSACTION,
                                    "Failed to execute query or update in changeUserPhoneNumberRequest(): {}",
                                    e.toString())));
        } finally {
            SQLManager.closeQuietly(reslSet);
            SQLManager.closeQuietly(prepStmt);
        }
        return EndpointsResponseProcessor.createResponseWithEmptyBodyAndStatusOk();
    }

    /**
     * Change a user email address.
     * @param userId The ID of the user
     */
    @PUT @Path("/{userId}/email_address/{userNewEmailAddress}")
    @Produces("application/json")
    public Response changeUserEmailAddressRequest(@Context HttpServletRequest reqs, @Context HttpHeaders hh,
                                                  @PathParam("userId") long userId,
                                                  @PathParam("userNewEmailAddress") String userNewEmailAddress) {
        if (!EndpointAuthenticationUtils.isUserTokenValid(hh, userId)) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(UNAUTHORIZED, "User token invalid.")));
        } else if (userNewEmailAddress == null || userNewEmailAddress.equals("")) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(INVALID_PARAMETER,
                                    "Change user email address: Parameter \"userNewEmailAddress\" null or" +
                                            " empty.")));
        }

        if (!EndpointsUtils.ResourceValidationUtils.ensureNecessaryConfigurationsAreInitialized()) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForServerFatalEndpointsRequestError(
                            new ServerFatalEndpointsRequestError(ILLEGAL_STATE,
                                    "Failed to initialize configurations.")));
        }

        List<UserValidator.Error> valdErrors = UserValidator.validateEmailAddress(userNewEmailAddress);
        if (valdErrors.size() > 0) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(INVALID_PARAMETER,
                                    EndpointsUtils.ResourceValidationUtils
                                            .createResourceValidationErrorsResponseMessage(ENDPOINT_TYPE.USERS,
                                                    valdErrors))));
        }

        PreparedStatement prepStmt = null;
        ResultSet reslSet = null;

        try (Connection conn = SQLManager.getConnection(false)) {
            prepStmt = conn.prepareStatement("SELECT id FROM " + DbConstants.SQL.USERS.TBL_USER +
                    " WHERE id = ?");
            prepStmt.setLong(1, userId);
            reslSet = prepStmt.executeQuery();

            if (!reslSet.next()) {
                return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                        EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                                new ClientFatalEndpointsRequestError(RESOURCE_DOES_NOT_EXIST,
                                        "Change user email address:User {} does not exist.", userId)));
            }

            prepStmt = conn.prepareStatement("SELECT id FROM " + DbConstants.SQL.USERS.TBL_USER +
                    " WHERE email_address = ?");
            prepStmt.setString(1, userNewEmailAddress);
            reslSet = prepStmt.executeQuery();

            if (reslSet.next()) {
                return EndpointsResponseProcessor.createResponseFromCommonResponseBody(new CommonResponseBody(
                        new ClientGeneralEndpointsRequestError(ALREADY_EXISTS,
                                "An account with email address {} already exists.", userNewEmailAddress)));
            }

            prepStmt = conn.prepareStatement("UPDATE " + DbConstants.SQL.USERS.TBL_USER +
                    " SET email_address = ? WHERE id = ?");
            prepStmt.setString(1, userNewEmailAddress);
            prepStmt.setLong(2, userId);
            prepStmt.executeUpdate();

            conn.commit();
        } catch (SQLException e) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForServerFatalEndpointsRequestError(
                            new ServerFatalEndpointsRequestError(DATABASE_TRANSACTION,
                                    "Failed to execute query or update in changeUserEmailAddressRequest(): {}",
                                    e.toString())));
        } finally {
            SQLManager.closeQuietly(reslSet);
            SQLManager.closeQuietly(prepStmt);
        }
        return EndpointsResponseProcessor.createResponseWithEmptyBodyAndStatusOk();
    }

    // TODO: The "Remove" requests for user properties use a "-" instead of "_" for entity in the path because if, e.g.,
    //       a user password is "phone_number" and the removeUser request is made, the removeUserPhoneNumber request
    //       will be made instead. This is a temporary solution.

    /**
     * Remove a user phone number.
     * @param userId The ID of the user
     */
    @DELETE @Path("/{userId}/phone-number")
    @Produces("application/json")
    public Response removeUserPhoneNumberRequest(@Context HttpServletRequest reqs, @Context HttpHeaders hh,
                                                 @PathParam("userId") long userId) {
        if (!EndpointAuthenticationUtils.isUserTokenValid(hh, userId)) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(UNAUTHORIZED, "User token invalid.")));
        }

        PreparedStatement prepSttm = null;
        ResultSet reslSet = null;

        try (Connection conn = SQLManager.getConnection(false)) {
            prepSttm = conn.prepareStatement("SELECT * FROM " + DbConstants.SQL.USERS.TBL_USER + " WHERE id = ?");
            prepSttm.setLong(1, userId);
            reslSet = prepSttm.executeQuery();

            if (!reslSet.next()) {
                return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                        EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                                new ClientFatalEndpointsRequestError(RESOURCE_DOES_NOT_EXIST,
                                        "Remove user phone number: User {} does not exist.", userId)));
            }

            if (reslSet.getString("phone_number") == null) {
                return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                        EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                                new ClientFatalEndpointsRequestError(ILLEGAL_ACTION,
                                        "Remove user phone number: A phone number for user {} does not exist.",
                                        userId)));
            }

            if (reslSet.getString("email_address") == null) {
                return EndpointsResponseProcessor.createResponseFromCommonResponseBody(new CommonResponseBody(
                        new ClientGeneralEndpointsRequestError(NOT_ALLOWED,
                        "The phone number cannot be removed because at least one form of contact is" +
                                " required per account.")));
            }

            prepSttm = conn.prepareStatement("UPDATE " + DbConstants.SQL.USERS.TBL_USER +
                    " SET country_code = NULL, phone_number = NULL WHERE id = ?");
            prepSttm.setLong(1, userId);
            prepSttm.executeUpdate();

            conn.commit();
        } catch (SQLException e) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForServerFatalEndpointsRequestError(
                            new ServerFatalEndpointsRequestError(DATABASE_TRANSACTION,
                                    "Failed to execute query or update in removeUserPhoneNumberRequest(): {}",
                                    e.toString())));
        } finally {
            SQLManager.closeQuietly(reslSet);
            SQLManager.closeQuietly(prepSttm);
        }
        return EndpointsResponseProcessor.createResponseWithEmptyBodyAndStatusOk();
    }

    /**
     * Remove a user email address.
     * @param userId The ID of the user
     */
    @DELETE @Path("/{userId}/email-address")
    @Produces("application/json")
    public Response removeUserEmailAddressRequest(@Context HttpServletRequest reqs, @Context HttpHeaders hh,
                                                  @PathParam("userId") long userId) {
        if (!EndpointAuthenticationUtils.isUserTokenValid(hh, userId)) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(UNAUTHORIZED, "User token invalid.")));
        }

        PreparedStatement prepSttm = null;
        ResultSet reslSet = null;

        try (Connection conn = SQLManager.getConnection(false)) {
            prepSttm = conn.prepareStatement("SELECT * FROM " + DbConstants.SQL.USERS.TBL_USER + " WHERE id = ?");
            prepSttm.setLong(1, userId);
            reslSet = prepSttm.executeQuery();

            if (!reslSet.next()) {
                return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                        EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                                new ClientFatalEndpointsRequestError(RESOURCE_DOES_NOT_EXIST,
                                        "Remove user email address: User {} does not exist.", userId)));
            }

            if (reslSet.getString("email_address") == null) {
                return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                        EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                                new ClientFatalEndpointsRequestError(ILLEGAL_ACTION,
                                        "Remove user email address: An email address for user {} does not" +
                                                " exist.", userId)));
            }

            if (reslSet.getString("phone_number") == null) {
                return EndpointsResponseProcessor.createResponseFromCommonResponseBody(new CommonResponseBody(
                        new ClientGeneralEndpointsRequestError(NOT_ALLOWED,
                                "The email address cannot be removed because at least one form of contact is" +
                                " required per account.")));
            }

            prepSttm = conn.prepareStatement("UPDATE " + DbConstants.SQL.USERS.TBL_USER +
                    " SET email_address = NULL WHERE id = ?");
            prepSttm.setLong(1, userId);
            prepSttm.executeUpdate();

            conn.commit();
        } catch (SQLException e) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForServerFatalEndpointsRequestError(
                            new ServerFatalEndpointsRequestError(DATABASE_TRANSACTION,
                                    "Failed to execute query or update in removeUserEmailAddressRequest(): {}",
                                    e.toString())));
        } finally {
            SQLManager.closeQuietly(reslSet);
            SQLManager.closeQuietly(prepSttm);
        }
        return EndpointsResponseProcessor.createResponseWithEmptyBodyAndStatusOk();
    }

    /**
     * Remove a user.
     * @param userId the ID of the user to remove
     * @param userPassword The password of the user being removed
     */
    @DELETE @Path("/{userId}/{userPassword}")
    @Produces("application/json")
    public Response removeUserRequest(@Context HttpServletRequest reqs, @Context HttpHeaders hh,
                                      @PathParam("userId") long userId,
                                      @PathParam("userPassword") String userPassword) {
        if (!EndpointAuthenticationUtils.isUserTokenValid(hh, userId)) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(UNAUTHORIZED, "User token invalid.")));
        } else if (userPassword == null || userPassword.equals("")) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(INVALID_PARAMETER,
                                    "Remove user: Parameter \"userPassword\" null or empty.")));
        }

        PreparedStatement prepStmt = null;

        try (Connection conn = SQLManager.getConnection(false)) {
            if (executeLogInAndGetUserQueryByUserId(userId, userPassword, conn) == null) {
                conn.commit();
                return EndpointsResponseProcessor.createResponseFromCommonResponseBody(new CommonResponseBody(
                        new ClientGeneralEndpointsRequestError(INVALID_CREDENTIALS,
                                "The password is incorrect.")));
            }

            prepStmt = conn.prepareStatement("DELETE FROM " + DbConstants.SQL.USERS.TBL_USER +
                    " WHERE id = ?");
            prepStmt.setLong(1, userId);
            prepStmt.executeUpdate();

            conn.commit();
        } catch (SQLException e) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForServerFatalEndpointsRequestError(
                            new ServerFatalEndpointsRequestError(DATABASE_TRANSACTION,
                                    "Failed to execute query in removeUserRequest(): {}", e.toString())));
        } finally {
            SQLManager.closeQuietly(prepStmt);
        }

        try {
            Session sess = CQLManager.getSession();

            sess.execute(CQLManager.StatementCache.getStatement("DELETE FROM " +
                    DbConstants.CQL.Tables.USER_AUTHENTICATION_KEYS + " WHERE user_id = ?").bind(userId));

            sess.execute(CQLManager.StatementCache.getStatement("DELETE FROM " +
                    DbConstants.CQL.Tables.USER_RECENT_SUITE_VOTES + " WHERE user_id = ?").bind(userId));

            com.datastax.driver.core.ResultSet cqlReslSet = sess.execute(CQLManager.StatementCache.getStatement(
                    "SELECT suite_id FROM " + DbConstants.CQL.Tables.USER_VOTED_SUITES + " WHERE user_id = ?").bind(
                    userId));

            List<String> suiteIdList = CQLUtils.RowUtils.extractColumnUniqueValuesFromRows("suite_id",
                    String.class, cqlReslSet.all());

            sess.execute(CQLManager.StatementCache.getStatement("DELETE FROM " +
                    DbConstants.CQL.Tables.SUITE_VOTED_USERS + " WHERE suite_id IN ? AND user_id = ?").bind(suiteIdList,
                    userId));

            sess.execute(CQLManager.StatementCache.getStatement("DELETE FROM " +
                    DbConstants.CQL.Tables.USER_VOTED_SUITES + " WHERE user_id = ?").bind(userId));

            sess.execute(CQLManager.StatementCache.getStatement("DELETE FROM " +
                    DbConstants.CQL.Tables.USER_RECENT_SUITE_CONNECTIONS + " WHERE user_id = ?").bind(userId));
        } catch (NoHostAvailableException e) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForServerFatalEndpointsRequestError(
                            new ServerFatalEndpointsRequestError(DATABASE_TRANSACTION,
                                    "Failed to execute query in removeUserRequest(): {}", e.toString())));
        }
        return EndpointsResponseProcessor.createResponseWithEmptyBodyAndStatusOk();
    }

    // TODO: The @PUT user method below is currently used only to update the password of a user verified by a
    //       verification code. It should replace all the other update user methods in the future.

    /**
     * Update a user.
     * @param userId The ID of the updated user or -1 if the user was updated via a verification process, such as a
     *               password reset process.
     * @param updatedUser The updated user
     * @param userPassword The password of the user; used for additional security
     * @param verificationCode The verification code used to verify a user
     */
    @PUT @Path("/{userId}")
    @Produces("application/json")
    public Response updateUserRequest(@Context HttpServletRequest reqs, @Context HttpHeaders hh,
                                      @PathParam("userId") long userId,
                                      User updatedUser,
                                      @QueryParam("userPassword") String userPassword,
                                      @QueryParam("verificationCode") String verificationCode) {
        if (userId != -1) {
            if (!EndpointAuthenticationUtils.isUserTokenValid(hh, userId)) {
                return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                        EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                                new ClientFatalEndpointsRequestError(UNAUTHORIZED, "User token invalid.")));
            }
        } else {
            if (!EndpointAuthenticationUtils.isSessionTokenValid(reqs, hh)) {
                return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                        EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                                new ClientFatalEndpointsRequestError(UNAUTHORIZED, "Session token invalid.")));
            } else if (verificationCode == null || verificationCode.equals("")) {
                return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                        EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                                new ClientFatalEndpointsRequestError(INVALID_PARAMETER,
                                        "Update user: Parameter \"userId\" equals -1 and parameter" +
                                                " \"verificationCode\" null or empty.")));
            } else if (!verificationCode.matches("[A-Z0-9]{6,8}$")) {
                return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                        EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                                new ClientFatalEndpointsRequestError(INVALID_PARAMETER,
                                        "Update user: Parameter \"verificationCode\" invalid: \"{}\".",
                                        verificationCode)));
            } else if (updatedUser.getCountryCode() != 0 && updatedUser.getPhoneNumber() != null &&
                    updatedUser.getEmailAddress() != null) {
                return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                        EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                                new ClientFatalEndpointsRequestError(INVALID_PARAMETER,
                                        "Update user: Both a phone number and email address exist for the" +
                                                " user when a verification code was provided.")));
            }

            CommonResponseBody commRespBody;
            if (updatedUser.getCountryCode() != 0 && updatedUser.getPhoneNumber() != null) {
                commRespBody = VerificationEndpoint.executePhoneNumberVerificationCodeQuery(
                        updatedUser.getCountryCode(), updatedUser.getPhoneNumber(), true);
            } else if (updatedUser.getEmailAddress() != null) {
                commRespBody = VerificationEndpoint.executeEmailAddressVerificationCodeQuery(
                        updatedUser.getEmailAddress(), true);
            } else {
                return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                        EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                                new ClientFatalEndpointsRequestError(INVALID_PARAMETER,
                                        "Update user: Neither a phone number nor email address was provided" +
                                                " for the user while a verification code was provided.")));
            }
            if (commRespBody.getEndpointsRequestError() instanceof ServerFatalEndpointsRequestError) {
                return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                        EndpointsResponseProcessor.processAndCreateCommonResponseBodyForServerFatalEndpointsRequestError((
                                ServerFatalEndpointsRequestError) commRespBody.getEndpointsRequestError()));
            } else if (commRespBody.getEndpointsRequestError() != null) {
                return EndpointsResponseProcessor.createResponseFromCommonResponseBody(commRespBody);
            }

            CommonResponseBody.Subresult commRespBodySubr = commRespBody.getSubresult();
            if (commRespBodySubr == null || !verificationCode.equals(commRespBodySubr.getData())) {
                return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                        EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                                new ClientFatalEndpointsRequestError(INVALID_PARAMETER,
                                        "Update user: The provided verification code does not exist or is" +
                                                " incorrect: \"{}\".", verificationCode)));
            }
        }

        PreparedStatement prepStmt = null;

        try (Connection conn = SQLManager.getConnection(false)) {
            if (updatedUser.getName() != null) {

            }
            if (updatedUser.getPassword() != null) {
                boolean doesUserPassExit = userPassword != null && !userPassword.equals("");
                boolean doesVerfCodeExit = verificationCode != null && !verificationCode.equals("");

                if (doesUserPassExit || doesVerfCodeExit) {
                    if (doesUserPassExit) {
                        if (executeLogInAndGetUserQueryByUserId(userId, userPassword, null) == null) {
                            conn.commit();
                            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                                    new CommonResponseBody(new ClientGeneralEndpointsRequestError(INVALID_CREDENTIALS,
                                            "The password is incorrect.")));
                        }
                    }

                    List<UserValidator.Error> valdErrors = UserValidator.validatePassword(updatedUser.getPassword());
                    if (valdErrors.size() > 0) {
                        conn.commit();
                        return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                                EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                                        new ClientFatalEndpointsRequestError(INVALID_PARAMETER,
                                                EndpointsUtils.ResourceValidationUtils
                                                        .createResourceValidationErrorsResponseMessage(
                                                                ENDPOINT_TYPE.USERS, valdErrors))));
                    }

                    String whereClause;
                    if (doesUserPassExit) {
                        whereClause = "id = ?";
                    } else {
                        if (updatedUser.getCountryCode() != 0 && updatedUser.getPhoneNumber() != null) {
                            whereClause = "country_code = ? AND phone_number = ?";
                        } else {
                            whereClause = "email_address = ?";
                        }
                    }

                    prepStmt = conn.prepareStatement("UPDATE " + DbConstants.SQL.USERS.TBL_USER +
                            " SET passw_hash = generate_password_hash(?) WHERE " + whereClause);
                    prepStmt.setString(1, updatedUser.getPassword());
                    if (doesUserPassExit) {
                        prepStmt.setLong(2, userId);
                    } else {
                        if (updatedUser.getCountryCode() != 0 && updatedUser.getPhoneNumber() != null) {
                            prepStmt.setInt(2, updatedUser.getCountryCode());
                            prepStmt.setString(3, updatedUser.getPhoneNumber());
                        } else {
                            prepStmt.setString(2, updatedUser.getEmailAddress());
                        }
                    }
                    prepStmt.executeUpdate();

                }
            }
            if (updatedUser.getCountryCode() != 0 && (verificationCode == null || verificationCode.equals(""))) {

            }
            if (updatedUser.getPhoneNumber() != null && (verificationCode == null || verificationCode.equals(""))) {

            }
            if (updatedUser.getEmailAddress() != null) {

            }
            if (updatedUser.getFirstName() != null) {

            }
            if (updatedUser.getFirstName() != null) {

            }

            conn.commit();
        } catch (SQLException e) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForServerFatalEndpointsRequestError(
                            new ServerFatalEndpointsRequestError(DATABASE_TRANSACTION,
                                    "Failed to execute query in updateUserRequest(): {}", e.toString())));
        } finally {
            SQLManager.closeQuietly(prepStmt);
        }
        return EndpointsResponseProcessor.createResponseWithEmptyBodyAndStatusOk();
    }



    /**
     * Check if a user name exists.
     * @param name Name
     */
    @GET @Path("/names/{name}")
    @Produces("application/json")
    public Response checkIfUserNameExistsRequest(@Context HttpServletRequest reqs, @Context HttpHeaders hh,
                                                 @PathParam("name") String name) {
        if (!EndpointAuthenticationUtils.isSessionTokenValid(reqs, hh)) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(UNAUTHORIZED, "Session token invalid.")));
        }

        CommonResponseBody commRespBody = checkIfUserColumnValueExistsHelper("name", name.trim());
        if (commRespBody.getEndpointsRequestError() instanceof ServerFatalEndpointsRequestError) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor
                            .processAndCreateCommonResponseBodyForServerFatalEndpointsRequestError((
                                    ServerFatalEndpointsRequestError) commRespBody.getEndpointsRequestError()));
        }
        return EndpointsResponseProcessor.createResponseFromCommonResponseBody(commRespBody);
    }

    /**
     * Check if a user phone number exists.
     * @param countryCode Country code of phone number
     * @param phoneNumber Phone number
     */
    @GET @Path("/phone_numbers/{countryCode}/{phoneNumber}")
    @Produces("application/json")
    public Response checkIfUserPhoneNumberExistsRequest(@Context HttpServletRequest reqs, @Context HttpHeaders hh,
                                                        @PathParam("countryCode") int countryCode,
                                                        @PathParam("phoneNumber") String phoneNumber) {
        if (!EndpointAuthenticationUtils.isSessionTokenValid(reqs, hh)) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(UNAUTHORIZED, "Session token invalid.")));
        }

        PreparedStatement prepStmt = null;
        ResultSet reslSet = null;

        try (Connection conn = SQLManager.getConnection(true)) {
            prepStmt = conn.prepareStatement("SELECT id FROM " + DbConstants.SQL.USERS.TBL_USER +
                    " WHERE country_code = ? AND phone_number = ?");
            prepStmt.setInt(1, countryCode);
            prepStmt.setString(2, phoneNumber.trim());
            reslSet = prepStmt.executeQuery();
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(new CommonResponseBody(
                    new String[] { (reslSet.next()) ? "true" : "false" }));
        } catch (SQLException e) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForServerFatalEndpointsRequestError(
                            new ServerFatalEndpointsRequestError(DATABASE_TRANSACTION,
                                    "Failed to execute query in checkIfUserPhoneNumberExistsRequest(): " +
                                            e.toString())));
        } finally {
            SQLManager.closeQuietly(reslSet);
            SQLManager.closeQuietly(prepStmt);
        }
    }

    /**
     * Check if a user email address exists.
     * @param emailAddress Email address
     */
    @GET @Path("/email_addresses/{emailAddress}")
    @Produces("application/json")
    public Response checkIfUserEmailAddressExistsRequest(@Context HttpServletRequest reqs, @Context HttpHeaders hh,
                                                         @PathParam("emailAddress") String emailAddress) {
        if (!EndpointAuthenticationUtils.isSessionTokenValid(reqs, hh)) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(UNAUTHORIZED, "Session token invalid.")));
        }

        CommonResponseBody commRespBody = checkIfUserColumnValueExistsHelper("email_address",
                emailAddress.trim());
        if (commRespBody.getEndpointsRequestError() instanceof ServerFatalEndpointsRequestError) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor
                            .processAndCreateCommonResponseBodyForServerFatalEndpointsRequestError((
                                    ServerFatalEndpointsRequestError) commRespBody.getEndpointsRequestError()));
        }
        return EndpointsResponseProcessor.createResponseFromCommonResponseBody(commRespBody);
    }

    /**
     * Helper to check if the value of a column in the user table exists.
     * @param column Column to check value against.
     * @param value Value to check.
     */
    private static CommonResponseBody checkIfUserColumnValueExistsHelper(String column, String value) {
        PreparedStatement prepStmt = null;
        ResultSet reslSet = null;

        try (Connection conn = SQLManager.getConnection(true)) {
            prepStmt = conn.prepareStatement("SELECT id FROM " + DbConstants.SQL.USERS.TBL_USER + " WHERE " +
                    column + " = ?");
            prepStmt.setString(1, value);
            reslSet = prepStmt.executeQuery();
            return new CommonResponseBody(new String[] { (reslSet.next()) ? "true" : "false" });
        } catch (SQLException e) {
            return new CommonResponseBody(new ServerFatalEndpointsRequestError(DATABASE_TRANSACTION,
                    "Failed to execute query in checkIfUserColumnValueExistsHelper(): {}", e.toString()));
        } finally {
            SQLManager.closeQuietly(reslSet);
            SQLManager.closeQuietly(prepStmt);
        }
    }



    /**
     * Get the type of a user identity entity.
     * @param userIdentityEntity A user phone number, email address or name
     * @return A {@link USER_IDENTITY_ENTITY_TYPE}
     */
    private static USER_IDENTITY_ENTITY_TYPE getUserIdentityEntityType(String userIdentityEntity) {
        if (userIdentityEntity.matches("^[0-9\\s()-+]+$")) {
            return USER_IDENTITY_ENTITY_TYPE.PHONE_NUMBER;
        } else if (userIdentityEntity.contains("@")) {
            return USER_IDENTITY_ENTITY_TYPE.EMAIL_ADDRESS;
        } else {
            return USER_IDENTITY_ENTITY_TYPE.NAME;
        }
    }

}
