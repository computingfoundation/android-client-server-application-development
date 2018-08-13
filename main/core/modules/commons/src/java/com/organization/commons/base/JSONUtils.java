package com.organization.commons.base;

import org.apache.commons.lang3.text.WordUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON utilities class
 */
public class JSONUtils {

    /**
     * Concatenate json arrays
     */
    public static JSONArray concatenateArrays(JSONArray... jsonArrs) {
        JSONArray result = new JSONArray();
        for (JSONArray arr : jsonArrs) {
            for (int i = 0; i < arr.length(); i++) {
                result.put(arr.get(i));
            }
        }
        return result;
    }

    /**
     * Remove all keys matching a name from a JSON object recursively.
     */
    public static String removeKeys(String jsonStr, String key) {
        // TODO: This method is not the best. Re-implement it as described here: https://stackoverflow.com/a/24183876
        // Note: removeKeys2() did not work and only removes the leading underscore in a key.
        return jsonStr.replaceAll("\"" + key + "\"[ ]*:[^,}\\]]*[,]?", "");
    }

    /**
     * Remove all keys matching a name from a JSON object recursively.
     */
//    public static void removeKeys2(JSONObject json, String keyToDelete) {
//        JSONArray keys = json.names();
//
//        for (int i = 0; i < keys.length(); i++) {
//            String key = (String) keys.get(i);
//            Object obj = null;
//
//            if (key.equals(keyToDelete)) {
//                json.remove(key);
//            } else {
//                obj = json.get(key);
//            }
//
//            if (obj != null && obj instanceof JSONObject) {
//                formatKeysToCamelCase((JSONObject) obj);
//            }
//        }
//    }

    /**
     * Recursively change all key names from lowercase underscore to camelcase
     */
    public static void formatKeysToCamelCase(JSONObject json) {
        JSONArray keys = json.names();

        for (int i = 0; i < keys.length(); i++) {
            String key = (String) keys.get(i);
            Object obj;

            if (key.contains("_")) {
                obj = json.remove(key);

                String newKeyName = WordUtils.capitalize(key, '_').replaceAll("_", "");
                newKeyName = Character.toLowerCase(newKeyName.charAt(0)) + newKeyName.substring(1);

                json.put(newKeyName, obj);
            } else {
                obj = json.get(key);
            }

            if (obj instanceof JSONObject) {
                formatKeysToCamelCase((JSONObject) obj);
            }
        }
    }

    /**
     * Add the string value of each root object in the JSON to a list.
     */
    public static List<String> parseJsonArrayRootObjectsToList(JSONArray jsonArray) {
        ArrayList<String> list;

        if (jsonArray != null) {
            list = new ArrayList<>(jsonArray.length());

            for (int i = 0; i < jsonArray.length(); i++) {
                list.add(jsonArray.get(i).toString());
            }
        } else {
            return null;
        }
        return list;
    }
}


/*
  Developer Documentation:
    -concatenateArrays can also be implemented to concatenate JSON arrays by using Strings, however, after testing,
     using StringBuilder is significantly slower by about 50 times.
 */
