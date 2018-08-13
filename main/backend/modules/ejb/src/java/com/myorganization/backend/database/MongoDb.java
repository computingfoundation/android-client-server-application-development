package com.organization.backend.database;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import de.undercouch.bson4jackson.serializers.BsonSerializer;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * MongoDb connection manager and utility class.
 */
public class MongoDb {
    private static final Logger LOG = LoggerFactory.getLogger(MongoDb.class);
    public static final String COLL_CONFIGURATIONS = "configurations";

    // TODO: Some methods have been commented as they use Java 8 which is not supported.

    public static MongoDatabase getDb() {
        MongoClient mongoClient = new MongoClient();
        return mongoClient.getDatabase(DbConstants.SQL.ORG_MAIN_DATABASE).withWriteConcern(WriteConcern.ACKNOWLEDGED);
    }

    public static MongoCollection<Document> configsCollection() {
        return getDb().getCollection(COLL_CONFIGURATIONS);
    }

    public static class Insert {

        public static ObjectId one(Document document, String collection) {
            MongoDb.mainCollection().insertOne(document);
            ObjectId objectId = (ObjectId) document.get("_id");
            if (objectId == null) {
                LOG.error("Failed to insert document into collection \"" + collection + "\"");
            }
            return objectId;
        }

    }

    public static class Delete {

        public static Object byId(String id, String collection) {
            MongoCollection clt = getDb().getCollection(collection);
            BasicDBObject query = new BasicDBObject();
            query.put("_id", id);
            Object object = clt.findOneAndDelete(query);
            if (object == null) {
                LOG.error("Failed to delete document with id \"" + id + "\" in collection \"" + collection + "\"");
            }
            return object;
        }

    }

    public static class Util {

        /**
         * Convert document to JSON objects.
         */
        public static List<JSONObject> documentsToJsonsList(List<Document> documents) {
            if (documents == null) {
                return null;
            }

            ArrayList<JSONObject> jsons = new ArrayList<>(documents.size());
            for (Document d : documents) {
                if (d == null) {
                    return null;
                }
                jsons.add(new JSONObject(d.toJson()));
            }
            return jsons;
        }

        /**
         * Convert document to JSON strings.
         */
        public static List<String> documentsToStringsList(List<Document> documents) {
            if (documents == null) {
                return null;
            }

            ArrayList<String> strs = new ArrayList<>(documents.size());
            for (Document d : documents) {
                if (d == null) {
                    return null;
                }
                strs.add(d.toJson());
            }
            return strs;
        }

        /**
         * Parse a List to a {@link BasicDBList}
         * @param list List to parse; list's type param must have a {@link BsonSerializer}
         * @return BasicDBList
         */
        public static BasicDBList listToBasicDBList(List list) {
            // TODO: This method is not finished; return an array
            // TODO: within an array; should be integrated better with BsonUtil
            BasicDBList basicDbList = new BasicDBList();
            for (Object obj : list) {
                basicDbList.add(obj.toString());
            }
            return basicDbList;
        }

    }

}


/*
  Developer Notes:

  The Util class contains util methods for objects in org.bson. These util methods are placed here instead
  of the BsonUtil class because they rely on a specific org.bson dependencies within the mongodb driver
  dependency. Any util method that uses a class dependent on the mongodb driver dependency should be
  placed here.
 */
