package com.organization.commons.base;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.json.JSONObject;

import java.io.IOException;

public class SerializeUtil {

    /**
     * Serializes any object to json
     */
    public static String serialize(Object object, ObjectMapper objectMapper) {
        String serialized = null;
        try {
            serialized = objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        return serialized;
    }

    public static String serialize(Object object) {
        return serialize(object, createSerializerDefaultObjectMapper());
    }

    /**
     * Deserializes any json to the given class type
     * @throws IOException If deserialization failed
     */
    public static <T> T deserialize(String jsonStr, Class<T> classType, ObjectMapper objectMapper) throws IOException {
        return objectMapper.readValue(jsonStr, classType);
    }

    public static <T> T deserialize(String jsonStr, Class<T> classType) throws IOException {
        return deserialize(jsonStr, classType, createDeserializerDefaultObjectMapper());
    }

    public static <T> T deserialize(JSONObject json, Class<T> classType, ObjectMapper objectMapper) throws IOException {
        return deserialize(json.toString(), classType, objectMapper);
    }

    public static <T> T deserialize(JSONObject json, Class<T> classType) throws IOException {
        return deserialize(json.toString(), classType, createDeserializerDefaultObjectMapper());
    }



    public static ObjectMapper createDeserializerDefaultObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule().setDeserializerModifier(new LowercaseEnumDeserializerModifier());

        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(module);
        return objectMapper;
    }

    public static ObjectMapper createSerializerDefaultObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // Omit unset properties in Json
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return objectMapper;
    }

}
