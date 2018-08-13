package com.organization.backend.authentication;

import com.organization.commons.base.ByteUtils;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.util.Arrays;

/**
 * InputRequest authentication will not be used for AuthenticationBase V1 as it does not really provided any
 * added security. The class is only saved in case it is needed.
 */
public class InputRequestAuthentication extends AuthenticationBase {
    private static final Logger log = LoggerFactory.getLogger(InputRequestAuthentication.class);

    public static String createToken(String clientSessionToken) {
        byte[] sessionTokenKey = SessionAuthentication.extractKeyFromClientSessionToken(clientSessionToken);
        byte[] sessionTokenHash = Base64.decodeBase64(clientSessionToken.split("\\.")[1]);
        if (sessionTokenKey == null || sessionTokenHash == null) return null;

        byte[] ssnTknHashSubbytes = new byte[14];
        System.arraycopy(sessionTokenHash, 12, ssnTknHashSubbytes, 0, ssnTknHashSubbytes.length);

        try {
            sMac.reset();
            sMac.init(new SecretKeySpec(sessionTokenKey, "HmacSHA256"));
        } catch (InvalidKeyException e) {
            log.error("Failed to create request token: {}", e.toString());
            return null;
        }
        byte[] hash = sMac.doFinal(ssnTknHashSubbytes);

        return Base64.encodeBase64String(hash);
    }

    public static boolean isTokenValid(String hmacToken, String clientSessionToken) {
        if (hmacToken != null && !hmacToken.equals("")) {
            JSONObject clnSsnTknJsn = new JSONObject(clientSessionToken.split("\\.")[0]);
            byte[] clnSsnTknHash = Base64.decodeBase64(clientSessionToken.split("\\.")[1]);
            byte[] clnSsnTknCookie = Base64.decodeBase64(clnSsnTknJsn.getString("cookie"));

            byte[] srvrHmcTkn = Base64.decodeBase64(createToken(clientSessionToken));

            byte[] clnSsnTknCookieSubbytes = new byte[5];
            System.arraycopy(clnSsnTknCookie, 2, clnSsnTknCookieSubbytes, 0, clnSsnTknCookieSubbytes.length);

            byte[] clntSsnTknHashSubbytes = new byte[16];
            System.arraycopy(clnSsnTknHash, 14, clntSsnTknHashSubbytes, 0, clntSsnTknHashSubbytes.length);

            byte[] srvrHmcTknSubbytes = new byte[16];
            System.arraycopy(srvrHmcTkn, 14, srvrHmcTknSubbytes, 0, srvrHmcTknSubbytes.length);

            try {
                sMac.reset();
                sMac.init(new SecretKeySpec(clnSsnTknHash, "HmacSHA256"));
            } catch (InvalidKeyException e) {
                log.error("Failed to validate client request hmac token: {}", e.toString());
                return false;
            }
            byte[] vldtrHash = sMac.doFinal(ByteUtils.concatenateArrays(clnSsnTknCookieSubbytes, clntSsnTknHashSubbytes));

            return Arrays.equals(vldtrHash, Base64.decodeBase64(hmacToken));
        } else {
            return false;
        }
    }

    private static void logTampering(String token, String ipAddress, String msg) {
        if (msg == null) {
            log.warn("Tampering attempt on received request token: {}; IP: {}", token, ipAddress);
        } else {
            log.warn("Tampering attempt on received request token: {}; IP: {}; Message: {}", token,
                    ipAddress, msg);
        }
    }

}
