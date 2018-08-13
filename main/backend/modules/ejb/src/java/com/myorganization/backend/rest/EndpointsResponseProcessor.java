package com.fencedin.backend.rest;

import com.fencedin.commons.rest.CommonResponseBody;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * The endpoints response processor
 */
public class EndpointsResponseProcessor {
    public static final Logger CLIENT_FATAL_ENDPOINTS_REQUEST_ERROR_LOGGER;
    public static final Logger SERVER_FATAL_ENDPOINTS_REQUEST_ERROR_LOGGER;

    static {
        CLIENT_FATAL_ENDPOINTS_REQUEST_ERROR_LOGGER = LoggerFactory.getLogger(
                "Client fatal endpoints request error");
        SERVER_FATAL_ENDPOINTS_REQUEST_ERROR_LOGGER = LoggerFactory.getLogger(
                "Server fatal endpoints request error");
    }

    /**
     * Get the default response builder.
     */
    private static Response.ResponseBuilder getDefaultResponseBuilder() {
        // TODO: Check if should be moved to static variable
        return Response.ok().type(MediaType.APPLICATION_JSON);
    }

    /**
     * Create a {@link javax.ws.rs.core.Response} with an empty body and status OK.
     */
    public static Response createResponseWithEmptyBodyAndStatusOk() {
        return getDefaultResponseBuilder().build();
    }

    /**
     * Create a {@link javax.ws.rs.core.Response} with a JSON body and status OK.
     * @param json The JSON to use as the response body; must be an instance of {@link JSONObject} or {@link JSONArray}
     * @throws IllegalArgumentException If json is not an instance of {@link JSONObject} or {@link JSONArray}
     */
    public static Response createResponseWithJsonBodyAndStatusOk(Object json) throws IllegalArgumentException {
        if (!(json instanceof JSONObject) && !(json instanceof JSONArray)) {
            throw new IllegalArgumentException("The JSON object is not an instance of JSONObject or JSONArray.");
        }

        Response.ResponseBuilder respBldr = getDefaultResponseBuilder();
        respBldr.entity(json.toString());
        return respBldr.build();
    }

    /**
     * Create a {@link javax.ws.rs.core.Response} from a {@link CommonResponseBody}.
     * @param commonResponseBody The CommonResponseBody to create a Response from
     */
    public static Response createResponseFromCommonResponseBody(CommonResponseBody commonResponseBody) {
        Response.ResponseBuilder respBldr = getDefaultResponseBuilder();
        respBldr.entity(commonResponseBody.toJsonString());

        // Note: Status BAD_REQUEST is returned by the server when it receives an invalid resource JSON and should
        // therefore not be used.
        if (commonResponseBody.getEndpointsRequestError() instanceof ServerFatalEndpointsRequestError) {
            respBldr.status(Response.Status.INTERNAL_SERVER_ERROR);
        }
        return respBldr.build();
    }

    /**
     * Process a server fatal endpoints request error and a create CommonResponseBody for it.
     * @param serverFatalEndpointsRequestError The server fatal endpoints request error
     * @return A CommonResponseBody created with the server fatal endpoints request error
     */
    public static CommonResponseBody processAndCreateCommonResponseBodyForServerFatalEndpointsRequestError(
            ServerFatalEndpointsRequestError serverFatalEndpointsRequestError) {
        // TODO: Create log file appenders
        SERVER_FATAL_ENDPOINTS_REQUEST_ERROR_LOGGER.error("{}: {}", serverFatalEndpointsRequestError.getType(),
                serverFatalEndpointsRequestError.getMessage());
        return new CommonResponseBody(serverFatalEndpointsRequestError);
    }

    /**
     * Process a client fatal endpoints request error and a create CommonResponseBody for it.
     * @param reqs The HTTP servlet request
     * @param clientFatalEndpointsRequestError The client fatal endpoints request error
     * @return A CommonResponseBody created with the client fatal endpoints request error
     */
    public static CommonResponseBody processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(
            HttpServletRequest reqs, ClientFatalEndpointsRequestError clientFatalEndpointsRequestError) {
        // TODO: Create log file appenders
        CLIENT_FATAL_ENDPOINTS_REQUEST_ERROR_LOGGER.warn("{}: {} (IP: {})",
                clientFatalEndpointsRequestError.getType(), clientFatalEndpointsRequestError.getMessage(),
                reqs.getRemoteAddr());

        // TODO: Block the client IP address
        return new CommonResponseBody(clientFatalEndpointsRequestError);
    }

}
