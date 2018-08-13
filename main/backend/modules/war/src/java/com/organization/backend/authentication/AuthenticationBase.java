package com.organization.backend.authentication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import java.security.NoSuchAlgorithmException;

public abstract class AuthenticationBase {
    private static final Logger LOG = LoggerFactory.getLogger(UserAuthentication.class);
    protected static KeyGenerator s128BitKeyGenerator;
    protected static KeyGenerator s64BitKeyGenerator;
    protected static Mac sMac;

    static {
        try {
            s128BitKeyGenerator = KeyGenerator.getInstance("HmacSHA256");
            s128BitKeyGenerator.init(128);
            s64BitKeyGenerator = KeyGenerator.getInstance("HmacSHA256");
            s64BitKeyGenerator.init(64);
            sMac = Mac.getInstance("HmacSHA256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

}
