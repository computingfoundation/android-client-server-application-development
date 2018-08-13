package com.organization.commons.rest;

import org.json.JSONObject;

/**
 * Client input response (requested by the server).
 */
public class InputResponse {

    public enum TextInputType {
        SINGLE_TEXT_INPUT(1),
        DUAL_TEXT_INPUT(2);

        private final int mCode;

        TextInputType(int code) {
            mCode = code;
        }

        public int getCode() { return mCode; }

        public static TextInputType fromCode(int code) {
            switch (code) {
                case 1: return SINGLE_TEXT_INPUT;
                case 2: return DUAL_TEXT_INPUT;
            }

            return null;
        }

        @Override
        public String toString() {
            return super.toString().toLowerCase().replaceAll("_", "");
        }
    }

    private TextInputType mTextInputType;
    private String mMessage;

    public InputResponse(TextInputType type, String message) {
        mTextInputType = type;
        mMessage = message;
    }

    private InputResponse(int typeCode, String text) {
        mTextInputType = TextInputType.fromCode(typeCode);
        mMessage = text;
    }

    public String toJson() {
        JSONObject jsonObje = new JSONObject();

        jsonObje.put("type", mTextInputType.getCode());
        jsonObje.put("message", mMessage);
        return new JSONObject().put("inputResponse", jsonObje).toString();
    }

    public static InputResponse fromJson(JSONObject jsonObject) {
        JSONObject inptRespJson = jsonObject.getJSONObject("inputResponse");
        return new InputResponse(inptRespJson.getInt("type"), inptRespJson.getString("message"));
    }

    public TextInputType getType() {
        return mTextInputType;
    }

    public String getMessage() {
        return mMessage;
    }

}
