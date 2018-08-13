package com.organization.backend.rest;

import com.organization.commons.configuration.BaseConfigurations;
import com.organization.commons.configuration.ConfigurationsManager;
import com.organization.commons.configuration.ValidationConfigurations;
import com.organization.commons.validation.ValidationError;
import org.json.JSONArray;

import java.util.List;

import static com.organization.backend.rest.ServerFatalError.TYPE.ILLEGAL_STATE;

/**
 * Endpoint resource validation utilities class
 */
public class EndpointResourceValidationUtils {

    /**
     * Ensure the necessary configuration classes are initialized.
     */
    public static Result ensureConfigurationsAreInitialized() {
        if (!ConfigurationsManager.didInitializationRun() || !BaseConfigurations.areInitialized() ||
                !ValidationConfigurations.areInitialized()) {
            JSONArray configsJsnArr = ConfigurationsEndpoint.executeConfigurationsQuery();
            ConfigurationsManager.initializeConfigurations(configsJsnArr.toString());
        }

        if (!BaseConfigurations.areInitialized() || !ValidationConfigurations.areInitialized()) {
            return EndpointErrorUtils.processServerFatalError(ILLEGAL_STATE, "Failed to initialize" +
                    " configurations");
        }
        return new Result();
    }

    /**
     * Format a validation errors response message.
     */
    public static String formatValidationErrorsResponseMessage(EndpointType endpointType,
                                                               List<? extends ValidationError> errors) {
        StringBuilder sb = new StringBuilder(100);

        for (ValidationError err : errors) {
            sb.append(" ").append(err).append(";");
        }
        sb.deleteCharAt(0);
        return endpointType.getResponseString() + ": " + sb.toString();
    }

}
