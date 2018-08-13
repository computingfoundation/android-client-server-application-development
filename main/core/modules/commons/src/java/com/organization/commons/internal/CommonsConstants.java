package com.organization.commons.internal;

public class CommonsConstants {
    public static final Server SERVER_URL = Server.DEVELOPMENT;
    public static final String MQTT_URL = "ssl://" + SERVER_URL.getAddress() + ":8883";

    public enum Server {
        DEVELOPMENT ("http", "192.168.42.229", 8083),
        PRODUCTION ("https", "<production-url>", 8443);
        private final String mProtocol;
        private final String mAddress;
        private final int mPort;

        Server(String protocol, String address, int port) {
            mProtocol = protocol;
            mAddress = address;
            mPort = port;
        }

        public String getProtocol() { return mProtocol; }
        public String getAddress() { return mAddress; }
        public int gePort() { return mPort; }

        @Override
        public String toString() {
            return mProtocol + "://" + mAddress + ":" + mPort;
        }
    }

    public static class Time {
        public static final int TWO_SECONDS = 2000;
        public static final int TEN_SECONDS = 10000;
        public static final int TWENTY_SECONDS = 20000;
        public static final int FIVE_MINUTES = 1000 * 60 * 5;
        public static final int FIFTEEN_MINUTES = 900000;
        public static final int ONE_MINUTE = 1000 * 60;
        public static final int ONE_AND_A_HALF_MINUTES = (int) (1000 * 60 * 1.5);
        public static final int TWO_MINUTES = 1000 * 60 * 2;
        public static final int TEN_MINUTES = 1000 * 60 * 10;
        public static final int FIFTY_MINUTES = 3000000;
        public static final int ONE_HOUR = 1000 * 60 * 60;
        public static final int ONE_AND_A_HALF_HOURS = (int) (1000 * 60 * 60 * 1.5);
        public static final int TWO_HOURS = 1000 * 60 * 60 * 2;
        public static final int THREE_HOURS = 10800000;
        public static final int FOUR_HOURS = 14400000;
        public static final int FIVE_HOURS = 18000000;
        public static final int SIX_HOURS = 21600000;
        public static final int TWELVE_HOURS = 43200000;
        public static final int TWENTY_FOUR_HOURS = 86400000;
        public static final int FORTY_EIGHT_HOURS = 172800000;
        public static final int FOUR_DAYS = 86400000 * 4;
    }

}
