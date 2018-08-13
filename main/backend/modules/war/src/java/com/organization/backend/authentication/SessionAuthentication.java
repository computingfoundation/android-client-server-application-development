package com.organization.backend.authentication;

import com.organization.commons.base.ByteUtils;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;

public class SessionAuthentication extends AuthenticationBase {
    private static final Logger LOG = LoggerFactory.getLogger(SessionAuthentication.class);

    public static JSONObject createInitialTokenSessionDataResponseJson() {
        JSONObject respJsonObje = new JSONObject();

        byte[] key = s128BitKeyGenerator.generateKey().getEncoded();
        byte[] cookieBytes = s64BitKeyGenerator.generateKey().getEncoded();
        long cookieLong = ByteUtils.bytesToLong(cookieBytes);

        JSONObject sessDataJsonObje = new JSONObject();
        sessDataJsonObje.put("key", Base64.encodeBase64String(key));
        sessDataJsonObje.put("cookie", Base64.encodeBase64String(cookieBytes));

        respJsonObje.put("session", Base64.encodeBase64String(sessDataJsonObje.toString().getBytes()) + "." +
                Base64.encodeBase64String(createTokenInitialHash(key, cookieLong)));
        return respJsonObje;
    }

    public static boolean isClientInitialTokenAndIpValid(String token, String ipAddress) {
        String[] tknSplit = token.split("\\.");

        if (tknSplit.length == 2 && tknSplit[0].length() > 0 && tknSplit[1].length() > 0) {
            String tknJsonStr = new String(Base64.decodeBase64(tknSplit[0]));
            JSONObject tknJson;
            byte[] key;
            byte[] cookie;
            String tknHash = tknSplit[1];

            try {
                tknJson = new JSONObject(tknJsonStr);
                if (tknJson.length() != 2) {
                    logTampering(tknJsonStr + "." + tknHash, ipAddress, "JSON not length of 2");
                    return false;
                }
                key = Base64.decodeBase64(tknJson.getString("key"));
                cookie = Base64.decodeBase64(tknJson.getString("cookie"));
                if (key.length != 16 || cookie.length != 8) {
                    logTampering(tknJsonStr + "." + tknHash, ipAddress, "Key or cookie incorrect length: " +
                            key.length + " and " + cookie.length);
                    return false;
                }
            } catch (JSONException | IllegalArgumentException e) {
                logTampering(tknJsonStr + "." + tknHash, ipAddress, e.toString());
                return false;
            }

            long cookieLong = ByteUtils.bytesToLong(cookie);

            byte[] tknIntlHash = createTokenInitialHash(key, cookieLong);
            byte[] tknIntlHashSubbytes = new byte[16];

            System.arraycopy(tknIntlHash, 6, tknIntlHashSubbytes, 0, tknIntlHashSubbytes.length);
            try {
                sMac.reset();
                sMac.init(new SecretKeySpec(key, "HmacSHA256"));
            } catch (InvalidKeyException e) {
                LOG.error("Failed to validate client initial token: {}", e.toString());
                return false;
            }
            byte[] validatorHash = sMac.doFinal(tknIntlHashSubbytes);

            return Base64.encodeBase64String(validatorHash).equals(tknHash);

            // TODO: Implement database technique to prevent replay attacks by checking ip validity; This should
            // TODO:   be done after validating a session token because it is the first primarily method of defense anyways
            // TODO:   and will save db lookups. Also do this using a memory based database. There should also be different
            // TODO:   tables, such as one for ip that should be blocked and received a "blocked" messaged and another for
            // TODO:   ips that should be blocked and receive simply the same message as an invalid token, "not authorized".
        } else {
            return false;
        }
    }

    public static byte[] extractKeyFromClientSessionToken(String token) {
        String tknSig = token.split("\\.")[0];
        try {
            JSONObject tknJson = new JSONObject(new String(Base64.decodeBase64(tknSig)));
            return Base64.decodeBase64(tknJson.getString("key"));
        } catch (JSONException | IllegalArgumentException ignored) { }

        return null;
    }

    private static byte[] createTokenInitialHash(byte[] key, long cookieLong) {
        JSONObject json = new JSONObject();
        String tknHashCookie = Base64.encodeBase64String(ByteUtils.longToBytes(cookieLong >> 4));
        json.put("tknHashCookie", tknHashCookie);
        try {
            sMac.reset();
            sMac.init(new SecretKeySpec(key, "HmacSHA256"));
        } catch (InvalidKeyException e) {
            LOG.error("Failed to create initial token: {}", e.toString());
            return null;
        }
        return sMac.doFinal(json.toString().getBytes());
    }

    private static void logTampering(String token, String ipAddress, String msg) {
        if (msg == null) {
            LOG.warn("Tampering attempt on received session token: {}; IP: {}", token, ipAddress);
        } else {
            LOG.warn("Tampering attempt on received session token: {}; IP: {}; Message: {}", token,
                    ipAddress, msg);
        }
    }

}
