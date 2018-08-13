package com.organization.backend.database;

public class DbConstants {

    public static final class SQL {
        public static final String ORG_MAIN_DATABASE = "organization_main";
        public static final String INTERNAL_DATABASE = "internal";
        public static final String ADMIN_DATABASE_USER = "fiadmin";
        public static final String PUBLIC_DATABASE_USER = "fipublic";

        public static class AUTHENTICATION {
            private static final String SCHEMA = "authentication";
            public static final String TBL_SESSION = SCHEMA + ".session";
            public static final String TBL_USER = SCHEMA + ".user";
        }

        public static class USERS {
            private static final String SCHEMA = "users";
            public static final String TBL_USER = SCHEMA + ".user";
            public static final String TBL_USER_LOG = SCHEMA + ".user_log";
            public static final String TBL_USER_ACTIVITY = SCHEMA + ".user_activity";
            public static final String APPLICATION_USER_ID_EXTERNAL_LOGIN_SERVICE_USER_ID_PAIR = SCHEMA +
                    ".application_user_id_external_login_service_user_id_pair";
        }
    }

    public static final class CQL {
        public static final String KEYSPACE = "organization_main";

        public static final String TBL_USER_VOTES = "user_votes";
        public static final String TBL_VOTES = "votes";
    }

}
