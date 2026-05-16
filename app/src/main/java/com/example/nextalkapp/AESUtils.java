package com.example.nextalkapp;

import android.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class AESUtils {
    // Khóa bí mật (16 ký tự cho AES-128).
    private static final String SECRET_KEY = "NexTalk_Key_2026";

    public static String encrypt(String cleartext) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        byte[] encrypted = cipher.doFinal(cleartext.getBytes());
        return Base64.encodeToString(encrypted, Base64.DEFAULT);
    }

    public static String decrypt(String encrypted) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        byte[] decodedValue = Base64.decode(encrypted, Base64.DEFAULT);
        byte[] decrypted = cipher.doFinal(decodedValue);
        return new String(decrypted);
    }
}