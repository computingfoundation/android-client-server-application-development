package com.fencedin.backend.rest;

import com.fencedin.backend.authentication.EndpointAuthenticationUtils;
import com.fencedin.backend.database.DbConstants;
import com.fencedin.backend.database.SQLManager;
import com.fencedin.backend.util.MessagingUtils;
import com.fencedin.commons.base.DateUtils;
import com.fencedin.commons.base.SecureRandomUtils;
import com.fencedin.commons.base.Times;
import com.fencedin.commons.rest.ClientGeneralEndpointsRequestError;
import com.fencedin.commons.rest.CommonResponseBody;
import com.fencedin.commons.validation.UserValidator;
import com.nexmo.client.NexmoClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import static com.fencedin.backend.base.BackendConstants.Dev.DISABLE_USER_MESSAGING;
import static com.fencedin.backend.base.BackendConstants.ORGANIZATION;
import static com.fencedin.backend.rest.ClientFatalEndpointsRequestError.TYPE.*;
import static com.fencedin.backend.rest.ServerFatalEndpointsRequestError.TYPE.DATABASE_TRANSACTION;
import static com.fencedin.backend.rest.ServerFatalEndpointsRequestError.TYPE.ILLEGAL_STATE;
import static com.fencedin.commons.rest.ClientGeneralEndpointsRequestError.TYPE.*;

/**
 * The verification endpoint
 */
