package com.organization.backend.rest;

import com.organization.commons.base.SerializeUtil;
import com.organization.commons.base.StringUtils;
import com.organization.commons.rest.*;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class Result {
    private String mMessage;
    private Object mJson;
    private EndpointsBaseError mError;
    private InsertRequest mInsertRequest;
    private UpdateRequest mUpdateRequest;
    private ValueRequest mValueRequest;
    private InputResponse mInputResponse;
    private long mLongData;

    public Result() { }

    public Result(String message) { mMessage = message; }

    public Result(String message, Object... msgVals) {
        mMessage = StringUtils.formatParameterString(message, msgVals);
    }

    public Result(JSONArray jsonArr) { mJson = jsonArr; }

    public Result(JSONObject jsonObj) { mJson = jsonObj; }

    public Result(ServerResponseGeneralError.TYPE type, String message) {
        mError = new ServerResponseGeneralError(type, message);
    }

    public Result(ClientFatalError.TYPE type) {
        mError = new ClientFatalError(type);
    }

    public Result(ClientFatalError.TYPE type, String message) {
        mError = new ClientFatalError(type, message);
    }

    public Result(ServerFatalError.TYPE type) {
        mError = new ServerFatalError(type);
    }

    public Result(ServerFatalError.TYPE type, String message) {
        mError = new ServerFatalError(type, message);
    }

    public Result (InsertRequest insertRequest) { mInsertRequest = insertRequest; }

    public Result (UpdateRequest updateRequest) { mUpdateRequest = updateRequest; }

    public Result (ValueRequest valueRequest) { mValueRequest = valueRequest; }

    public Result (InputResponse inputResponse) { mInputResponse = inputResponse; }

    public Result (long longData) { mLongData = longData; }

    /**
     * Create the JSON body for the response.
     * @return JSON string
     */
    public String toJson() {
        if (mInsertRequest != null) {
            return mInsertRequest.toJson();
        } else if (mUpdateRequest != null) {
            return mUpdateRequest.toJson();
        } else if (mValueRequest != null) {
            return mValueRequest.toJson();
        } else if (mInputResponse != null) {
            return mInputResponse.toJson();
        }

        JSONObject json = new JSONObject();
        JSONObject rootJson = new JSONObject();

        if (mError == null) {
            // key "message" will not be added if mMessage is null
            json.put("message", mMessage);
            rootJson.put("success", json);
        } else {
            json = mError.toJson();
            rootJson.put("error", json);
        }
        return rootJson.toString();
    }

    /**
     * Create the response of the result.
     * @return Response of result
     */
    public Response toResponse() {
        Response.ResponseBuilder builder = Response.ok();

        builder.type(MediaType.APPLICATION_JSON);

        if (mError == null) {
            if (mJson == null || mInsertRequest != null || mUpdateRequest != null || mValueRequest != null ||
                    mInputResponse != null) {
                builder.entity(toJson());
            } else {
                builder.entity(mJson.toString());
            }
        } else {
            builder.entity(toJson());

            // Note: Status BAD_REQUEST is used when the server throws an error accepting an invalid REST resource and
            // should REST therefore not be used for a client fatal error.

            if (mError instanceof ServerFatalError) {
                builder.status(Response.Status.INTERNAL_SERVER_ERROR);
            }
        }
        return builder.build();
    }


    public String getMessage() {
        return mMessage;
    }

    public Object getJson() {
        return mJson;
    }

    public EndpointsBaseError getError() {
        return mError;
    }

    public long getLongData() {
        return mLongData;
    }

    @Override
    public String toString() {
        return SerializeUtil.serialize(this);
    }

}






/*
  Developer documentation:
      Do not use "getCode" or "setCode" for mErrCode as JavaScript will interpret it as a thrown exception.

  Sample response JSONs:

      Twitter:
       {"errors":[{"code":215,"message":"Bad Session database."}]}

      Facebook:
       {
        "error": {
          "message": "Unsupported get request. Please read the Graph API documentation at ...",
          "type": "GraphMethodException",
          "code": 100,
          "fbtrace_id": "A81o5Gzkxng"
       }

       Google +:
        {
         "error": {
          "errors": [
           {
            "domain": "usageLimits",
            "reason": "dailyLimitExceededUnreg",
            "message": "Daily Limit for Unauthenticated Use Exceeded. Continued use requires signup.",
            "extendedHelp": "https://code.google.com/apis/console"
           }
          ],
          "code": 403,
          "message": "Daily Limit for Unauthenticated Use Exceeded. Continued use requires signup."
         }
        }

}

 */