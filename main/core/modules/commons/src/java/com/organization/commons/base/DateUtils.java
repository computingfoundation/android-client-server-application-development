package com.organization.commons.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Date utilities
 */
public class DateUtils {
    private static final Logger LOG = LoggerFactory.getLogger(DateUtils.class);
    private static final DateFormat ISO8601_DATE_FORMAT;
    private static final DateFormat ISO8601_DATE_FORMAT_NO_MILLIS;

    static {
        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        ISO8601_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        ISO8601_DATE_FORMAT.setTimeZone(timeZone);
        ISO8601_DATE_FORMAT_NO_MILLIS = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        ISO8601_DATE_FORMAT_NO_MILLIS.setTimeZone(timeZone);
    }

    /**
     * Convert date to ISO-8601 format (yyyy-MM-dd'T'HH:mm:ss.SSS'Z').
     * @param date Date to format
     * @return Date formatted in ISO-8601.
     */
    public static String toIso8601(Date date) {
        return ISO8601_DATE_FORMAT.format(date);
    }

    /**
     * Convert date to ISO-8601 format discluding milliseconds (yyyy-MM-dd'T'HH:mm:ss'Z').
     * @param date Date to format
     * @return Date formatted in ISO-8601 discluding milliseconds.
     */
    public static String toIso8601NoMillis(Date date) {
        return ISO8601_DATE_FORMAT_NO_MILLIS.format(date);
    }

    /**
     * Parse ISO-8601 formatted date to a Date object.
     * @param iso8601String ISO-8601 formatted date
     * @return Date object of ISO-8601 date.
     */
    public static Date fromIso8601(String iso8601String) {
        List<String> formatStrings = Arrays.asList(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
                "yyyy-MM-dd HH:mm:ss.SSS",
                "yyyy-MM-dd HH:mm:ss");
        Date date = null;

        for (String formatString : formatStrings) {
            try {
                date = new SimpleDateFormat(formatString, Locale.US).parse(iso8601String);
                break;
            } catch (ParseException ignored) { }
        }

        if (date == null) {
            LOG.error("Failed to parse ISO-8601 string \"{}\" to Date", iso8601String);
        }
        return date;
    }

    /**
     * Convert milliseconds to seconds as a formatted String. If the number is a whole number, no decimal places will be
     * included. If not, at most the specified number of decimal places will be used.
     */
    public static String formatMillisecondsToSeconds(long milliseconds, int decimalPlaces) {
        return NumberUtils.formatDecimal((double) milliseconds / 1000, decimalPlaces);
    }

    /**
     * Same as formatMillisecondsToSeconds(long, int) using no decimal places by default.
     */
    public static String formatMillisecondsToSeconds(long milliseconds) {
        return NumberUtils.formatDecimal((double) milliseconds / 1000, 0);
    }

    /**
     * Convert milliseconds to minutes as a formatted String. If the number is a whole number, no decimal places will be
     * included. If not, at most the specified number of decimal places will be used.
     */
    public static String formatMillisecondsToMinutes(long milliseconds, int decimalPlaces) {
        return NumberUtils.formatDecimal((double) milliseconds / (1000 * 60), decimalPlaces);
    }

    /**
     * Same as formatMillisecondsToMinutes(long, int) using no decimal places by default.
     */
    public static String formatMillisecondsToMinutes(long milliseconds) {
        return NumberUtils.formatDecimal((double) milliseconds / (1000 * 60), 0);
    }

    /**
     * Convert milliseconds to hours as a formatted String. If the number is a whole number, no decimal places will be
     * included. If not, at most the specified number of decimal places will be used.
     */
    public static String formatMillisecondsToHours(long milliseconds, int decimalPlaces) {
        return NumberUtils.formatDecimal((double) milliseconds / (1000 * 60 * 60), decimalPlaces);
    }

    /**
     * Same as formatMillisecondsToHours(long, int) using no decimal places by default.
     */
    public static String formatMillisecondsToHours(long milliseconds) {
        return NumberUtils.formatDecimal((double) milliseconds / (1000 * 60 * 60), 0);
    }

    /**
     * Convert milliseconds to days as a formatted String. If the number of days is a whole number, no decimal places
     * will be included. If not, at most the specified number of decimal places will be used.
     */
    public static String formatMillisecondsToDays(long milliseconds, int decimalPlaces) {
        return NumberUtils.formatDecimal((double) milliseconds / (1000 * 60 * 60 * 24), decimalPlaces);
    }

    /**
     * Same as formatMillisecondsToDays(long, int) using no decimal places by default.
     */
    public static String formatMillisecondsToDays(long milliseconds) {
        return NumberUtils.formatDecimal((double) milliseconds / (1000 * 60 * 60 * 24), 0);
    }

    /**
     * Format milliseconds to a time measured with the correct unit.
     * @return A time with its unit of measurement (e.g. "42 minutes")
     */
    public static String formatMillisecondsToTimeWithUnit(long milliseconds) {
        String unit, remTimeStr;

        if (milliseconds < 1000 * 60) {
            unit = (milliseconds >= 1500) ? " seconds" : " second";
            remTimeStr = (milliseconds >= 500) ? DateUtils.formatMillisecondsToSeconds(milliseconds) : "1";
        } else if (milliseconds < 1000 * 60 * 60) {
            unit = (milliseconds >= 1000 * 60 * 1.5) ? " minutes" : " minute";
            remTimeStr = DateUtils.formatMillisecondsToMinutes(milliseconds);
        } else {
            unit = (milliseconds >= 1000 * 60 * 60 * 1.5) ? " hours" : " hour";
            remTimeStr = DateUtils.formatMillisecondsToHours(milliseconds);
        }
        return remTimeStr + unit;
    }

    /**
     * Get a new {@link Calendar} instance using the UTC time zone.
     */
    public static Calendar getUtcCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        return cal;
    }

}
