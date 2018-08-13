package com.fencedin.backend.rest;

import com.fencedin.commons.configuration.ConfigurationsManager;
import com.fencedin.commons.configuration.ValidationConfigurations;
import com.fencedin.commons.validation.ValidationError;

import java.util.List;

/**
 * Endpoints utilities
 */
public class EndpointsUtils {

    /**
     * Endpoints resource validation utilities
     */
    public static final class ResourceValidationUtils {

        /**
         * Ensure the necessary configurations are initialized.
         */
        public static boolean ensureNecessaryConfigurationsAreInitialized() {
            if (!ConfigurationsManager.didInitializationRun() || !ValidationConfigurations.areInitialized()) {
                ConfigurationsManager.initConfigurations(ConfigurationsEndpoint.executeConfigurationsQuery().toString());
            }
            return ValidationConfigurations.areInitialized();
        }

        /**
         * Create a response message for the validation errors of a resource.
         */
        public static String createResourceValidationErrorsResponseMessage(ENDPOINT_TYPE endpointType,
                List<? extends ValidationError> validationErrors) {
            StringBuilder strnBldr = new StringBuilder(150);
            for (ValidationError valdError : validationErrors) {
                String strn = valdError.toString();

                strn = (strn.charAt(strn.length() - 1) == '.') ? strn.substring(0, strn.length() - 1) : strn;
                strnBldr.append(" ").append(strn).append(";");
            }
            strnBldr.deleteCharAt(0);
            return endpointType.getLogMessageDisplayString() + ": " + strnBldr.toString();
        }

    }

}
