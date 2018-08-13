package com.organization.commons.base;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public final class NumberUtils {

    /**
     * Format a decimal number to the given decimal of places. If the number does not have a decimal or numPlaces is set
     * to 0, it will be returned as is.
     * @param round Whether or not to round; if set to true, the decimal will be rounded using the the half-up method
     *              (e.g. if using 2 decimal places, 6.375 would be rounded to 6.38 and same if negative)
     */
    public static String formatDecimal(double decimal, int numPlaces, boolean round) {
        String repeated = (numPlaces > 0) ? "." + new String(new char[numPlaces]).replace("\0", "#"): "";
        DecimalFormat decimalFormat = new DecimalFormat("#,###" + repeated);

        if (decimal >= 0) {
            RoundingMode roundingMode = (round) ? RoundingMode.HALF_UP : RoundingMode.FLOOR;
            decimalFormat.setRoundingMode(roundingMode);
        } else {
            RoundingMode roundingMode = (round) ? RoundingMode.HALF_DOWN : RoundingMode.CEILING;
            decimalFormat.setRoundingMode(roundingMode);
        }
        return decimalFormat.format(decimal);
    }

    /**
     * Same as formatDecimal(double, int, boolean) with rounding set to true by default.
     */
    public static String formatDecimal(double decimal, int numPlaces) {
        return formatDecimal(decimal, numPlaces, true);
    }

    /**
     * Format a decimal number to be exactly the given number of decimal places. If the given number of decimal places
     * is less than the given decimal, the decimal will be truncated and rounded.
     */
    public static String formatDecimalPadded(double decimal, int numPlaces) {
        String repeated = (numPlaces > 0) ? "." + new String(new char[numPlaces]).replace("\0", "0") : "";
        DecimalFormat decimalFormat = new DecimalFormat("#,##0" + repeated);
        return decimalFormat.format(decimal);
    }

    /**
     * Round a decimal to the given number of places. (Note: The rounded decimal will not include trailing zeros).
     */
    public static double roundDecimal(double decimal, int numPlaces) {
        double numPlacesFinal = Math.pow(10, numPlaces);
        return (double) Math.round(decimal * numPlacesFinal) / numPlacesFinal;
    }

}

