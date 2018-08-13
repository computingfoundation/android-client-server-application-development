package com.organization.commons.configuration;

import com.organization.commons.base.JSONUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;

/**
 * Manager for all configurations retrieved from the datastore
 */
public final class ConfigurationsManager {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationsManager.class);
    private static final int NUM_TOTAL_ENTITIES = 5;
    private static final HashMap<String, String> CONFIG_JSONS_MAP = new HashMap<>();
    private static boolean sDidInitializationRun;
    private static boolean sDidAllConfigurationsSuccessfullyInitialize;

    private ConfigurationsManager() { }

    /**
     * Initialize all configuration JSONs
     * @return  0 if all configuration updated successfully
     *          1 if not all configurations updated successfully
     *         -1 if list contains an invalid JSONs
     *         -2 if list is null or empty
     */
    public static int initializeConfigurations(JSONArray jsonArry) {
        List<String> configsJsonsStringsList = JSONUtils.parseJsonArrayRootObjectsToList(jsonArry);

        for (int i = 0; i < configsJsonsStringsList.size(); i++) {
            String configsJsonStr = configsJsonsStringsList.get(i);
            String id;

            try {
                JSONObject root = new JSONObject(configsJsonStr);
                id = root.getString("_id");
            } catch (JSONException e) {
                LOG.error("Configurations could not update: List contains invalid json at index {}: {}", i,
                        e.getMessage());
                return -1;
            }

            CONFIG_JSONS_MAP.put(id, configsJsonStr);
        }

        int numEntsSuccessfullyInitialized = 0;

        if (BaseConfigurations.parseFromJson(CONFIG_JSONS_MAP.get(BaseConfigurations.ID))) {
            numEntsSuccessfullyInitialized++;
        }
        if (ApplicationMessagesConfigurations.parseFromJson(CONFIG_JSONS_MAP.get(ApplicationMessagesConfigurations.ID))) {
            numEntsSuccessfullyInitialized++;
        }
        if (RegulationConfigurations.parseFromJson(CONFIG_JSONS_MAP.get(RegulationConfigurations.ID))) {
            numEntsSuccessfullyInitialized++;
        }
        if (ValidationConfigurations.parseFromJson(CONFIG_JSONS_MAP.get(ValidationConfigurations.ID))) {
            numEntsSuccessfullyInitialized++;
        }

        sDidInitializationRun = true;

        if (numEntsSuccessfullyInitialized == NUM_TOTAL_ENTITIES) {
            LOG.info("Configurations successfully initialized");
            sDidAllConfigurationsSuccessfullyInitialize = true;
            return 0;
        } else {
            LOG.error((NUM_TOTAL_ENTITIES - numEntsSuccessfullyInitialized) + " configuration entities failed to initialize.");
            sDidAllConfigurationsSuccessfullyInitialize = false;
            return 1;
        }
    }

    public static int initializeConfigurations(String configsJsonArryStr) {
        JSONArray jsonArry;
        try {
            jsonArry = new JSONArray(configsJsonArryStr);
        } catch (JSONException e) {
            LOG.error("Configurations could not be updated: String does not form a valid JSONArray: {}",
                    e.getMessage());
            return -1;
        }

        return initializeConfigurations(jsonArry);
    }

    public static boolean didInitializationRun() {
        return sDidInitializationRun;
    }

    public static boolean didAllConfigurationsSuccessfullyInitialize() {
        return sDidAllConfigurationsSuccessfullyInitialize;
    }

}
