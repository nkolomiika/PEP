package ru.pep.platform.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;
import org.springframework.stereotype.Component;

/**
 * Shared helpers for dynamic stand flags. Format <code>pep{[a-zA-Z0-9]{20}}</code>.
 * Used by both official pentest tasks and user-uploaded stands so that the
 * generation / verification logic stays in one place.
 */
@Component
public class FlagSecrets {

    private static final String FLAG_PREFIX = "pep{";
    private static final String FLAG_SUFFIX = "}";
    private static final int FLAG_RANDOM_LENGTH = 20;
    private static final String FLAG_ALPHABET =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final Random secureRandom = new SecureRandom();

    public String generate() {
        StringBuilder token = new StringBuilder(FLAG_RANDOM_LENGTH);
        for (int i = 0; i < FLAG_RANDOM_LENGTH; i++) {
            token.append(FLAG_ALPHABET.charAt(secureRandom.nextInt(FLAG_ALPHABET.length())));
        }
        return FLAG_PREFIX + token + FLAG_SUFFIX;
    }

    public String hash(String flag) {
        if (flag == null) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(flag.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable");
        }
    }

    public boolean matches(String submitted, String storedHash) {
        if (submitted == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
                hash(submitted).getBytes(StandardCharsets.UTF_8),
                storedHash.getBytes(StandardCharsets.UTF_8));
    }
}
