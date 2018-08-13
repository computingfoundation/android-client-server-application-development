package com.fencedin.backend.rest;

import com.fencedin.commons.base.StringUtils;
import com.fencedin.commons.rest.EndpointsRequestError;
import org.json.JSONObject;

/**
 * A server fatal endpoints request error
 */
public class ServerFatalEndpointsRequestError implements EndpointsRequestError {

    public enum TYPE implements BASE_TYPE {
        /*
        Errors resulting from general operations
         */

        /** An error caused by performing an operation on an illegal state of database or server information */
        ILLEGAL_STATE(100),

        /*
        Errors resulting from data manipulation
         */

        /** An error caused by an thrown exception for database transaction */
        DATABASE_TRANSACTION(120);


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
     * Construct a ServerFatalEndpointsRequestError.
     */
    public ServerFatalEndpointsRequestError(TYPE type) {
        mType = type;
    }

    /**
     * Construct a ServerFatalEndpointsRequestError with a message.
     */
    public ServerFatalEndpointsRequestError(TYPE type, String message, Object... messageArguments) {
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
        JSONObject jsonObje = new JSONObject();
        jsonObje.put("code", mType.getCode());
        return jsonObje;
    }

}
