package com.organization.backend.rest;

import com.organization.commons.rest.EndpointsBaseError;
import org.json.JSONObject;

/**
 * A fatal error caused by the server
 */
public class ServerFatalError implements EndpointsBaseError {

    public enum TYPE implements EndpointsBaseErrorType {
        // error caused when a database transaction fails
        DATABASE_TRANSACTION(100),

        // error caused when the server or a database or database information are in an incorrect state to process a
        // request (e.g. when configuration are not initialized)
        ILLEGAL_STATE(120);

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

    public ServerFatalError(TYPE type) {
        mType = type;
    }

    public ServerFatalError(TYPE type, String message) {
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