@Path("/verification")
public class VerificationEndpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(VerificationEndpoint.class);
    /**
     * The time until a verification code expires.
     */
    private static final long VERIFICATION_CODE_EXPIRATION_TIME = Times.FIFTEEN_MINUTES;

    /**
     * The period of which consecutive verification code requests are counted. (Applies to both the phone number and
     * email address contact entities. Also applies to both per contact entity and per IP address requests).
     */
    private static final long CONSECUTIVE_VERIFICATION_CODE_REQUEST_COUNT_PERIOD = Times.THREE_HOURS;
    /**
     * The maximum number of verification code requests allowed during the consecutive verification code request count
     * period. (Applies to both the phone number and email address contact entities. Also applies to both per contact
     * entity and per IP address requests).
     */
    private static final long MAXIMUM_CONSECUTIVE_VERIFICATION_CODE_REQUESTS = 6;

    /**
     * The minimum interval between verification code requests.
     */
    private static final long VERIFICATION_CODE_REQUEST_MINIMUM_INTERVAL = Times.THIRTY_SECONDS;

    // TODO: Possibly merge these request and verify methods (see method "resetVerifiedUserPasswordRequest" in
    //       UsersEndpoint for reference).

    /**
     * Request a phone number verification code.
     */
    @GET @Path("/phone_number_and_email_address_codes/{countryCode}/{phoneNumber}")
    @Produces("application/json")
    public Response requestPhoneNumberVerificationCodeRequest(@Context HttpServletRequest reqs, @Context HttpHeaders hh,
                              @PathParam("countryCode") int countryCode,
                              @PathParam("phoneNumber") String phoneNumber,
                              @QueryParam("reqHighLvlSecVerCode") boolean requestHighLevelSecurityVerificationCode) {
        if (!EndpointAuthenticationUtils.isSessionTokenValid(reqs, hh)) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(UNAUTHORIZED, "Session token invalid.")));
        }

        if (!EndpointsUtils.ResourceValidationUtils.ensureNecessaryConfigurationsAreInitialized()) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForServerFatalEndpointsRequestError(
                            new ServerFatalEndpointsRequestError(ILLEGAL_STATE,
                                    "Failed to initialize configurations.")));
        }

        List<UserValidator.Error> valdErrors = UserValidator.validateCountryCode(countryCode);
        valdErrors.addAll(UserValidator.validatePhoneNumber(phoneNumber));
        if (valdErrors.size() > 0) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(INVALID_RESOURCE,
                                    EndpointsUtils.ResourceValidationUtils
                                            .createResourceValidationErrorsResponseMessage(ENDPOINT_TYPE.VERIFICATION,
                                                    valdErrors))));
        }

        String verfCode = (requestHighLevelSecurityVerificationCode) ?
                SecureRandomUtils.generateNSecureRandomUppercaseLetters(8) :
                SecureRandomUtils.generateNSecureRandomDigits(6);
        PreparedStatement prepStmt = null;
        ResultSet reslSet = null;

        try (Connection conn = SQLManager.getConnection(false)) {
            long reqrWaitTime = checkIfMaxConsVerfCodeRequestLimitReachedAgainstIpAddrAndGetRequiredWaitTime(
                    UsersEndpoint.USER_IDENTITY_ENTITY_TYPE.PHONE_NUMBER, conn, reqs.getRemoteAddr());
            if (reqrWaitTime != 0) {
                return EndpointsResponseProcessor.createResponseFromCommonResponseBody(new CommonResponseBody(
                        new ClientGeneralEndpointsRequestError(LIMIT_REACHED,
                                "The phone number verification code request limit has been reached. Please" +
                                        " wait {} to request a new phone number verification code.",
                                DateUtils.formatMillisecondsToTimeWithUnit(reqrWaitTime))));
            }

            reqrWaitTime = checkIfMaxConsVerfCodeRequestLimitReachedAgainstContactEntyAndGetRequiredWaitTime(conn,
                    countryCode, phoneNumber, null, requestHighLevelSecurityVerificationCode);
            if (reqrWaitTime != 0) {
                return EndpointsResponseProcessor.createResponseFromCommonResponseBody(new CommonResponseBody(
                        new ClientGeneralEndpointsRequestError(LIMIT_REACHED,
                                "The verification code request limit for this phone number has been reached."
                                        + " Please wait {} to request a new verification code for this phone number.",
                                DateUtils.formatMillisecondsToTimeWithUnit(reqrWaitTime))));
            }

            prepStmt = conn.prepareStatement("SELECT phone_number_verification_code_requested_at FROM " +
                    DbConstants.SQL.VERIFICATION.TBL_VERIFICATION_CODE_REQUESTS + " WHERE ip_address = ?::inet");
            prepStmt.setString(1, reqs.getRemoteAddr());
            reslSet = prepStmt.executeQuery();
            if (reslSet.next()) {
                Date phoneNumbVerfCodeReqsAt = reslSet.getTimestamp("phone_number_verification_code_requested_at",
                        DateUtils.getUTCCalendar());

                if (phoneNumbVerfCodeReqsAt != null) {
                    long currTimeVerfCodeReqsAtTimeDelta = System.currentTimeMillis() -
                            phoneNumbVerfCodeReqsAt.getTime();
                    if (currTimeVerfCodeReqsAtTimeDelta < VERIFICATION_CODE_REQUEST_MINIMUM_INTERVAL) {
                        return EndpointsResponseProcessor.createResponseFromCommonResponseBody(new CommonResponseBody(
                                new ClientGeneralEndpointsRequestError(MINIMUM_INTERVAL_NOT_ELAPSED,
                                        "Please wait {} to request a new verification code.",
                                        DateUtils.formatMillisecondsToTimeWithUnit(
                                                VERIFICATION_CODE_REQUEST_MINIMUM_INTERVAL -
                                                        currTimeVerfCodeReqsAtTimeDelta))));
                    }
                }
            }

            prepStmt = conn.prepareStatement("INSERT INTO " + ((requestHighLevelSecurityVerificationCode) ?
                    DbConstants.SQL.VERIFICATION.TBL_HIGH_LEVEL_SECURITY_PHONE_NUMBER_VERIFICATION_CODE :
                    DbConstants.SQL.VERIFICATION.TBL_LOW_LEVEL_SECURITY_PHONE_NUMBER_VERIFICATION_CODE) +
                    " (country_code, phone_number, verification_code) VALUES (?, ?, ?) ON CONFLICT" +
                    " (country_code, phone_number) DO UPDATE SET verification_code = EXCLUDED.verification_code");
            prepStmt.setInt(1, countryCode);
            prepStmt.setString(2, phoneNumber);
            prepStmt.setString(3, verfCode);
            prepStmt.executeUpdate();

            prepStmt = conn.prepareStatement("UPDATE " + DbConstants.SQL.VERIFICATION.TBL_VERIFICATION_CODE_REQUESTS
                    + " SET phone_number_verification_code_requested_at = '" + DateUtils.toIso8601(new Date()) +
                    "' WHERE ip_address = ?::inet");
            prepStmt.setString(1, reqs.getRemoteAddr());
            prepStmt.executeUpdate();

            conn.commit();
        } catch (SQLException e) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForServerFatalEndpointsRequestError(
                            new ServerFatalEndpointsRequestError(DATABASE_TRANSACTION,
                                    "Failed to execute query in requestPhoneNumberVerificationCodeRequest():" +
                                            " {}", e.toString())));
        } finally {
            SQLManager.closeQuietly(reslSet);
            SQLManager.closeQuietly(prepStmt);
        }

        if (DISABLE_USER_MESSAGING) {
            LOGGER.info("Verification code requested for phone number {}; verification code: {}.", countryCode +
                    phoneNumber, verfCode);
        } else {
            try {
                MessagingUtils.sendSMSMessage(countryCode + phoneNumber, "Your " + ORGANIZATION +
                        " verification code is " + verfCode + ".");
            } catch (IOException | NexmoClientException e) {
                LOGGER.error("Failed to send SMS message in requestPhoneNumberVerificationCodeRequest(): {}",
                        e.toString());
            }
        }
        return EndpointsResponseProcessor.createResponseWithEmptyBodyAndStatusOk();
    }

    @GET @Path("/phone_number_and_email_address_codes/{emailAddress}")
    @Produces("application/json")
    public Response requestEmailAddressVerificationCodeRequest(@Context HttpServletRequest reqs,
                               @Context HttpHeaders hh,
                               @PathParam("emailAddress") String emailAddress,
                               @QueryParam("reqHighLvlSecVerCode") boolean requestHighLevelSecurityVerificationCode) {
        if (!EndpointAuthenticationUtils.isSessionTokenValid(reqs, hh)) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(UNAUTHORIZED, "Session token invalid.")));
        }

        if (!EndpointsUtils.ResourceValidationUtils.ensureNecessaryConfigurationsAreInitialized()) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForServerFatalEndpointsRequestError(
                            new ServerFatalEndpointsRequestError(ILLEGAL_STATE,
                                    "Failed to initialize configurations.")));
        }

        List<UserValidator.Error> valdErrors = UserValidator.validateEmailAddress(emailAddress);
        if (valdErrors.size() > 0) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(INVALID_RESOURCE,
                                    EndpointsUtils.ResourceValidationUtils
                                            .createResourceValidationErrorsResponseMessage(ENDPOINT_TYPE.VERIFICATION,
                                                    valdErrors))));
        }

        String verfCode = (requestHighLevelSecurityVerificationCode) ?
                SecureRandomUtils.generateNSecureRandomUppercaseLetters(8) :
                SecureRandomUtils.generateNSecureRandomDigits(6);
        PreparedStatement prepStmt = null;
        ResultSet reslSet = null;

        try (Connection conn = SQLManager.getConnection(false)) {
            long reqrWaitTime = checkIfMaxConsVerfCodeRequestLimitReachedAgainstIpAddrAndGetRequiredWaitTime(
                    UsersEndpoint.USER_IDENTITY_ENTITY_TYPE.EMAIL_ADDRESS, conn, reqs.getRemoteAddr());
            if (reqrWaitTime != 0) {
                return EndpointsResponseProcessor.createResponseFromCommonResponseBody(new CommonResponseBody(
                        new ClientGeneralEndpointsRequestError(LIMIT_REACHED,
                                "The email address verification code request limit has been reached. Please" +
                                        " wait {} to request a new email address verification code.",
                                DateUtils.formatMillisecondsToTimeWithUnit(reqrWaitTime))));
            }

            reqrWaitTime = checkIfMaxConsVerfCodeRequestLimitReachedAgainstContactEntyAndGetRequiredWaitTime(conn,
                    0, null, emailAddress, requestHighLevelSecurityVerificationCode);
            if (reqrWaitTime != 0) {
                return EndpointsResponseProcessor.createResponseFromCommonResponseBody(new CommonResponseBody(
                        new ClientGeneralEndpointsRequestError(LIMIT_REACHED,
                                "The verification code request limit for this email address has been reached."
                                        + " Please wait {} to request a new verification code for this email address.",
                                DateUtils.formatMillisecondsToTimeWithUnit(reqrWaitTime))));
            }

            prepStmt = conn.prepareStatement("SELECT email_address_verification_code_requested_at FROM " +
                    DbConstants.SQL.VERIFICATION.TBL_VERIFICATION_CODE_REQUESTS + " WHERE ip_address = ?::inet");
            prepStmt.setString(1, reqs.getRemoteAddr());
            reslSet = prepStmt.executeQuery();
            if (reslSet.next()) {
                Date emailAddrVerfCodeReqsAt = reslSet.getTimestamp("email_address_verification_code_requested_at",
                        DateUtils.getUTCCalendar());

                if (emailAddrVerfCodeReqsAt != null) {
                    long currTimeVerfCodeReqsAtTimeDelta = System.currentTimeMillis() -
                            emailAddrVerfCodeReqsAt.getTime();
                    if (currTimeVerfCodeReqsAtTimeDelta < VERIFICATION_CODE_REQUEST_MINIMUM_INTERVAL) {
                        return EndpointsResponseProcessor.createResponseFromCommonResponseBody(new CommonResponseBody(
                                new ClientGeneralEndpointsRequestError(MINIMUM_INTERVAL_NOT_ELAPSED,
                                        "Please wait {} to request a new verification code.",
                                        DateUtils.formatMillisecondsToTimeWithUnit(
                                                VERIFICATION_CODE_REQUEST_MINIMUM_INTERVAL -
                                                        currTimeVerfCodeReqsAtTimeDelta))));
                    }
                }
            }

            prepStmt = conn.prepareStatement("INSERT INTO " + ((requestHighLevelSecurityVerificationCode) ?
                    DbConstants.SQL.VERIFICATION.TBL_HIGH_LEVEL_SECURITY_EMAIL_ADDRESS_VERIFICATION_CODE :
                    DbConstants.SQL.VERIFICATION.TBL_LOW_LEVEL_SECURITY_EMAIL_ADDRESS_VERIFICATION_CODE) +
                    " (email_address, verification_code) VALUES (?, ?) ON CONFLICT (email_address) DO UPDATE SET" +
                    " verification_code = EXCLUDED.verification_code");
            prepStmt.setString(1, emailAddress);
            prepStmt.setString(2, verfCode);
            prepStmt.executeUpdate();

            prepStmt = conn.prepareStatement("UPDATE " + DbConstants.SQL.VERIFICATION.TBL_VERIFICATION_CODE_REQUESTS
                    + " SET email_address_verification_code_requested_at = '" + DateUtils.toIso8601(new Date()) +
                    "' WHERE ip_address = ?::inet");
            prepStmt.setString(1, reqs.getRemoteAddr());
            prepStmt.executeUpdate();

            conn.commit();
        } catch (SQLException e) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForServerFatalEndpointsRequestError(
                            new ServerFatalEndpointsRequestError(DATABASE_TRANSACTION,
                                    "Failed to execute query in" +
                                            " requestEmailAddressVerificationCodeRequest(): {}", e.toString())));
        } finally {
            SQLManager.closeQuietly(reslSet);
            SQLManager.closeQuietly(prepStmt);
        }

        if (DISABLE_USER_MESSAGING) {
            LOGGER.info("Verification code requested for email address {}; verification code: {}.", emailAddress,
                    verfCode);
        } else {
            try {
                String orgNameHeaderColor = "#874d48";
                MessagingUtils.sendEmail(emailAddress, "Verification Code",
                        "<h1 style='text-align: center; color: " + orgNameHeaderColor + ";'>" +
                                ORGANIZATION +
                                "</h1><p>&nbsp;</p><p style='text-align: center; font-size: 16px;'>Your verification" +
                                " code is " + verfCode + ".</p><p>&nbsp;</p>", true);
            } catch (MessagingException e) {
                LOGGER.error("Failed to send email in requestEmailAddressVerificationCodeRequest(): {}",
                        e.toString());
            }
        }
        return EndpointsResponseProcessor.createResponseWithEmptyBodyAndStatusOk();
    }

    /**
     * Check if the maximum consecutive verification code request limit has been reached against a contact entity.
     * @param connection The SQL connection to use for the SQL queries
     * @param countryCode The country code the verification code is being requested for
     * @param phoneNumber The phone number the verification code is being requested for
     * @param emailAddress The email address the verification code is being requested for
     * @return The time until the next verification code of the respective contact entity can be requested if the limit
     *         has been reached or 0 if it has not.
     * @throws SQLException If a database transaction error occurred
     */
    private static long checkIfMaxConsVerfCodeRequestLimitReachedAgainstContactEntyAndGetRequiredWaitTime(
            Connection connection, int countryCode, String phoneNumber, String emailAddress,
            boolean isHighLevelSecurityVerificationCode) throws SQLException {
        boolean isPhoneNumbVerfCodeReqs = countryCode != 0 && phoneNumber != null;
        String verfCodeTable;
        if (isHighLevelSecurityVerificationCode) {
            verfCodeTable = (isPhoneNumbVerfCodeReqs) ?
                    DbConstants.SQL.VERIFICATION.TBL_HIGH_LEVEL_SECURITY_PHONE_NUMBER_VERIFICATION_CODE :
                    DbConstants.SQL.VERIFICATION.TBL_HIGH_LEVEL_SECURITY_EMAIL_ADDRESS_VERIFICATION_CODE;
        } else {
            verfCodeTable = (isPhoneNumbVerfCodeReqs) ?
                    DbConstants.SQL.VERIFICATION.TBL_LOW_LEVEL_SECURITY_PHONE_NUMBER_VERIFICATION_CODE :
                    DbConstants.SQL.VERIFICATION.TBL_LOW_LEVEL_SECURITY_EMAIL_ADDRESS_VERIFICATION_CODE;
        }
        String whereClause = (isPhoneNumbVerfCodeReqs) ? "country_code = ? AND phone_number = ?" :
                "email_address = ?";

        PreparedStatement prepStmt = connection.prepareStatement("SELECT * FROM " + verfCodeTable +
                " WHERE " + whereClause);
        if (isPhoneNumbVerfCodeReqs) {
            prepStmt.setInt(1, countryCode);
            prepStmt.setString(2, phoneNumber);
        } else {
            prepStmt.setString(1, emailAddress);
        }
        ResultSet reslSet = prepStmt.executeQuery();

        if (reslSet.next()) {
            Date consVerfCodeReqsCountPerdStart = reslSet.getTimestamp(
                    "consecutive_verification_code_request_count_period_start", DateUtils.getUTCCalendar());
            long countPerdStartTimeCurrTimeDelta = (consVerfCodeReqsCountPerdStart != null) ?
                    System.currentTimeMillis() - consVerfCodeReqsCountPerdStart.getTime() : 0;

            if (consVerfCodeReqsCountPerdStart != null && countPerdStartTimeCurrTimeDelta <
                    CONSECUTIVE_VERIFICATION_CODE_REQUEST_COUNT_PERIOD) {
                if (reslSet.getInt("consecutive_verification_code_request_count") >=
                        MAXIMUM_CONSECUTIVE_VERIFICATION_CODE_REQUESTS) {
                    return CONSECUTIVE_VERIFICATION_CODE_REQUEST_COUNT_PERIOD - countPerdStartTimeCurrTimeDelta;
                } else {
                    prepStmt = connection.prepareStatement("UPDATE " + verfCodeTable +
                            " SET consecutive_verification_code_request_count =" +
                            " consecutive_verification_code_request_count + 1 WHERE " + whereClause);
                    if (isPhoneNumbVerfCodeReqs) {
                        prepStmt.setInt(1, countryCode);
                        prepStmt.setString(2, phoneNumber);
                    } else {
                        prepStmt.setString(1, emailAddress);
                    }
                    prepStmt.executeUpdate();
                }
            } else {
                prepStmt = connection.prepareStatement("UPDATE " + verfCodeTable +
                        " SET consecutive_verification_code_request_count = 1," +
                        " consecutive_verification_code_request_count_period_start = '" + DateUtils.toIso8601(
                                new Date()) + "' WHERE " + whereClause);
                if (isPhoneNumbVerfCodeReqs) {
                    prepStmt.setInt(1, countryCode);
                    prepStmt.setString(2, phoneNumber);
                } else {
                    prepStmt.setString(1, emailAddress);
                }
                prepStmt.executeUpdate();
            }
        }
        return 0;
    }

    /**
     * Check if the maximum consecutive verification code request limit has been reached against an IP address.
     * @param userIdentityEntityType The type of the user identity entity the verification code is being requested for
     * @param connection The SQL connection to use for the SQL queries
     * @param ipAddress The IP address to check against
     * @return The time until the next verification code of the respective contact entity can be requested if the limit
     *         has been reached or 0 if it has not.
     * @throws SQLException If a database transaction error occurred
     */
    private static long checkIfMaxConsVerfCodeRequestLimitReachedAgainstIpAddrAndGetRequiredWaitTime(
            UsersEndpoint.USER_IDENTITY_ENTITY_TYPE userIdentityEntityType, Connection connection, String ipAddress)
            throws SQLException {
        PreparedStatement prepStmt = connection.prepareStatement("SELECT * FROM " +
                DbConstants.SQL.VERIFICATION.TBL_VERIFICATION_CODE_REQUESTS + " WHERE ip_address = ?::inet");
        prepStmt.setString(1, ipAddress);
        ResultSet reslSet = prepStmt.executeQuery();

        // TODO: Current: Splitting up min interval check for phone number and email address

        if (reslSet.next()) {
            Date consContEnttVerfCodeReqsCountPerdStart = reslSet.getTimestamp("consecutive_" +
                    userIdentityEntityType + "_verf_code_request_count_period_start", DateUtils.getUTCCalendar());
            long currTimePeriodStartTimeDelta = (consContEnttVerfCodeReqsCountPerdStart != null) ?
                    System.currentTimeMillis() - consContEnttVerfCodeReqsCountPerdStart.getTime() : 0;

            if (consContEnttVerfCodeReqsCountPerdStart != null && currTimePeriodStartTimeDelta <
                    CONSECUTIVE_VERIFICATION_CODE_REQUEST_COUNT_PERIOD) {
                if (reslSet.getInt("consecutive_" + userIdentityEntityType + "_verification_code_request_count") >=
                        MAXIMUM_CONSECUTIVE_VERIFICATION_CODE_REQUESTS) {
                    return CONSECUTIVE_VERIFICATION_CODE_REQUEST_COUNT_PERIOD - currTimePeriodStartTimeDelta;
                } else {
                    prepStmt = connection.prepareStatement("UPDATE " +
                            DbConstants.SQL.VERIFICATION.TBL_VERIFICATION_CODE_REQUESTS + " SET consecutive_" +
                            userIdentityEntityType + "_verification_code_request_count = consecutive_" +
                            userIdentityEntityType + "_verification_code_request_count + 1 WHERE ip_address = ?::inet");
                    prepStmt.setString(1, ipAddress);
                    prepStmt.executeUpdate();
                }
            } else {
                prepStmt = connection.prepareStatement("UPDATE " +
                        DbConstants.SQL.VERIFICATION.TBL_VERIFICATION_CODE_REQUESTS + " SET consecutive_" +
                        userIdentityEntityType + "_verification_code_request_count = 1, consecutive_" +
                        userIdentityEntityType + "_verf_code_request_count_period_start = '" + DateUtils.toIso8601(
                                new Date()) + "' WHERE ip_address = ?::inet");
                prepStmt.setString(1, ipAddress);
                prepStmt.executeUpdate();
            }
        } else {
            prepStmt = connection.prepareStatement("INSERT INTO " +
                    DbConstants.SQL.VERIFICATION.TBL_VERIFICATION_CODE_REQUESTS + " (ip_address, consecutive_" +
                    userIdentityEntityType + "_verification_code_request_count, consecutive_" + userIdentityEntityType +
                    "_verf_code_request_count_period_start) VALUES (?::inet, 1, '" + DateUtils.toIso8601(new Date()) +
                    "')");
            prepStmt.setString(1, ipAddress);
            prepStmt.executeUpdate();
        }
        return 0;
    }

    /**
     * Verify a phone number verification code.
     */
    @POST @Path("/phone_number_and_email_address_codes/{countryCode}/{phoneNumber}/{verificationCode}")
    @Produces("application/json")
    public Response verifyPhoneNumberVerificationCodeRequest(@Context HttpServletRequest reqs, @Context HttpHeaders hh,
                                     @PathParam("countryCode") int countryCode,
                                     @PathParam("phoneNumber") String phoneNumber,
                                     @PathParam("verificationCode") String verificationCode,
                                     @QueryParam("isHighLvlSecVerCode") boolean isHighLevelSecurityVerificationCode) {
        if (!EndpointAuthenticationUtils.isSessionTokenValid(reqs, hh)) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(UNAUTHORIZED, "Session token invalid.")));
        } else if (verificationCode == null || verificationCode.equals("")) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(INVALID_PARAMETER,
                                    "Verify phone number verification code: Parameter \"verificationCode\"" +
                                            " null or empty.")));
        } else if (!verificationCode.matches("[A-Z0-9]{6,8}$")) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(INVALID_PARAMETER,
                                    "Verify phone number verification code: Parameter \"verificationCode\"" +
                                            " invalid: \"{}\".", verificationCode)));
        }

        // if (!EndpointsUtils.ResourceValidationUtils.ensureNecessaryConfigurationsAreInitialized()) {
        //     return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
        //             EndpointsResponseProcessor.processAndCreateCommonResponseBodyForServerFatalEndpointsRequestError(
        //                     new ServerFatalEndpointsRequestError(ILLEGAL_STATE,
        //                             "Failed to initialize configurations.")));
        // }

        List<UserValidator.Error> valdErrors = UserValidator.validateCountryCode(countryCode);
        valdErrors.addAll(UserValidator.validatePhoneNumber(phoneNumber));
        if (valdErrors.size() > 0) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(INVALID_PARAMETER,
                                    EndpointsUtils.ResourceValidationUtils
                                            .createResourceValidationErrorsResponseMessage(ENDPOINT_TYPE.VERIFICATION,
                                                    valdErrors))));
        }

        CommonResponseBody commRespBody = executePhoneNumberVerificationCodeQuery(countryCode, phoneNumber,
                isHighLevelSecurityVerificationCode);
        if (commRespBody.getEndpointsRequestError() instanceof ServerFatalEndpointsRequestError) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForServerFatalEndpointsRequestError((
                            ServerFatalEndpointsRequestError) commRespBody.getEndpointsRequestError()));
        } else if (commRespBody.getEndpointsRequestError() != null) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(commRespBody);
        }

        CommonResponseBody.Subresult commRespBodySubr = commRespBody.getSubresult();
        return EndpointsResponseProcessor.createResponseFromCommonResponseBody(new CommonResponseBody(new String[] {
                String.valueOf(commRespBodySubr != null && verificationCode.equals(commRespBodySubr.getData())) }));
    }

    /**
     * Verify an email address verification code.
     */
    @POST @Path("/phone_number_and_email_address_codes/{emailAddress}/{verificationCode}")
    @Produces("application/json")
    public Response verifyEmailAddressVerificationCodeRequest(@Context HttpServletRequest reqs, @Context HttpHeaders hh,
                                      @PathParam("emailAddress") String emailAddress,
                                      @PathParam("verificationCode") String verificationCode,
                                      @QueryParam("isHighLvlSecVerCode") boolean isHighLevelSecurityVerificationCode) {
        if (!EndpointAuthenticationUtils.isSessionTokenValid(reqs, hh)) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(UNAUTHORIZED, "Session token invalid.")));
        } else if (verificationCode == null || verificationCode.equals("")) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(INVALID_PARAMETER,
                                    "Verify email address verification code: Parameter \"verificationCode\"" +
                                            " null or empty.")));
        } else if (!verificationCode.matches("[A-Z0-9]{6,8}$")) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(INVALID_PARAMETER,
                                    "Verify email address verification code: Parameter \"verificationCode\"" +
                                            " invalid: \"{}\".", verificationCode)));
        }

        //if (!EndpointsUtils.ResourceValidationUtils.ensureNecessaryConfigurationsAreInitialized()) {
        //    return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
        //            EndpointsResponseProcessor.processAndCreateCommonResponseBodyForServerFatalEndpointsRequestError(
        //                    new ServerFatalEndpointsRequestError(ILLEGAL_STATE,
        //                            "Failed to initialize configurations.")));
        //}

        List<UserValidator.Error> valdErrors = UserValidator.validateEmailAddress(emailAddress);
        if (valdErrors.size() > 0) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(INVALID_PARAMETER,
                                    EndpointsUtils.ResourceValidationUtils
                                            .createResourceValidationErrorsResponseMessage(ENDPOINT_TYPE.VERIFICATION,
                                                    valdErrors))));
        }

        CommonResponseBody commRespBody = executeEmailAddressVerificationCodeQuery(emailAddress,
                isHighLevelSecurityVerificationCode);
        if (commRespBody.getEndpointsRequestError() instanceof ServerFatalEndpointsRequestError) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForServerFatalEndpointsRequestError((
                            ServerFatalEndpointsRequestError) commRespBody.getEndpointsRequestError()));
        } else if (commRespBody.getEndpointsRequestError() != null) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(commRespBody);
        }

        CommonResponseBody.Subresult commRespBodySubr = commRespBody.getSubresult();
        return EndpointsResponseProcessor.createResponseFromCommonResponseBody(new CommonResponseBody(new String[] {
                String.valueOf(commRespBodySubr != null && verificationCode.equals(commRespBodySubr.getData())) }));
    }

    /**
     * Execute a phone number verification code query.
     * @param countryCode The country code of the verification code to query
     * @param phoneNumber The phone number of the verification code to query
     * @return The queried verification code
     */
    public static CommonResponseBody executePhoneNumberVerificationCodeQuery(
            int countryCode, String phoneNumber, boolean isHighLevelSecurityVerificationCode) {
        PreparedStatement prepStmt = null;
        ResultSet reslSet = null;

        try (Connection conn = SQLManager.getConnection(false)) {
            prepStmt = conn.prepareStatement("SELECT * FROM " + ((isHighLevelSecurityVerificationCode) ?
                    DbConstants.SQL.VERIFICATION.TBL_HIGH_LEVEL_SECURITY_PHONE_NUMBER_VERIFICATION_CODE :
                    DbConstants.SQL.VERIFICATION.TBL_LOW_LEVEL_SECURITY_PHONE_NUMBER_VERIFICATION_CODE) +
                    " WHERE country_code = ? AND phone_number = ?");

            prepStmt.setInt(1, countryCode);
            prepStmt.setString(2, phoneNumber);
            reslSet = prepStmt.executeQuery();

            if (reslSet.next()) {
                if (System.currentTimeMillis() - reslSet.getTimestamp("created_at",
                        DateUtils.getUTCCalendar()).getTime() > VERIFICATION_CODE_EXPIRATION_TIME) {
                    conn.commit();
                    return new CommonResponseBody(new ClientGeneralEndpointsRequestError(EXPIRED,
                            "The verification code for phone number {}{} has expired.", countryCode,
                            phoneNumber));
                }

                conn.commit();
                return new CommonResponseBody(new CommonResponseBody.Subresult(reslSet.getString(
                        "verification_code")));
            } else {
                conn.commit();
                return new CommonResponseBody();
            }
        } catch (SQLException e) {
            return new CommonResponseBody(new ServerFatalEndpointsRequestError(DATABASE_TRANSACTION,
                    "Failed to execute query in executePhoneNumberVerificationCodeQuery(): {}",
                    e.toString()));
        } finally {
            SQLManager.closeQuietly(reslSet);
            SQLManager.closeQuietly(prepStmt);
        }
    }

    /**
     * Execute an email address verification code query.
     * @param emailAddress The email address of the verification code to query
     * @return The queried verification code
     */
    public static CommonResponseBody executeEmailAddressVerificationCodeQuery(
            String emailAddress, boolean isHighLevelSecurityVerificationCode) {
        PreparedStatement prepStmt = null;
        ResultSet reslSet = null;

        try (Connection conn = SQLManager.getConnection(false)) {
            prepStmt = conn.prepareStatement("SELECT * FROM " + ((isHighLevelSecurityVerificationCode) ?
                    DbConstants.SQL.VERIFICATION.TBL_HIGH_LEVEL_SECURITY_EMAIL_ADDRESS_VERIFICATION_CODE :
                    DbConstants.SQL.VERIFICATION.TBL_LOW_LEVEL_SECURITY_EMAIL_ADDRESS_VERIFICATION_CODE) +
                    " WHERE email_address = ?");

            prepStmt.setString(1, emailAddress);
            reslSet = prepStmt.executeQuery();

            if (reslSet.next()) {
                if (System.currentTimeMillis() - reslSet.getTimestamp("created_at",
                        DateUtils.getUTCCalendar()).getTime() > VERIFICATION_CODE_EXPIRATION_TIME) {
                    conn.commit();
                    return new CommonResponseBody(new ClientGeneralEndpointsRequestError(EXPIRED,
                            "The verification code for email address {} has expired.", emailAddress));
                }

                conn.commit();
                return new CommonResponseBody(new CommonResponseBody.Subresult(reslSet.getString(
                        "verification_code")));
            } else {
                conn.commit();
                return new CommonResponseBody();
            }
        } catch (SQLException e) {
            return new CommonResponseBody(new ServerFatalEndpointsRequestError(DATABASE_TRANSACTION,
                    "Failed to execute query in executeEmailAddressVerificationCodeQuery(): {}",
                    e.toString()));
        } finally {
            SQLManager.closeQuietly(reslSet);
            SQLManager.closeQuietly(prepStmt);
        }
    }

}
