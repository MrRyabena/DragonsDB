package server;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Minimal in-memory authentication service.
 *
 * <p>Passwords are stored as SHA-384 hashes. This is a temporary implementation until
 * PostgreSQL-backed user storage is added.
 */
public class AuthService {

    public boolean register(String login, String rawPassword) {
        validateCredentials(login, rawPassword);

        String normalizedLogin = normalizeLogin(login);
        String passwordHash = hashPassword(rawPassword);
        return users.putIfAbsent(normalizedLogin, passwordHash) == null;
    }

    public boolean authenticate(String login, String rawPassword) {
        if (isBlank(login) || isBlank(rawPassword)) {
            return false;
        }

        String normalizedLogin = normalizeLogin(login);
        String expectedHash = users.get(normalizedLogin);
        if (expectedHash == null) {
            return false;
        }
        return expectedHash.equals(hashPassword(rawPassword));
    }

    public void validateCredentials(String login, String rawPassword) {
        if (isBlank(login)) {
            throw new IllegalArgumentException("Login cannot be empty");
        }
        if (isBlank(rawPassword)) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
    }

    private String normalizeLogin(String login) {
        return login.trim().toLowerCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String hashPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-384");
            byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));

            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-384 is unavailable", e);
        }
    }

    private final Map<String, String> users = new ConcurrentHashMap<>();
}
