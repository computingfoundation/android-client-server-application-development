package com.organization.commons.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

public class Encryptor {
    static final Logger Log = LoggerFactory.getLogger(Encryptor.class);
    static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    static Cipher sCipher;

    static {
        try {
            sCipher = Cipher.getInstance(ALGORITHM);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            Log.error("Failed to initialize Cipher for encryption: " + e.toString());
        }
    }

    public static byte[] encrypt(byte[] key, byte[] iv, byte[] data) {
        Key aesKey = new SecretKeySpec(key, "AES");
        try {
            sCipher.init(Cipher.ENCRYPT_MODE, aesKey, new IvParameterSpec(iv));
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            Log.error("Failed to encrypt data: " + e.toString());
            return null;
        }

        try {
            return sCipher.doFinal(data);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            Log.error("Failed to encrypt data: " + e.toString());
        }

        return null;
    }

    public static byte[] decrypt(byte[] key, byte[] iv, byte[] encryptedData) {
        Key aesKey = new SecretKeySpec(key, "AES");
        try {
            sCipher.init(Cipher.DECRYPT_MODE, aesKey, new IvParameterSpec(iv));
        } catch (InvalidKeyException | InvalidAlgorithmParameterException e) {
            Log.error("Failed to decrypt data: " + e.toString());
            return null;
        }

        try {
            return sCipher.doFinal(encryptedData);
        } catch (IllegalBlockSizeException e) {
            Log.error("Failed to decrypt data: " + e.toString());
        } catch (BadPaddingException e) {
            Log.error("Failed to encrypt data: Incorrect key");
        }

        return null;
    }

}
