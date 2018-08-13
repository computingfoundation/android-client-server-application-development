package com.organization.commons.base;

/**
 * String utility class for commons.
 */
public class StringUtils {

    /**
     * Format a string with "{}" by replacing it with arguments.
     * @param s String
     * @param objects Values to replace "{}"
     * @return Formatted string.
     */
    public static String formatParameterString(String s, Object... objects) {
        for (Object object : objects) {
            s = s.replaceFirst("\\{\\}", object.toString());
        }
        return s;
    }

    /**
     * Capitalize the first letter of each string in the array.
     * @return Array of strings with first letter uppercase.
     */
    public static String[] capitalizeStringsFirstLetter(String[] strings) {
        for (int i = 0; i < strings.length; i++) {
            if (strings[i] != null && strings[i].length() > 0) {
                String frstLttr = strings[i].substring(0, 1).toUpperCase();
                strings[i] = strings[i].length() > 1 ? frstLttr + strings[i].substring(1) : frstLttr;
            }

        }
        return strings;
    }

}
