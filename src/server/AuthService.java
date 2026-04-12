package server;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import storage.PostgresSupport;

/**
 * PostgreSQL-backed authentication service.
 * <p>Passwords are stored as SHA-384 hashes.
 */
public class AuthService {
    private static final Logger logger = Logger.getLogger(AuthService.class);
    private static final String USERS_TABLE = "users";

    public boolean register(String login, String rawPassword) {
        validateCredentials(login, rawPassword);

        String normalizedLogin = normalizeLogin(login);
        String passwordHash = hashPassword(rawPassword);

        try (Connection connection = PostgresSupport.openConnection()) {
            ensureUsersSchema(connection);
            try (PreparedStatement statement =
                    connection.prepareStatement(
                            "insert into " + USERS_TABLE + " (login, password_hash) values (?, ?)") ) {
                statement.setString(1, normalizedLogin);
                statement.setString(2, passwordHash);
                statement.executeUpdate();
                logger.info("Created user: " + normalizedLogin);
                return true;
            }
        } catch (SQLException e) {
            if (isUniqueViolation(e)) {
                logger.info("Registration attempt for existing user: " + normalizedLogin);
                return false;
            }
            logger.error("Failed to register user in PostgreSQL: " + normalizedLogin, e);
            return false;
        }
    }

    public boolean authenticate(String login, String rawPassword) {
        if (isBlank(login) || isBlank(rawPassword)) {
            logger.debug("Authentication rejected: blank credentials");
            return false;
        }

        String normalizedLogin = normalizeLogin(login);
        String expectedHash = null;
        try (Connection connection = PostgresSupport.openConnection()) {
            ensureUsersSchema(connection);
            try (PreparedStatement statement =
                    connection.prepareStatement(
                            "select password_hash from " + USERS_TABLE + " where login = ?")) {
                statement.setString(1, normalizedLogin);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        expectedHash = resultSet.getString("password_hash");
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to authenticate user via PostgreSQL: " + normalizedLogin, e);
            return false;
        }

        if (expectedHash == null) {
            logger.debug("Authentication failed: unknown user " + normalizedLogin);
            return false;
        }
        boolean ok = expectedHash.equals(hashPassword(rawPassword));
        if (!ok) {
            logger.debug("Authentication failed: invalid password for " + normalizedLogin);
        }
        return ok;
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

    private void ensureUsersSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "create table if not exists "
                            + USERS_TABLE
                            + " ("
                            + "id bigserial primary key, "
                            + "login varchar(128) not null unique, "
                            + "password_hash varchar(128) not null, "
                            + "created_at timestamp not null default now()"
                            + ")");
        }
    }

    private boolean isUniqueViolation(SQLException e) {
        return "23505".equals(e.getSQLState());
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

}
