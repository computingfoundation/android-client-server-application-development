package com.organization.backend.rest;

import com.organization.backend.authentication.EndpointAuthenticationUtils;
import com.organization.backend.authentication.SessionAuthentication;
import com.organization.backend.database.SQLManager;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.SQLException;

import static com.organization.backend.rest.ClientFatalError.TYPE.INVALID_PARAMETER;
import static com.organization.backend.rest.ClientFatalError.TYPE.UNAUTHORIZED;
import static com.organization.backend.rest.ServerFatalError.TYPE.DATABASE_TRANSACTION;

/**
 * Starting point endpoint for the mobile application.
 */
@Path("/base")
public class BaseEndpoint {
    private static final Logger LOG = LoggerFactory.getLogger(BaseEndpoint.class);

    /**
     * Creates the initial session data. This is the very first request that must be made before proceeding
     * with any others.
     */
    @GET @Path("/session")
    @Produces("application/json")
    public Response getInitialTokenRequest() {
        return new Result(SessionAuthentication.createInitialTokenSessionDataResponseJson()).toResponse();
    }

    /**
     * Query all the initial data needed by the mobile application.
     */
    @GET @Path("/initial")
    @Produces("application/json")
    public Response getInitialDataRequest(@Context HttpServletRequest reqs, @Context HttpHeaders hh) {
        if (!EndpointAuthenticationUtils.isSessionTokenValid(reqs, hh)) {
            return EndpointErrorUtils.processClientFatalError(reqs, UNAUTHORIZED, "Invalid session token.")
                    .toResponse();
        }

        JSONObject jsonObje = new JSONObject();
        jsonObje.put("configurations", ConfigurationsEndpoint.executeConfigurationsQuery());

        try (Connection conn = SQLManager.getConnection(false)) {
        } catch (IllegalArgumentException e) {
            return EndpointErrorUtils.processClientFatalError(reqs, INVALID_PARAMETER, e.toString()).toResponse();
        } catch (SQLException e) {
            return EndpointErrorUtils.processServerFatalError(DATABASE_TRANSACTION, "Failed to execute statement" +
                    " in getInitialDataRequest(): {}", e.toString()).toResponse();
        }
        return new Result(jsonObje).toResponse();
    }

}
