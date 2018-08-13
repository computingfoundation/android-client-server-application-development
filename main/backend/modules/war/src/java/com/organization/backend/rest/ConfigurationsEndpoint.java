package com.organization.backend.rest;

import com.organization.backend.authentication.EndpointAuthenticationUtils;
import com.organization.backend.database.MongoDb;
import com.organization.commons.base.JSONUtils;
import com.mongodb.client.MongoCollection;
import org.apache.commons.collections4.IteratorUtils;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

import static com.organization.backend.rest.ClientFatalError.TYPE.UNAUTHORIZED;

@Path("/configurations")
public class ConfigurationsEndpoint {
    private static final Logger log = LoggerFactory.getLogger(ConfigurationsEndpoint.class);

    @GET
    @Produces("application/json")
    public Response getConfigurationsRequest(@Context HttpServletRequest reqs, @Context HttpHeaders hh) {
        if (!EndpointAuthenticationUtils.isSessionTokenValid(reqs, hh)) {
            return EndpointErrorUtils.processClientFatalError(reqs, UNAUTHORIZED, "Invalid session token.")
                    .toResponse();
        }
        return new Result(new JSONObject().put("configurations", executeConfigurationsQuery())).toResponse();
    }

    /**
     * Execute the configurations database query.
     * @return JSON array of configurations
     */
    @SuppressWarnings("unchecked")
    protected static JSONArray executeConfigurationsQuery() {
        MongoCollection mongColl = MongoDb.getDb().getCollection(MongoDb.COLL_CONFIGURATIONS);
        List<Document> foundDocs = IteratorUtils.toList(mongColl.find().iterator());
        List<String> jsonStrs = MongoDb.Util.documentsToStringsList(foundDocs);
        ArrayList<JSONObject> jsonObjs = new ArrayList<>(jsonStrs.size());

        for (String jsonStr : jsonStrs) {
            jsonObjs.add(new JSONObject(JSONUtils.removeKeys(jsonStr, "_description")));
        }
        return new JSONArray(jsonObjs);
    }

}
