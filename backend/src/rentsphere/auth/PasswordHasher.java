package rentsphere.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.HexFormat;

/**
 * Cryptographic Password Hashing Utility using PBKDF2 with HMAC-SHA256.
 * Implements industry-standard password protection using standard Java SE libraries (Zero External JARs).
 * - 100,000 iterations
 * - 32-byte cryptographically secure random salt
 * - 256-bit derived key length
 */
public class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 100_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        return HexFormat.of().formatHex(salt);
    }

    public static String hashPassword(String password, String hexSalt) {
        if (password == null || hexSalt == null) {
            throw new IllegalArgumentException("Password and salt must not be null");
        }
        try {
            byte[] salt = HexFormat.of().parseHex(hexSalt);
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error hashing password with PBKDF2: " + e.getMessage(), e);
        }
    }

    public static boolean verifyPassword(String inputPassword, String hexSalt, String expectedHash) {
        if (inputPassword == null || hexSalt == null || expectedHash == null) {
            return false;
        }
        String calculatedHash = hashPassword(inputPassword, hexSalt);
        // Constant-time comparison to prevent timing attacks
        return MessageDigestConstantTimeEquals(calculatedHash, expectedHash);
    }

    private static boolean MessageDigestConstantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
