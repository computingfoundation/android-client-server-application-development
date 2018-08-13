package com.organization.backend.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * General loggers for different topics.
 */
public class Loggers {

    // TODO: Create appenders to log to file

    public static final Logger illegalRequest = LoggerFactory.getLogger("Illegal request");
    public static final Logger internalError = LoggerFactory.getLogger("Internal error");

}
