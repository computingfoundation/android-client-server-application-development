package com.organization.commons.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract configurations class for all configuration classes.
 */
public class ConfigurationsUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationsUtils.class);

    /**
     * Log a NullPointerException for parsing the JSON of a configuration entity. A NullPointerException is thrown when
     * a JSON key is not found.
     */
    protected static void logParseNullPointerException(String configEntityId) {
        // Note: The NullPointerException will not contain a message.
        LOG.error("Failed to update \"{}\" configurations: Key not found", configEntityId);
    }

    /**
     * Log a JSON exception for parsing the JSON of a configuration entity.
     */
    protected static void logParseJSONException(String configEntityId, Exception e) {
        LOG.error("Failed to update \"{}\" configurations: {}", configEntityId, e.getMessage());
    }

}
