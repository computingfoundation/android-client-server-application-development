package com.organization.commons.base;

/**
 * Development timer utility
 */
public class DevTimer {
    private static long mStartTime;
    private static long mStopTime;

    public static void start() {
        mStartTime = mStopTime = 0;
        mStartTime = System.nanoTime();
    }

    public static String stopAndPrint() {
        mStopTime = System.nanoTime();
        return print(null);
    }

    public static String stopAndPrint(String beginningMessage) {
        mStopTime = System.nanoTime();
        return print(beginningMessage);
    }

    private static String print(String beginningMessage) {
        StringBuilder sb = new StringBuilder(15);
        sb.append("DEV TIMER: ");
        if (beginningMessage != null) {
            sb.append(beginningMessage);
            sb.append(": ");
        }
        sb.append((mStopTime - mStartTime) / 1000000);
        sb.append("ms");
        String msg = sb.toString();

        System.out.println(msg);
        return msg;
    }

}
