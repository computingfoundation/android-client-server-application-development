package com.organization.backend.rest;

import com.organization.commons.rest.EndpointsBaseError;
import org.json.JSONObject;

/**
 * A fatal error caused by the client
 */
public class ClientFatalError implements EndpointsBaseError {

    public enum TYPE implements EndpointsBaseErrorType {
        // error caused when receiving no or an invalid token
        UNAUTHORIZED(300),

        // error caused when receiving an invalid or non-existing URL parameter
        INVALID_PARAMETER(310),

        // errors caused when receiving a invalid resource JSON
        INVALID_RESOURCE(320),

        // error caused when receiving additional information for a resource (such as a user ID) that does not exist
        RESOURCE_NOT_FOUND(330);

        private final int mCode;

        TYPE(int code) {
            mCode = code;
        }

        public int getCode() {
            return mCode;
        }
    }

    private TYPE mType;
    private String mMessage;

    public ClientFatalError(TYPE type) {
        mType = type;
    }

    public ClientFatalError(TYPE type, String message) {
        mType = type;
        mMessage = message;
    }

    @Override
    public EndpointsBaseErrorType getType() {
        return mType;
    }

    @Override
    public String getMessage() {
        return mMessage;
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("code", mType.getCode());
        return json;
    }
}
