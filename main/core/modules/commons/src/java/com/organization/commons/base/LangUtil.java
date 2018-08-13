package com.organization.commons.base;

import java.util.*;

public class LangUtil {
    static final Random RANDOM = new Random();

    public static <T> T[] concatenateArrays(T[] first, T[]... remaining) {
        int len = first.length;
        for (T[] array : remaining) {
            len += array.length;
        }
        T[] finalArr = Arrays.copyOf(first, len);

        int offset = first.length;
        for (T[] array : remaining) {
            System.arraycopy(array, 0, finalArr, offset, array.length);
            offset += array.length;
        }
        return finalArr;
    }

    /**
     * Returns a pseudo-random number between min and max inclusive.
     * The difference between min and max can be at most
     * <code>Integer.MAX_VALUE - 1</code>.
     */
    public static int randInt(int min, int max) {
        return RANDOM.nextInt((max - min) + 1) + min;
    }

    /**
     * Get the keys of a Map by value.
     */
    public static <T, E> Set<T> getKeysByValue(Map<T, E> map, E value) {
        Set<T> keys = new HashSet<T>();
        for (Map.Entry<T, E> entry : map.entrySet()) {
            if (Objects.equals(value, entry.getValue())) {
                keys.add(entry.getKey());
            }
        }
        return keys;
    }

}
