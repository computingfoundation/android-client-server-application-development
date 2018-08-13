package com.fencedin.backend.rest;

import org.apache.commons.lang3.text.WordUtils;

/**
 * An endpoint type
 */
public enum ENDPOINT_TYPE {
    BASE,
    USERS,
    USER_VERIFICATION,
    VERIFICATION,
    CONFIGURATIONS,
    SUITES,
    PLACE_TYPES;

    /**
     * Get the log message display string of the enum.
     */
    public String getLogMessageDisplayString() {
        String strn = super.toString().toLowerCase().replaceAll("_", " ");
        WordUtils.capitalize(strn);
        strn = (strn.endsWith("s")) ? strn.substring(0, strn.length() - 1) : strn;
        return strn;
    }

}
