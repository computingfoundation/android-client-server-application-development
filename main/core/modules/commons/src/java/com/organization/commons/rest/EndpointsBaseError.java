package com.organization.commons.rest;

import org.json.JSONObject;

/**
 * Base error for REST and endpoint errors.
 */
public interface EndpointsBaseError {

//    int getCode();
    String getMessage();
    JSONObject toJson();
    EndpointsBaseErrorType getType();

    interface EndpointsBaseErrorType { }

}
