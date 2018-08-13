package com.organization.backend.rest;

import com.organization.commons.configuration.ConfigurationsManager;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Application;

public class RestApplication extends Application {
    private static final Logger Log = LoggerFactory.getLogger(RestApplication.class);

    static {
        JSONArray configurationsJsonArr = ConfigurationsEndpoint.executeConfigurationsQuery();
        ConfigurationsManager.initializeConfigurations(configurationsJsonArr.toString());
    }

}