package com.organization.commons.configuration;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Application messages configurations
 */
public final class ApplicationMessagesConfigurations {
    protected static final String ID = "ApplicationMessages";
    public static boolean sAreInitialized;

    public static final class Application {
        protected static String onStart;
        protected static int onStartShowInterval;
        public static String onStart() { return onStart; }
        public static int onStartShowInterval() { return onStartShowInterval; }
    }

    public static final class UserAccount {
        protected static String onLogIn;
        protected static int onLogInShowInterval;
        public static String onLogIn() { return onLogIn; }
        public static int onLogInShowInterval() { return onLogInShowInterval; }
    }

    public static final class CreateAccount {
        protected static String postAccountCreated;
        public static String postAccountCreated() { return postAccountCreated; }
    }

    protected static boolean parseFromJson(String json) {
        try {
            JSONObject root = new JSONObject(json);

            JSONObject l1 = root.getJSONObject("application");
            Application.onStart = l1.getString("onStart");
            Application.onStartShowInterval = l1.getInt("onStartShowInterval");

            l1 = root.getJSONObject("userAccount");
            UserAccount.onLogIn = l1.getString("onLogIn");
            UserAccount.onLogInShowInterval = l1.getInt("onLogInShowInterval");

            l1 = root.getJSONObject("createAccount");
            CreateAccount.postAccountCreated = l1.getString("postAccountCreated");

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
