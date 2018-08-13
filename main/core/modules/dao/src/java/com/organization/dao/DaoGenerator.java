package com.organization.dao;

import de.greenrobot.daogenerator.*;

/**
 * Generator for the DAO classes for the app.
 */
public class DaoGenerator {
    private static final String APP_ROOT_DIR = "/mnt/local/components/main/app/src/";
    private static final String APP_DAO_PACKAGE = "com.organization.app.database.dao.generated";

    public static void main(String[] args) throws Exception {
        Schema schema = new Schema(1000, APP_DAO_PACKAGE);

        addUser(schema);

        new de.greenrobot.daogenerator.DaoGenerator().generateAll(schema, APP_ROOT_DIR);
    }

    private static void addUser(Schema schema) {
        Entity user = schema.addEntity("User");
        user.addIdProperty();
        user.addStringProperty("name").notNull();
        user.addStringProperty("password");
        user.addStringProperty("email");
        user.addStringProperty("phoneNumber");
        user.addStringProperty("firstName");
        user.addStringProperty("lastName");
        user.addDoubleProperty("facebookId");
        user.addDoubleProperty("twitterId");
        user.addDoubleProperty("googleId");
    }

}
