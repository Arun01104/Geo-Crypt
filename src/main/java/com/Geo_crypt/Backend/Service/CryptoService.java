package com.Geo_crypt.Backend.Service;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.text.DecimalFormat;

@Service
@Data
public class CryptoService {


    @Value("${app.crypto.pepper}")
    private String pepper;


    @Value("${app.crypto.pbkdf2-iterations}")
    private int iterations;


    @Value("${app.crypto.coord-decimals}")
    private int coordDecimals;


    private static final int AES_KEY_BITS = 256;
    private static final int GCM_NONCE_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;


    public String buildCombined(String secretKey, String latitude, String longitude,  int decimals) {
        String lat = latitude == null ? "" : roundCoord(latitude, decimals);
        String lon = longitude == null ? "" : roundCoord(longitude, decimals);
        return String.format("%s:%s:%s", secretKey, lat, lon);
    }


    private String roundCoord(String coordStr, int decimals) {
        try {
            double d = Double.parseDouble(coordStr);
            String pattern = "0"; // default
            if (decimals > 0) {
                StringBuilder sb = new StringBuilder("0.");
                for (int i = 0; i < decimals; i++) sb.append("0");
                pattern = sb.toString();
            }
            DecimalFormat df = new DecimalFormat();
            df.setMaximumFractionDigits(decimals);
            df.setMinimumFractionDigits(decimals);
            df.setGroupingUsed(false);
            return String.valueOf(Math.round(d * Math.pow(10, decimals)) / Math.pow(10, decimals));
        } catch (Exception ex) {
            return coordStr;
        }
    }


    public SecretKeySpec deriveKey(String combined) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(combined.toCharArray(), pepper.getBytes(StandardCharsets.UTF_8), iterations, AES_KEY_BITS);
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = skf.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }


    public byte[] encrypt(byte[] plain, SecretKeySpec aesKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        byte[] nonce = new byte[GCM_NONCE_LENGTH];
        SecureRandom rnd = new SecureRandom();
        rnd.nextBytes(nonce);


        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, nonce);
        cipher.init(Cipher.ENCRYPT_MODE,aesKey,gcmSpec);
        byte[] cipherText = cipher.doFinal(plain);


        byte[] out = new byte[nonce.length + cipherText.length];
        System.arraycopy(nonce, 0, out, 0, nonce.length);
        System.arraycopy(cipherText, 0, out, nonce.length, cipherText.length);
        return out;
    }


    public byte[] decrypt(byte[] encrypted, SecretKeySpec aesKey) throws Exception {
        if (encrypted.length < GCM_NONCE_LENGTH) throw new IllegalArgumentException("Invalid encrypted data");
        byte[] nonce = new byte[GCM_NONCE_LENGTH];
        System.arraycopy(encrypted, 0, nonce, 0, GCM_NONCE_LENGTH);
        int cipherLen = encrypted.length - GCM_NONCE_LENGTH;
        byte[] cipherText = new byte[cipherLen];
        System.arraycopy(encrypted, GCM_NONCE_LENGTH, cipherText, 0, cipherLen);


        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, nonce);
        cipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec);


        return cipher.doFinal(cipherText);
    }

    public byte[] tryDecryptWithTolerance(byte[] encrypted, String secretKey, String latitude, String longitude) throws Exception {
        int start = Math.max(0, coordDecimals);
        Exception lastEx = null;
        for (int d = start; d >= 0; d--) {
            try {
                String combined = buildCombined(secretKey, latitude, longitude, d);
                var aes = deriveKey(combined);
                return decrypt(encrypted, aes);
            } catch (Exception ex) {
                lastEx = ex;
            }
        }
        throw lastEx == null ? new Exception("Decryption failed") : lastEx;
    }

}
