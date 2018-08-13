package com.organization.backend.authentication;

import com.organization.backend.base.BackendConstants;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import java.util.List;

public class EndpointAuthenticationUtils {

    /**
     * Determine if a session token provided in the Http headers is valid.
     */
    public static boolean isSessionTokenValid(HttpServletRequest reqs, HttpHeaders hh) {
        if (BackendConstants.Dev.DISABLE_AUTHENTICATION) { return true; }
        List<String> tokens = hh.getRequestHeaders().get("Authorization");
        return tokens.size() > 0 && SessionAuthentication.isClientInitialTokenAndIpValid(tokens.get(0),
                reqs.getRemoteAddr());
    }

    /**
     * Determine if a user token provided in the Http headers is valid.
     */
    public static boolean isUserTokenValid(HttpHeaders hh, long userId) {
        if (BackendConstants.Dev.DISABLE_AUTHENTICATION) { return true; }
        List<String> tokens = hh.getRequestHeaders().get("Authorization");
        return tokens.size() > 0 && UserAuthentication.isTokenValid(tokens.get(0), userId);
    }


}
