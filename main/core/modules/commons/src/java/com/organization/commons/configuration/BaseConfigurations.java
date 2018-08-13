package com.organization.commons.configuration;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Base configurations
 */
public final class BaseConfigurations {
    protected static final String ID = "Base";
    public static boolean sAreInitialized;

    public static final class GeneralConstants {

        public static final class Resources {
            protected static String latitudeLongitudeRegex;
            public static String latitudeLongitudeRegex() { return latitudeLongitudeRegex; }
        }

    }

    public static final class Servers {

        public static final class Mqtt {
            protected static String userName;
            public static String userName() { return userName; }
        }

    }

    protected static boolean parseFromJson(String json) {
        try {
            JSONObject root = new JSONObject(json);

            JSONObject l1 = root.getJSONObject("generalConstants");
            JSONObject l2 = l1.getJSONObject("resources");
            GeneralConstants.Resources.latitudeLongitudeRegex = l2.getString("latitudeLongitudeRegex");

            l1 = root.getJSONObject("servers");
            l2 = l1.getJSONObject("mqtt");
            Servers.Mqtt.userName = l2.getString("userName");

            sAreInitialized = true;
        } catch (NullPointerException e) {
            ConfigurationsUtils.logParseNullPointerException(ID);
            sAreInitialized = false;
        } catch (JSONException e) {
            ConfigurationsUtils.logParseJSONException(ID, e);
            sAreInitialized = false;
        }
        return sAreInitialized;
    }

    public static boolean areInitialized() {
        return sAreInitialized;
    }

}
