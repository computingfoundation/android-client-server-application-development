package com.organization.commons.rest;

import org.json.JSONObject;

/**
 * A value request
 */
public class ValueRequest {
    private String mValue;

    public ValueRequest(String value) {
        mValue = value;
    }

    public String toJson() {
        return new JSONObject().put("valueRequest", new JSONObject().put("value", mValue)).toString();
    }

    public static ValueRequest fromJson(JSONObject jsonObject) {
        return new ValueRequest(jsonObject.getJSONObject("valueRequest").getString("value"));
    }

    public String getValue() {
        return mValue;
    }

}
