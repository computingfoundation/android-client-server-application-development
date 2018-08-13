package com.organization.commons;

import com.organization.commons.configuration.ConfigurationsManager;
import com.organization.commons.internal.CommonsConstants;
import com.organization.commons.base.HttpUtil;
import org.apache.http.HttpResponse;

import java.io.IOException;

public class TestUtils {
    private static String sConfigurationsJson;

    /**
     * @return The response content or the already initialized configurations json string
     * @throws IllegalStateException if the response was null
     */
    public static String initializeConfigurationsIfNotInitialized() throws IllegalStateException {
        if (!ConfigurationsManager.didInitializationRun()) {
            HttpResponse resp = null;
            try {
                resp = HttpUtil.get(CommonsConstants.SERVER_URL + "/configurations");
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (resp == null) {
                throw new IllegalStateException("Could not retrieve configuration JSONs");
            }

            sConfigurationsJson = HttpUtil.getResponseContent(resp);
            ConfigurationsManager.initializeConfigurations(sConfigurationsJson);
        }

        return sConfigurationsJson;
    }

}
