package com.organization.commons.rest;

import org.json.JSONObject;

/**
 * A general error caused by the client
 */
public class ServerResponseGeneralError implements EndpointsBaseError {

    public enum TYPE implements EndpointsBaseErrorType {
        ALREADY_EXISTS(320),

        INVALID_CREDENTIALS(340),

        NOT_ALLOWED(380);

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

    public ServerResponseGeneralError(TYPE type, String message) {
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
        json.put("type", mType.toString());
        json.put("message", mMessage);
        return json;
    }

    public static ServerResponseGeneralError fromJson(JSONObject jsonObject) {
        TYPE type = TYPE.valueOf(jsonObject.getString("type"));
        String message = jsonObject.getString("message");
        return new ServerResponseGeneralError(type, message);
    }
}
