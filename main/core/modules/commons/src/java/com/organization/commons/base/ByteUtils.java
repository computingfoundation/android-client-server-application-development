package com.organization.commons.base;

import java.util.Arrays;

public class ByteUtils {

    /* ---------------------------------------
          BASE
       --------------------------------------- */

    public static byte[] concatenateArrays(byte[] first, byte[]... remaining) {
        int len = first.length;
        for (byte[]  array : remaining) {
            len += array.length;
        }
        byte[]  newArr = Arrays.copyOf(first, len);
        int offset = first.length;
        for (byte[] array : remaining) {
            System.arraycopy(array, 0, newArr, offset, array.length);
            offset += array.length;
        }
        return newArr;
    }

    /**
     * Trims a byte to the first and last non-zero values; will return an empty array with one element being a
     * value of 0 if all values equal 0.
     */
    public static byte[] trim(byte[] bytes) {
        int startIdx = -1, endIdx, currStartIdx = 0, currEndIdx = 0;
        for (int i = 0; i < bytes.length; i++) {
            if ((long) bytes[i] != 0) {
                currEndIdx = i;
                if (startIdx == -1) {
                    startIdx = currStartIdx;
                }
            } else {
                currStartIdx++;
            }
        }
        endIdx = currEndIdx;
        startIdx = (startIdx == -1) ? 0 : startIdx;

        byte[] newBytes = new byte[endIdx - startIdx + 1];
        System.arraycopy(bytes, startIdx, newBytes, 0, newBytes.length);
        return newBytes;
    }

    /**
     * Shift bytes to the right the given number of places by reference.
     */
    public static byte[] shiftLeft(byte[] bytes, int places) throws IllegalArgumentException {
        if (places < 0) {
            throw new IllegalArgumentException("Value for variable \"places\" cannot be negative.");
        }

        byte[] newBytes = new byte[bytes.length];
        for (int i = places; i < bytes.length; i++) {
            newBytes[i - places] = bytes[i];
        }
        return newBytes;
    }

    /**
     * Shift bytes to the right the given number of places by reference.
     */
    public static byte[] shiftRight(byte[] bytes, int places) throws IllegalArgumentException {
        if (places < 0) {
            throw new IllegalArgumentException("Value for variable \"places\" cannot be negative.");
        }

        byte[] newBytes = new byte[bytes.length];
        for (int i = bytes.length - places - 1; i >= 0; i--) {
            newBytes[i + places] = bytes[i];
        }
        return newBytes;
    }

    /* ---------------------------------------
          CONVERSION
       --------------------------------------- */

    public static byte[] longToBytes(long l) {
        byte[] bytes = new byte[8];

        for (int i = bytes.length - 1; i >= 0; i--) {
            bytes[i] = (byte) (l);
            l >>>= 8;
        }
        return bytes;
    }

    public static long bytesToLong(byte[] bytes) {
        if (bytes.length != 8) {
            return -1;
        }

        long l = 0;
        for (byte b : bytes) {
            l = (l << 8) + (b & 0xff);
        }
        return l;
    }

    /**
     * Print a bytes array.
     */
    public static void printBytes(byte[] bytes) {
        for (byte b : bytes) {
            System.out.println((char) b + " " + String.format("%3s", (int) b) + " " + String.format("%8s", Integer
                    .toBinaryString(b)).replace(' ', '0'));
        }
    }

    /* ---------------------------------------
          BIT MANIPULATION
       --------------------------------------- */

    /**
     * Converts all values within a byte array, being -128 to 127 (inclusive),
     * to positive, being 0 to 127 (inclusive).
     */
    public static byte[] toPositive(byte[] byteArray) {
        byte[] newArr = new byte[byteArray.length];
        for (int i = 0; i < byteArray.length; i++) {
            newArr[i] = (byte) (byteArray[i] & ~(1 << 7));
        }

        return newArr;
    }

    public static byte[] and(byte[] bytes1, byte[] bytes2) {
        if (bytes1.length != bytes2.length) {
            return null;
        }
        byte[] finalBytes = new byte[bytes1.length];
        for (int i = 0; i < bytes1.length; i++) {
            finalBytes[i] = (byte) (bytes1[i] & bytes2[i]);
        }
        return finalBytes;
    }

    public static byte[] or(byte[] bytes1, byte[] bytes2) {
        if (bytes1.length != bytes2.length) {
            return null;
        }
        byte[] finalBytes = new byte[bytes1.length];
        for (int i = 0; i < bytes1.length; i++) {
            finalBytes[i] = (byte) (bytes1[i] | bytes2[i]);
        }
        return finalBytes;
    }

    public static byte[] xor(byte[] bytes1, byte[] bytes2) {
        if (bytes1.length != bytes2.length) {
            return null;
        }
        byte[] finalBytes = new byte[bytes1.length];
        for (int i = 0; i < bytes1.length; i++) {
            finalBytes[i] = (byte) (bytes1[i] ^ bytes2[i]);
        }
        return finalBytes;
    }

}
