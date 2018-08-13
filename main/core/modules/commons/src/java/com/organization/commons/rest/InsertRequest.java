package com.organization.commons.rest;

import org.json.JSONObject;

/**
 * Client insert request.
 */
public class InsertRequest {
    private String mEntityType;
    private String mMessage;
    private JSONObject mData;

    public InsertRequest(String entityType) {
        mEntityType = entityType;
    }

    public InsertRequest(String entityType, String message) {
        mEntityType = entityType;
        mMessage = message;
    }

    public InsertRequest(String entityType, JSONObject data) {
        mEntityType = entityType;
        mData = data;
    }

    public InsertRequest(String entityType, String message, JSONObject data) {
        mEntityType = entityType;
        mMessage = message;
        mData = data;
    }

    public static InsertRequest fromJson(JSONObject jsonObj) {
        JSONObject insrtJsonObj = jsonObj.getJSONObject("insert");

        String entyType = insrtJsonObj.getString("entityType");
        String mssg = null;
        JSONObject value = null;

        if (insrtJsonObj.has("message")) {
            mssg = insrtJsonObj.getString("message");
        }
        if (insrtJsonObj.has("data")) {
            value = insrtJsonObj.getJSONObject("data");
        }
        return new InsertRequest(entyType, mssg, value);
    }

    public String toJson() {
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("entityType", mEntityType);
        jsonObj.put("message", mMessage);
        jsonObj.put("data", mData);

        JSONObject rootJsonObj = new JSONObject();
        rootJsonObj.put("insert", jsonObj);
        return rootJsonObj.toString();
    }

    public String getEntityType() {
        return mEntityType;
    }

    public String getMessage() {
        return mMessage;
    }

    public JSONObject getData() {
        return mData;
    }

}
