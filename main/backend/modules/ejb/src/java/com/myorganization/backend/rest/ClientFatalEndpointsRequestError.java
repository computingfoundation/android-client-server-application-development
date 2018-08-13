package com.fencedin.backend.rest;

import com.fencedin.commons.base.StringUtils;
import com.fencedin.commons.rest.EndpointsRequestError;
import org.json.JSONObject;

/**
 * A client fatal endpoints request error
 * <p><p>
 * This is an error caused only by code or logic errors on the client side or a cyber attack.
 */
public class ClientFatalEndpointsRequestError implements EndpointsRequestError {

    public enum TYPE implements BASE_TYPE {
        /*
        Errors resulting from receiving invalid content
         */

        /** An error caused by receiving an invalid or no token */
        UNAUTHORIZED(200),
        /** An error caused by receiving an invalid parameter */
        INVALID_PARAMETER(201),
        /** An error caused by receiving an invalid JSON resource */
        INVALID_RESOURCE(202),

        /*
        Errors resulting from receiving illegal content (caused by client logic)
         */

        /** An error caused by a resource (e.g. a user) not existing */
        RESOURCE_DOES_NOT_EXIST(210),
        /** An error caused by performing an action that should not have been performed */
        ILLEGAL_ACTION(211);

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

    /**
     * Construct a ClientFatalEndpointsRequestError.
     */
    public ClientFatalEndpointsRequestError(TYPE type) {
        mType = type;
    }

    /**
     * Construct a ClientFatalEndpointsRequestError with a message.
     */
    public ClientFatalEndpointsRequestError(TYPE type, String message, Object... messageArguments) {
        mType = type;
        mMessage = StringUtils.formatParameterString(message, messageArguments);
    }

    @Override
    public BASE_TYPE getType() {
        return mType;
    }

    @Override
    public int getCode() {
        return mType.mCode;
    }

    @Override
    public String getMessage() {
        return mMessage;
    }

    @Override
    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();
        json.put("code", mType.getCode());
        return json;
    }

}
