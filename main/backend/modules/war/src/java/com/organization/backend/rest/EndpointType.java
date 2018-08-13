package com.organization.backend.rest;

import org.apache.commons.lang3.text.WordUtils;

/**
 * Endpoint types.
 */
public enum EndpointType {
    USERS,
    CONFIGURATIONS,
    PLACE_TYPES;

    /**
     * Get the String shown in a response or log message.
     */
    public String getResponseString() {
        String str = WordUtils.capitalize(super.toString().toLowerCase().replaceAll("_", " "));

        if (str.endsWith("s")) {
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }
}
