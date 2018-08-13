package com.organization.commons.rest;

import org.json.JSONObject;

/**
 * Client update request.
 */
public class UpdateRequest {
    private String mType;
    private String mValue;
    private String mValue2;

    public UpdateRequest(String type) {
        mType = type;
    }

    public UpdateRequest(String type, String value) {
        mType = type;
        mValue = value;
    }

    public UpdateRequest(String type, String value, String value2) {
        mType = type;
        mValue = value;
        mValue2 = value2;
    }

    public String toJson() {
        JSONObject jsonObje = new JSONObject();

        jsonObje.put("type", mType);
        jsonObje.put("value", mValue);
        jsonObje.put("value2", mValue2);
        return new JSONObject().put("updateRequest", jsonObje).toString();
    }

    public static UpdateRequest fromJson(JSONObject jsonObject) {
        JSONObject updtReqsJsonObje = jsonObject.getJSONObject("updateRequest");
        String value = null;
        String value2 = null;

        if (updtReqsJsonObje.has("value")) {
            value = updtReqsJsonObje.getString("value");
        }
        if (updtReqsJsonObje.has("value2")) {
            value2 = updtReqsJsonObje.getString("value2");
        }
        return new UpdateRequest(updtReqsJsonObje.getString("type"), value, value2);
    }

    public String getType() {
        return mType;
    }

    public String getValue() {
        return mValue;
    }

    public String getValue2() {
        return mValue2;
    }

}
