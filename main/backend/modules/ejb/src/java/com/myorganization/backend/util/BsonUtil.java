package com.organization.backend.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.undercouch.bson4jackson.BsonFactory;
import de.undercouch.bson4jackson.BsonModule;
import de.undercouch.bson4jackson.serializers.BsonSerializer;
import org.bson.BSONObject;
import org.bson.BasicBSONDecoder;
import org.bson.BsonArray;
import org.bson.BsonString;
import org.bson.types.BasicBSONList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

public class BsonUtil {
    private static final Logger LOG = LoggerFactory.getLogger(BsonUtil.class);

    // TODO: This class is not fully finished; implementations need to still use binary
    // TODO: instead of, e.g., looping through a list and parsing to a BsonArray

    /**
     * Parse a List to a {@link BSONObject};
     * (Note: BSONObject will be a json object with integers as keys)
     * @param list List to parse; list's type param must have a {@link BsonSerializer}
     * @return BSONObject
     */
    public static BSONObject toBsonObject(List list) {
        BSONObject bsonObject = null;
        byte[] rawContent = createRawByteArray(list);
        bsonObject = new BasicBSONDecoder().readObject(rawContent);

        return bsonObject;
    }

    /**
     * Parse a List to a {@link BsonArray};
     * uses the generic type's object toString method for conversion
     * @param list List to parse; list's type param must have a {@link BsonSerializer}
     * @return BSONObject
     */
    public static BsonArray toBsonArray(List list) {
        BsonArray bsonArray = new BsonArray();
        for (Object obj : list) {
            bsonArray.add(new BsonString(obj.toString()));
        }

        return bsonArray;
    }

    /**
     * Util to convert a List to a raw byte array
     */
    private static byte[] createRawByteArray(List list) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectMapper mapper = new ObjectMapper(new BsonFactory());
        mapper.registerModule(new BsonModule());
        try {
            mapper.writeValue(baos, list);
        } catch (IOException e) {
            LOG.error("Failed to create raw byte array from list: {}", e.toString());
        }

        return baos.toByteArray();
    }

    /* ====================================================
         Miscellaneous util methods (do not use binary)
       ==================================================== */

    /**
     * Parse a List to a BasicBSONList
     * @param list List to parse; list's type param must have a {@link BsonSerializer}
     * @return BSONObject
     */
    public static BasicBSONList toBasicBsonList(List list) {
        BasicBSONList basicBSONList = new BasicBSONList();
        for (Object obj : list) {
            basicBSONList.add(obj.toString());
        }

        return basicBSONList;
    }

}
