package com.fencedin.backend.rest;

import com.fencedin.commons.configuration.ConfigurationsManager;

import javax.ws.rs.core.Application;

public class RestApplication extends Application {

    static {
        ConfigurationsManager.initConfigurations(ConfigurationsEndpoint.executeConfigurationsQuery().toString());
    }

}
