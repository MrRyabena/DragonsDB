package storage;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.log4j.Logger;

import core.Defaults;

/** Shared PostgreSQL connection and schema helpers. */
public final class PostgresSupport {
    public static final String DEFAULT_URL =
            "jdbc:postgresql://" + Defaults.DB_HOST + ":" + Defaults.DB_PORT + "/" + Defaults.DB_NAME;
    public static final String DEFAULT_DRAGON_TABLE = "dragons";
    private static final Logger logger = Logger.getLogger(PostgresSupport.class);

    private PostgresSupport() {}

    public static Connection openConnection() throws SQLException {
        String url = firstNonBlank(System.getenv("DATABASE_URL"), System.getenv("PGJDBC_URL"), DEFAULT_URL);
        String user = firstNonBlank(
                System.getenv("PGUSER"),
                System.getenv("DB_USER"),
                System.getenv("POSTGRES_USER"));
        String password = firstNonBlank(
                System.getenv("PGPASSWORD"),
                System.getenv("DB_PASSWORD"),
                System.getenv("POSTGRES_PASSWORD"));

        PgPassEntry pgPassEntry = null;
        if (isBlank(user) || isBlank(password)) {
            pgPassEntry = loadCredentialsFromPgPass(url, user);
        }
        if (isBlank(user) && pgPassEntry != null && !isBlank(pgPassEntry.user)) {
            user = pgPassEntry.user;
        }
        if (isBlank(password) && pgPassEntry != null) {
            password = pgPassEntry.password;
        }

        if (isBlank(user) || isBlank(password)) {
            logger.info("Connecting to PostgreSQL without explicit credentials, url=" + url);
            return DriverManager.getConnection(url);
        }
        logger.info("Connecting to PostgreSQL, url=" + url + ", user=" + user);
        return DriverManager.getConnection(url, user, password);
    }

    public static void ensureDragonSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "create table if not exists "
                            + DEFAULT_DRAGON_TABLE
                            + " ("
                            + "id bigserial primary key, "
                            + "name varchar(255) not null, "
                            + "x real not null, "
                            + "y real not null, "
                            + "creation_date timestamp not null default now(), "
                            + "modified_at timestamp not null default now(), "
                            + "age integer not null check (age > 0), "
                            + "weight bigint not null check (weight > 0), "
                            + "speaking boolean not null default false, "
                            + "type varchar(32), "
                            + "head_size real not null, "
                            + "head_tooth_count real not null, "
                            + "owner_login varchar(64)"
                            + ")");
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static PgPassEntry loadCredentialsFromPgPass(String jdbcUrl, String requestedUser) {
        String pgPassPath =
                firstNonBlank(
                        System.getenv("PGPASSFILE"),
                        Path.of(System.getProperty("user.home"), ".pgpass").toString());

        String host = extractHost(jdbcUrl);
        String port = extractPort(jdbcUrl);
        String database = extractDatabase(jdbcUrl);

        try (BufferedReader reader = new BufferedReader(new FileReader(pgPassPath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                String[] parts = trimmed.split(":", 5);
                if (parts.length != 5) {
                    continue;
                }

                String entryUser = parts[3];
                if (!fieldMatches(parts[0], host)
                        || !fieldMatches(parts[1], port)
                        || !fieldMatches(parts[2], database)
                        || !fieldMatches(entryUser, requestedUser)) {
                    continue;
                }
                if (isBlank(requestedUser) && "*".equals(entryUser)) {
                    continue;
                }

                String resolvedUser = isBlank(requestedUser) ? entryUser : requestedUser;
                logger.info("Loaded PostgreSQL credentials from .pgpass for user " + resolvedUser);
                return new PgPassEntry(resolvedUser, parts[4]);
            }
        } catch (IOException e) {
            logger.debug("Unable to read .pgpass file: " + pgPassPath);
        }

        return null;
    }

    private static boolean fieldMatches(String pattern, String value) {
        if ("*".equals(pattern)) {
            return true;
        }
        if (value == null) {
            return false;
        }
        return pattern.equals(value);
    }

    private static String extractHost(String jdbcUrl) {
        String value = stripJdbcPrefix(jdbcUrl);
        int slashIndex = value.indexOf('/');
        String hostPort = slashIndex >= 0 ? value.substring(0, slashIndex) : value;
        int colonIndex = hostPort.indexOf(':');
        return colonIndex >= 0 ? hostPort.substring(0, colonIndex) : hostPort;
    }

    private static String extractPort(String jdbcUrl) {
        String value = stripJdbcPrefix(jdbcUrl);
        int slashIndex = value.indexOf('/');
        String hostPort = slashIndex >= 0 ? value.substring(0, slashIndex) : value;
        int colonIndex = hostPort.indexOf(':');
        return colonIndex >= 0 ? hostPort.substring(colonIndex + 1) : "5432";
    }

    private static String extractDatabase(String jdbcUrl) {
        String value = stripJdbcPrefix(jdbcUrl);
        int slashIndex = value.indexOf('/');
        if (slashIndex < 0 || slashIndex + 1 >= value.length()) {
            return "*";
        }
        String dbPart = value.substring(slashIndex + 1);
        int queryIndex = dbPart.indexOf('?');
        return queryIndex >= 0 ? dbPart.substring(0, queryIndex) : dbPart;
    }

    private static String stripJdbcPrefix(String jdbcUrl) {
        if (jdbcUrl == null) {
            return Defaults.DB_HOST + ":" + Defaults.DB_PORT + "/" + Defaults.DB_NAME;
        }
        String prefix = "jdbc:postgresql://";
        if (!jdbcUrl.startsWith(prefix)) {
            return Defaults.DB_HOST + ":" + Defaults.DB_PORT + "/" + Defaults.DB_NAME;
        }
        return jdbcUrl.substring(prefix.length());
    }

    private record PgPassEntry(String user, String password) {}
}
