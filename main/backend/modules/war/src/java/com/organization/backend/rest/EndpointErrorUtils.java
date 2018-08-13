package com.organization.backend.rest;

import com.organization.backend.util.Loggers;
import com.organization.commons.base.StringUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * Endpoint error utilities.
 */
public class EndpointErrorUtils {

    public static Result processClientFatalError(HttpServletRequest reqs, ClientFatalError.TYPE error, String msg,
                                                 Object... msgVals) {
        // TODO: Block IP
        Loggers.illegalRequest.warn("{}: {} (IP: {})", error, StringUtils.formatParameterString(msg, msgVals), reqs
                .getRemoteAddr());
        return new Result(error);
    }

    public static Result processServerFatalError(ServerFatalError.TYPE type, String msg, Object... msgVals) {
        Loggers.internalError.error("{}: {}", type, StringUtils.formatParameterString(msg, msgVals));
        return new Result(type);
    }

}
