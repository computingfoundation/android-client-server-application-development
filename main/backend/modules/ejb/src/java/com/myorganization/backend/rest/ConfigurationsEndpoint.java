package com.fencedin.backend.rest;

import com.fencedin.backend.authentication.EndpointAuthenticationUtils;
import com.fencedin.backend.database.DbConstants;
import com.fencedin.backend.database.MongoDbManager;
import com.fencedin.commons.base.JsonUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static com.fencedin.backend.rest.ClientFatalEndpointsRequestError.TYPE.UNAUTHORIZED;

@Path("/configurations")
public class ConfigurationsEndpoint {

    @GET
    @Produces("application/json")
    public Response getConfigurationsRequest(@Context HttpServletRequest reqs, @Context HttpHeaders hh) {
        if (!EndpointAuthenticationUtils.isSessionTokenValid(reqs, hh)) {
            return EndpointsResponseProcessor.createResponseFromCommonResponseBody(
                    EndpointsResponseProcessor.processAndCreateCommonResponseBodyForClientFatalEndpointsRequestError(reqs,
                            new ClientFatalEndpointsRequestError(UNAUTHORIZED, "Session token invalid.")));
        }
        return EndpointsResponseProcessor.createResponseWithJsonBodyAndStatusOk(executeConfigurationsQuery());
    }

    /**
     * Execute the configurations query.
     * @return A JSON array consisting of the configurations
     */
    @SuppressWarnings("unchecked")
    protected static JSONArray executeConfigurationsQuery() {
        List<Document> foundDocs = IteratorUtils.toList(MongoDbManager.getMongoDatabase().getCollection(
                DbConstants.MONGODB.COLL_CONFIGURATIONS).find().iterator());
        List<String> mongoDocmJsonList = MongoDbManager.Util.documentsToStringsList(foundDocs);
        ArrayList<JSONObject> jsonObjeList = new ArrayList<>(mongoDocmJsonList.size());

        for (String mongoDocmJson : mongoDocmJsonList) {
            String frmtJson = JsonUtils.removeKeys(mongoDocmJson, "_description");
            frmtJson = JsonUtils.removeKeys(frmtJson, "_note\\w*");

            jsonObjeList.add(new JSONObject(frmtJson));
        }
        return new JSONArray(jsonObjeList);
    }

}
