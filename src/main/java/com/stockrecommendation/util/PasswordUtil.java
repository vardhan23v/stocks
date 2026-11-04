package com.stockrecommendation.util;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Turns a plain password into the value stored in users.password_hash.
 *
 * PBKDF2 with HMAC-SHA256, a random 16 byte salt per user and 210 000
 * iterations (the OWASP 2023 recommendation). Everything needed to verify the
 * password is stored in one string:
 *
 *   pbkdf2$210000$<salt base64>$<hash base64>
 *
 * The algorithm ships with every JVM, so no extra library is needed.
 */
public final class PasswordUtil {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PREFIX = "pbkdf2";
    private static final int ITERATIONS = 210_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
        // utility class: never instantiated
    }

    /** @return the encoded salted hash of the password */
    public static String hash(String rawPassword) {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = derive(rawPassword.toCharArray(), salt, ITERATIONS);
        Base64.Encoder encoder = Base64.getEncoder();
        return PREFIX + "$" + ITERATIONS + "$" + encoder.encodeToString(salt) + "$" + encoder.encodeToString(hash);
    }

    /**
     * Compares a typed password with the value stored in the database.
     * The comparison takes the same time whether the first or the last byte differs.
     */
    public static boolean matches(String rawPassword, String storedHash) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }
        String[] parts = storedHash.trim().split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            byte[] actual = derive(rawPassword.toCharArray(), salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            return false;   // malformed number or base64
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("PBKDF2 is not available in this JVM.", e);
        } finally {
            spec.clearPassword();
        }
    }

    /**
     * Prints a hash for a password - used once to produce the demo accounts
     * in sample-data.sql:  java PasswordUtil.java Admin@123
     */
    public static void main(String[] args) {
        for (String password : args) {
            System.out.println(password + " -> " + hash(password));
        }
    }
}
